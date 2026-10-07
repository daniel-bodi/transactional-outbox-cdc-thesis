package dev.danielbodi.thesis.idempotency.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

/**
 * @author danielbodi
 */
@RequiredArgsConstructor
public class ProcessedEventRepository {

    private static final String INSERT = """
            INSERT INTO processed_events (consumer_group, event_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """;

    private static final String INCREMENT_DUPLICATE_DELIVERIES = """
            UPDATE processed_events
            SET duplicate_deliveries = duplicate_deliveries + 1
            WHERE consumer_group = ? AND event_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public boolean insertIfAbsent(String consumerGroup, UUID eventId) {
        return jdbcTemplate.update(INSERT, consumerGroup, eventId) == 1;
    }

    public void incrementDuplicateDeliveries(String consumerGroup, UUID eventId) {
        jdbcTemplate.update(INCREMENT_DUPLICATE_DELIVERIES, consumerGroup, eventId);
    }
}
