package dev.danielbodi.thesis.outbox;

import dev.danielbodi.thesis.outbox.event.OutboxEvent;
import dev.danielbodi.thesis.outbox.event.OutboxEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author danielbodi
 */
@SpringBootTest
@Testcontainers
class OutboxStarterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");

    @SpringBootApplication
    static class OutboxTestApplication {
    }

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS business_record (id VARCHAR(255) PRIMARY KEY)");
        jdbcTemplate.update("DELETE FROM business_record");
        jdbcTemplate.update("DELETE FROM outbox");
    }

    @Test
    void schemaInitializerCreatesOutboxTableOnStartup() {
        final Integer tables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'outbox'",
                Integer.class);

        assertThat(tables).isEqualTo(1);
    }

    @Test
    void outboxEventPublisherRejectsPublishingOutsideOfTransaction() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> outboxEventPublisher.publish(new TestEvent()))
                    .isInstanceOf(IllegalTransactionStateException.class);
            softly.assertThat(outboxRowCount()).isZero();
        });
    }

    @Test
    void outboxEventPublisherPersistsEventWhenTransactionCommits() {
        transactionTemplate.executeWithoutResult(status -> outboxEventPublisher.publish(new TestEvent()));

        assertSoftly(softly -> {
            // record count
            softly.assertThat(outboxRowCount()).isEqualTo(1);
            // record data
            softly.assertThat(singleStringColumn("aggregate_id")).isEqualTo("subscription-1");
            softly.assertThat(singleStringColumn("aggregate_type")).isEqualTo("Subscription");
            softly.assertThat(singleStringColumn("type")).isEqualTo("TestEvent");
            final String amountValue = jdbcTemplate.queryForObject(
                    "SELECT payload ->> 'amount' FROM outbox", String.class);
            softly.assertThat(amountValue).isEqualTo("100");
        });
    }

    @Test
    void createdAtFieldGetsPopulatedByDatabase() {
        transactionTemplate.executeWithoutResult(status -> outboxEventPublisher.publish(new TestEvent()));

        final Integer withoutTimestamp = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox WHERE created_at IS NULL", Integer.class);

        assertThat(withoutTimestamp).isZero();
    }

    @Test
    void discardsOutboxRecordTogetherWithBusinessWrite() {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("INSERT INTO business_record (id) VALUES (?)", "record-1");
            outboxEventPublisher.publish(new TestEvent());
            status.setRollbackOnly();
        });

        assertSoftly(softly -> {
            softly.assertThat(businessRowCount()).isZero();
            softly.assertThat(outboxRowCount()).isZero();
        });
    }

    @Test
    void keepsOutboxRecordTogetherWithBusinessWriteOnCommit() {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("INSERT INTO business_record (id) VALUES (?)", "record-1");
            outboxEventPublisher.publish(new TestEvent());
        });

        assertSoftly(softly -> {
            softly.assertThat(businessRowCount()).isEqualTo(1);
            softly.assertThat(outboxRowCount()).isEqualTo(1);
        });
    }

    private Integer outboxRowCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM outbox", Integer.class);
    }

    private Integer businessRowCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM business_record", Integer.class);
    }

    private String singleStringColumn(String column) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM outbox", String.class);
    }

    static final class TestEvent implements OutboxEvent {

        @Override
        public String getTraceId() {
            return "trace-1";
        }

        @Override
        public String getAggregateId() {
            return "subscription-1";
        }

        @Override
        public String getAggregateType() {
            return "Subscription";
        }

        public String getAmount() {
            return "100";
        }
    }
}
