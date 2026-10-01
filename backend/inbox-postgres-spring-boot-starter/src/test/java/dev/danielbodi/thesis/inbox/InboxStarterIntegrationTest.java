package dev.danielbodi.thesis.inbox;

import dev.danielbodi.thesis.inbox.annotation.Idempotent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

/**
 * @author danielbodi
 */
@SpringBootTest(properties = {
        "spring.kafka.consumer.group-id=" + InboxStarterIntegrationTest.MAIN_GROUP,
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
class InboxStarterIntegrationTest {

    static final String MAIN_GROUP = "inbox-test";
    static final String SECOND_GROUP = "inbox-test-second";

    private static final String TOPIC = "inbox.test";

    @SpringBootApplication
    static class InboxTestApplication {

        // Containers are Spring beans, not JUnit @Container fields: the cached test context is closed
        // only at JVM shutdown, after JUnit would already have stopped the containers. The Kafka clients
        // would then wait out their close timeouts against a dead broker. As beans, the clients that
        // depend on them are destroyed first, while the broker is still running.
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

        @Bean
        TestIdempotentKafkaListener testListener(JdbcClient jdbcClient) {
            jdbcClient.sql("CREATE TABLE IF NOT EXISTS business_record (id VARCHAR(255) PRIMARY KEY)").update();
            return new TestIdempotentKafkaListener(jdbcClient);
        }

        @Bean
        SecondGroupIdempotentKafkaListener secondGroupListener() {
            return new SecondGroupIdempotentKafkaListener();
        }

        @Bean
        DefaultErrorHandler errorHandler() {
            return new DefaultErrorHandler(new FixedBackOff(0L, 2L));
        }
    }

    @Autowired
    private TestIdempotentKafkaListener testIdempotentKafkaListener;

    @Autowired
    private SecondGroupIdempotentKafkaListener secondGroupListener;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void firstDeliveryRunsBusinessLogicAndInsertsProcessedEventRow() {
        final UUID eventId = UUID.randomUUID();
        final String payload = "first-" + eventId;

        send(eventId.toString(), payload);
        awaitConsumed();

        assertSoftly(softly -> {
            softly.assertThat(testIdempotentKafkaListener.invocationsFor(payload)).isEqualTo(1);
            softly.assertThat(processedEventExists(MAIN_GROUP, eventId)).isTrue();
            softly.assertThat(businessRowExists(payload)).isTrue();
        });
    }

    @Test
    void redeliveryWithSameEventIdSkipsBusinessLogic() {
        final UUID eventId = UUID.randomUUID();
        final String original = "original-" + eventId;
        final String redelivery = "redelivery-" + eventId;

        send(eventId.toString(), original);
        send(eventId.toString(), redelivery);
        awaitConsumed();

        assertSoftly(softly -> {
            softly.assertThat(testIdempotentKafkaListener.invocationsFor(original)).isEqualTo(1);
            softly.assertThat(businessRowExists(original)).isTrue();
            softly.assertThat(testIdempotentKafkaListener.invocationsFor(redelivery)).isZero();
            softly.assertThat(businessRowExists(redelivery)).isFalse();
        });
    }

    @Test
    void sameEventIsProcessedOncePerConsumerGroup() {
        final UUID eventId = UUID.randomUUID();
        final String original = "per-group-original-" + eventId;
        final String redelivery = "per-group-redelivery-" + eventId;

        send(eventId.toString(), original);
        send(eventId.toString(), redelivery);
        awaitConsumed();

        assertSoftly(softly -> {
            softly.assertThat(testIdempotentKafkaListener.invocationsFor(original)).isEqualTo(1);
            softly.assertThat(secondGroupListener.invocationsFor(original)).isEqualTo(1);
            softly.assertThat(testIdempotentKafkaListener.invocationsFor(redelivery)).isZero();
            softly.assertThat(secondGroupListener.invocationsFor(redelivery)).isZero();
            softly.assertThat(processedEventExists(MAIN_GROUP, eventId)).isTrue();
            softly.assertThat(processedEventExists(SECOND_GROUP, eventId)).isTrue();
        });
    }

    @Test
    void failedAttemptRollsBackAndRetryProcessesEventOnce() {
        final UUID eventId = UUID.randomUUID();
        final String payload = TestIdempotentKafkaListener.FAIL_ONCE_PREFIX + eventId;

        send(eventId.toString(), payload);
        awaitConsumed();

        // the retry inserts the same business row again: it only succeeds if the first attempt was rolled back
        assertSoftly(softly -> {
            softly.assertThat(testIdempotentKafkaListener.invocationsFor(payload)).isEqualTo(2);
            softly.assertThat(processedEventExists(MAIN_GROUP, eventId)).isTrue();
            softly.assertThat(businessRowExists(payload)).isTrue();
        });
    }

    @Test
    void missingEventIdHeaderNeverReachesBusinessLogic() {
        final String payload = "missing-header-" + UUID.randomUUID();

        send(null, payload);
        awaitConsumed();

        assertSoftly(softly -> {
            softly.assertThat(testIdempotentKafkaListener.invocationsFor(payload)).isZero();
            softly.assertThat(businessRowExists(payload)).isFalse();
        });
    }

    @Test
    void nonUuidEventIdHeaderNeverReachesBusinessLogic() {
        final String payload = "non-uuid-header-" + UUID.randomUUID();

        send("not-a-uuid", payload);
        awaitConsumed();

        assertSoftly(softly -> {
            softly.assertThat(testIdempotentKafkaListener.invocationsFor(payload)).isZero();
            softly.assertThat(businessRowExists(payload)).isFalse();
        });
    }

    private void send(String eventIdHeader, String payload) {
        final ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, payload);
        if (eventIdHeader != null) {
            record.headers().add("id", eventIdHeader.getBytes(StandardCharsets.UTF_8));
        }
        kafkaTemplate.send(record).join();
    }

    // single-partition topic: once the barrier is processed by both groups, every earlier record (incl. retries) is done
    private void awaitConsumed() {
        final String barrier = "barrier-" + UUID.randomUUID();
        send(UUID.randomUUID().toString(), barrier);
        await().atMost(Duration.ofSeconds(30))
                .until(() -> businessRowExists(barrier) && secondGroupListener.invocationsFor(barrier) == 1);
    }

    private boolean processedEventExists(String consumerGroup, UUID eventId) {
        return jdbcClient.sql("SELECT exists(SELECT 1 FROM processed_events WHERE consumer_group = ? AND event_id = ?)")
                .param(consumerGroup)
                .param(eventId)
                .query(Boolean.class)
                .single();
    }

    private boolean businessRowExists(String id) {
        return jdbcClient.sql("SELECT exists(SELECT 1 FROM business_record WHERE id = ?)")
                .param(id)
                .query(Boolean.class)
                .single();
    }

    @RequiredArgsConstructor
    static class TestIdempotentKafkaListener {

        static final String FAIL_ONCE_PREFIX = "fail-once-";

        private final JdbcClient jdbcClient;
        // counts every run, including rolled-back ones: stands in for a non-transactional side effect (e.g. a PSP call)
        private final Map<String, AtomicInteger> invocations = new ConcurrentHashMap<>();

        @Idempotent
        @KafkaListener(topics = TOPIC)
        public void on(String payload) {
            final int invocation = invocations.computeIfAbsent(payload, key -> new AtomicInteger()).incrementAndGet();

            jdbcClient.sql("INSERT INTO business_record (id) VALUES (?)").param(payload).update();

            if (payload.startsWith(FAIL_ONCE_PREFIX) && invocation == 1) {
                throw new RuntimeException("boom");
            }
        }

        int invocationsFor(String payload) {
            final AtomicInteger counter = invocations.get(payload);
            return counter == null ? 0 : counter.get();
        }
    }

    static class SecondGroupIdempotentKafkaListener {

        private final Map<String, AtomicInteger> invocations = new ConcurrentHashMap<>();

        @Idempotent
        @KafkaListener(topics = TOPIC, groupId = SECOND_GROUP)
        public void on(String payload) {
            invocations.computeIfAbsent(payload, key -> new AtomicInteger()).incrementAndGet();
        }

        int invocationsFor(String payload) {
            final AtomicInteger counter = invocations.get(payload);
            return counter == null ? 0 : counter.get();
        }
    }
}
