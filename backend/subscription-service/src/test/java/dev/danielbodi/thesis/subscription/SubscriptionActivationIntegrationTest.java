package dev.danielbodi.thesis.subscription;

import dev.danielbodi.thesis.subscription.create.service.CreateSubscriptionService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

/**
 * @author danielbodi
 */
@SpringBootTest
class SubscriptionActivationIntegrationTest {

    private static final String SUCCEEDED_TOPIC = "outbox.event.payment_succeeded";
    private static final String FAILED_TOPIC = "outbox.event.payment_failed";
    private static final UUID BASIC_PLAN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final BigDecimal BASIC_PLAN_AMOUNT = new BigDecimal("999");

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

    @Autowired
    private CreateSubscriptionService createSubscriptionService;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void creationRecordsPaymentRequestAndPublishesItsReference() {
        final UUID subscriptionId = createPendingSubscription();
        final UUID reference = referenceOf(subscriptionId);

        final Map<String, Object> outbox = jdbcClient.sql("""
                        SELECT aggregate_id, type, payload ->> 'reference' AS reference, (payload ->> 'amount')::numeric AS amount
                        FROM outbox WHERE aggregate_id = ?""")
                .param(subscriptionId.toString())
                .query()
                .singleRow();

        assertSoftly(softly -> {
            softly.assertThat(reference).isNotEqualTo(subscriptionId);
            softly.assertThat(completedAt(subscriptionId)).isNull();
            softly.assertThat(outbox.get("type")).isEqualTo("payment_requested");
            softly.assertThat(outbox.get("reference")).isEqualTo(reference.toString());
            softly.assertThat((BigDecimal) outbox.get("amount")).isEqualByComparingTo(BASIC_PLAN_AMOUNT);
        });
    }

    @Test
    void succeededPaymentActivatesSubscription() {
        final UUID subscriptionId = createPendingSubscription();

        send(SUCCEEDED_TOPIC, UUID.randomUUID(), referenceOf(subscriptionId));
        awaitConsumed(SUCCEEDED_TOPIC);

        assertSoftly(softly -> {
            softly.assertThat(status(subscriptionId)).isEqualTo("ACTIVE");
            softly.assertThat(completedAt(subscriptionId)).isAfterOrEqualTo(createdAt(subscriptionId));
        });
    }

    @Test
    void failedPaymentFailsSubscription() {
        final UUID subscriptionId = createPendingSubscription();

        send(FAILED_TOPIC, UUID.randomUUID(), referenceOf(subscriptionId));
        awaitConsumed(FAILED_TOPIC);

        assertSoftly(softly -> {
            softly.assertThat(status(subscriptionId)).isEqualTo("FAILED");
            softly.assertThat(completedAt(subscriptionId)).isAfterOrEqualTo(createdAt(subscriptionId));
        });
    }

    @Test
    void redeliveredOutcomeIsRecordedOnceForThisConsumerGroup() {
        final UUID eventId = UUID.randomUUID();
        final UUID subscriptionId = createPendingSubscription();

        final UUID reference = referenceOf(subscriptionId);

        send(SUCCEEDED_TOPIC, eventId, reference);
        send(SUCCEEDED_TOPIC, eventId, reference);
        awaitConsumed(SUCCEEDED_TOPIC);

        assertSoftly(softly -> {
            softly.assertThat(status(subscriptionId)).isEqualTo("ACTIVE");
            softly.assertThat(processedEventExists(eventId)).isTrue();
        });
    }

    @Test
    void conflictingOutcomeDoesNotOverrideSettledSubscription() {
        final UUID failedEventId = UUID.randomUUID();
        final UUID subscriptionId = createPendingSubscription();

        final UUID reference = referenceOf(subscriptionId);

        send(SUCCEEDED_TOPIC, UUID.randomUUID(), reference);
        awaitConsumed(SUCCEEDED_TOPIC);
        send(FAILED_TOPIC, failedEventId, reference);
        awaitConsumed(FAILED_TOPIC);

        // the rejected transition is rolled back together with its processed_events row
        assertSoftly(softly -> {
            softly.assertThat(status(subscriptionId)).isEqualTo("ACTIVE");
            softly.assertThat(processedEventExists(failedEventId)).isFalse();
        });
    }

    private UUID createPendingSubscription() {
        return createSubscriptionService.createSubscription(UUID.randomUUID(), BASIC_PLAN_ID);
    }

    private void send(String topic, UUID eventId, UUID reference) {
        final String payload = """
                {"reference":"%s"}""".formatted(reference);
        final ProducerRecord<String, String> record = new ProducerRecord<>(topic, reference.toString(), payload);
        record.headers().add("id", eventId.toString().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).join();
    }

    // single-partition topic: once the barrier subscription is settled, every earlier record (incl. retries) is done
    private void awaitConsumed(String topic) {
        final UUID barrier = createPendingSubscription();
        send(topic, UUID.randomUUID(), referenceOf(barrier));
        await().atMost(Duration.ofSeconds(30)).until(() -> !"PENDING".equals(status(barrier)));
    }

    private UUID referenceOf(UUID subscriptionId) {
        return jdbcClient.sql("SELECT id FROM payment_request WHERE subscription_id = ?")
                .param(subscriptionId)
                .query(UUID.class)
                .single();
    }

    private String status(UUID subscriptionId) {
        return jdbcClient.sql("SELECT status FROM subscription WHERE id = ?")
                .param(subscriptionId)
                .query(String.class)
                .single();
    }

    private OffsetDateTime createdAt(UUID subscriptionId) {
        return timestamp("created_at", subscriptionId);
    }

    private OffsetDateTime completedAt(UUID subscriptionId) {
        return timestamp("completed_at", subscriptionId);
    }

    private OffsetDateTime timestamp(String column, UUID subscriptionId) {
        return jdbcClient.sql("SELECT " + column + " FROM subscription WHERE id = ?")
                .param(subscriptionId)
                .query(OffsetDateTime.class)
                .optional()
                .orElse(null);
    }

    private boolean processedEventExists(UUID eventId) {
        return jdbcClient.sql("""
                        SELECT exists(SELECT 1 FROM processed_events
                                      WHERE consumer_group = 'subscription-service' AND event_id = ?)""")
                .param(eventId)
                .query(Boolean.class)
                .single();
    }
}
