package dev.danielbodi.thesis.payment;

import dev.danielbodi.thesis.payment.common.psp.PspChargeResponse;
import dev.danielbodi.thesis.payment.common.psp.PspChargeStatus;
import dev.danielbodi.thesis.payment.common.psp.PspClient;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author danielbodi
 */
// PspClient is mocked, the base URL is only needed to build the RestClient bean
@SpringBootTest(properties = "psp.base-url=http://psp-mock.invalid")
class ChargePaymentIntegrationTest {

    private static final String TOPIC = "outbox.event.payment_requested";
    private static final BigDecimal AMOUNT = new BigDecimal("999.0000");

    // Containers are Spring beans, not JUnit @Container fields, so the Kafka clients are closed before the broker stops
    @TestConfiguration
    static class ContainerConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18.6");
        }

        @Bean
        @ServiceConnection
        KafkaContainer kafka() {
            return new KafkaContainer("apache/kafka:4.3.1");
        }
    }

    @MockitoBean
    private PspClient pspClient;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        when(pspClient.charge(any(), any())).thenAnswer(invocation -> pspResponse(PspChargeStatus.SUCCEEDED));
    }

    @Test
    void succeededChargeIsRecordedAndPublished() {
        final UUID reference = UUID.randomUUID();

        sendPaymentRequested(UUID.randomUUID(), reference);
        awaitConsumed();

        assertSoftly(softly -> {
            softly.assertThat(paymentStatus(reference)).contains("SUCCEEDED");
            softly.assertThat(outboxTypes(reference)).containsExactly("payment_succeeded");
        });
        verify(pspClient, times(1)).charge(reference, AMOUNT);
    }

    @Test
    void declinedChargeIsRecordedAndPublishedAsFailure() {
        final UUID reference = UUID.randomUUID();
        when(pspClient.charge(eq(reference), any())).thenReturn(pspResponse(PspChargeStatus.FAILED));

        sendPaymentRequested(UUID.randomUUID(), reference);
        awaitConsumed();

        assertSoftly(softly -> {
            softly.assertThat(paymentStatus(reference)).contains("FAILED");
            softly.assertThat(outboxTypes(reference)).containsExactly("payment_failed");
        });
    }

    @Test
    void redeliveredRequestDoesNotChargeAgain() {
        final UUID eventId = UUID.randomUUID();
        final UUID reference = UUID.randomUUID();

        sendPaymentRequested(eventId, reference);
        sendPaymentRequested(eventId, reference);
        awaitConsumed();

        assertSoftly(softly -> {
            softly.assertThat(paymentStatus(reference)).contains("SUCCEEDED");
            softly.assertThat(outboxTypes(reference)).containsExactly("payment_succeeded");
        });
        verify(pspClient, times(1)).charge(eq(reference), any());
    }

    @Test
    void technicalPspFailureIsRetriedAndRecordedOnce() {
        final UUID reference = UUID.randomUUID();
        when(pspClient.charge(eq(reference), any()))
                .thenThrow(new ResourceAccessException("PSP unreachable"))
                .thenReturn(pspResponse(PspChargeStatus.SUCCEEDED));

        sendPaymentRequested(UUID.randomUUID(), reference);
        awaitConsumed();

        assertSoftly(softly -> {
            softly.assertThat(paymentStatus(reference)).contains("SUCCEEDED");
            softly.assertThat(outboxTypes(reference)).containsExactly("payment_succeeded");
        });
        verify(pspClient, times(2)).charge(eq(reference), any());
    }

    private void sendPaymentRequested(UUID eventId, UUID reference) {
        final String payload = """
                {"reference":"%s","amount":%s}""".formatted(reference, AMOUNT);
        final ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, reference.toString(), payload);
        record.headers().add("id", eventId.toString().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).join();
    }

    // single-partition topic: once the barrier is processed, every earlier record (incl. retries) is done
    private void awaitConsumed() {
        final UUID barrier = UUID.randomUUID();
        sendPaymentRequested(UUID.randomUUID(), barrier);
        await().atMost(Duration.ofSeconds(30)).until(() -> paymentStatus(barrier).isPresent());
    }

    private static PspChargeResponse pspResponse(PspChargeStatus status) {
        return new PspChargeResponse(UUID.randomUUID(), status);
    }

    private Optional<String> paymentStatus(UUID reference) {
        return jdbcClient.sql("SELECT status FROM payment WHERE reference = ?")
                .param(reference)
                .query(String.class)
                .optional();
    }

    private List<String> outboxTypes(UUID reference) {
        return jdbcClient.sql("SELECT type FROM outbox WHERE aggregate_id = ? ORDER BY created_at")
                .param(reference.toString())
                .query(String.class)
                .list();
    }
}
