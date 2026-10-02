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

    private final JdbcTemplate jdbcTemplate;

    public boolean insertIfAbsent(String consumerGroup, UUID eventId) {
        return jdbcTemplate.update(INSERT, consumerGroup, eventId) == 1;
    }
}
