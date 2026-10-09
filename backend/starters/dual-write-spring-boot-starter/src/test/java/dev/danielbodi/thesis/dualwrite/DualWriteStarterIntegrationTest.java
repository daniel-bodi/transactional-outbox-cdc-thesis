package dev.danielbodi.thesis.dualwrite;

import dev.danielbodi.thesis.event.IntegrationEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

/**
 * @author danielbodi
 */
@SpringBootTest(properties = {
        "dual-write.enabled=true",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
class DualWriteStarterIntegrationTest {

    private static final String TOPIC = "outbox.event.test_event";

    @SpringBootApplication
    static class DualWriteTestApplication {

        // Containers are Spring beans, not JUnit @Container fields, so the Kafka clients are closed before the broker stops
        @Bean
        @ServiceConnection
        KafkaContainer kafka() {
            return new KafkaContainer("apache/kafka:4.3.1");
        }

        // only for a real transaction manager: the publisher sends after the commit of the business transaction
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18.6");
        }

        @Bean
        ReceivedRecords receivedRecords() {
            return new ReceivedRecords();
        }
    }

    @Autowired
    private DualWriteEventPublisher publisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ReceivedRecords receivedRecords;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void eventIsSentAfterTheTransactionCommit() {
        final TestEvent event = new TestEvent(UUID.randomUUID().toString(), "created");

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publish(event);
            // the records need some time to reach the listener, so an immediate check can be false pass
            await().during(Duration.ofSeconds(2))
                    .atMost(Duration.ofSeconds(3))
                    .until(() -> !receivedRecords.contains(event.getAggregateId()));
        });

        await().atMost(Duration.ofSeconds(10)).until(() -> receivedRecords.contains(event.getAggregateId()));

        final ConsumerRecord<String, String> consumerRecord = receivedRecords.get(event.getAggregateId());
        assertSoftly(softly -> {
            softly.assertThat(consumerRecord.value()).isEqualTo("{\"status\":\"created\"}");
            final String eventId = new String(consumerRecord.headers().lastHeader("id").value(), StandardCharsets.UTF_8);
            softly.assertThat(UUID.fromString(eventId)).isNotNull();
        });
    }

    @Test
    void noEventIsSentWhenTheTransactionRollsBack() {
        final TestEvent event = new TestEvent(UUID.randomUUID().toString(), "rolled-back");

        transactionTemplate.executeWithoutResult(status -> {
            publisher.publish(event);
            status.setRollbackOnly();
        });

        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(3))
                .until(() -> !receivedRecords.contains(event.getAggregateId()));
    }

    @Test
    void publishingOutsideATransactionIsRejected() {
        assertThatThrownBy(() -> publisher.publish(new TestEvent(UUID.randomUUID().toString(), "none")))
                .isInstanceOf(IllegalStateException.class);
    }

    static class ReceivedRecords {

        private final Map<String, ConsumerRecord<String, String>> recordsByKey = new ConcurrentHashMap<>();

        @KafkaListener(topics = TOPIC, groupId = "dual-write-test")
        void collect(ConsumerRecord<String, String> record) {
            recordsByKey.put(record.key(), record);
        }

        boolean contains(String key) {
            return recordsByKey.containsKey(key);
        }

        ConsumerRecord<String, String> get(String key) {
            return recordsByKey.get(key);
        }
    }

    @Getter
    @RequiredArgsConstructor
    static final class TestEvent implements IntegrationEvent {

        private final String aggregateId;
        private final String status;

        @Override
        public String getAggregateType() {
            return "Test";
        }

        @Override
        public String getEventType() {
            return "test_event";
        }
    }
}
