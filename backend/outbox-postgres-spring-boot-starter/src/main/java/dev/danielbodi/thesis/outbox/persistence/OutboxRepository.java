package dev.danielbodi.thesis.outbox.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author danielbodi
 */
@RequiredArgsConstructor
public class OutboxRepository {

    private static final String INSERT = """
            INSERT INTO outbox (id, trace_id, aggregate_id, aggregate_type, type, payload)
            VALUES (?, ?, ?, ?, ?, ?::jsonb)
            """;

    private final JdbcTemplate jdbcTemplate;

    public void save(Outbox outbox) {
        jdbcTemplate.update(INSERT,
                outbox.id(),
                outbox.traceId(),
                outbox.aggregateId(),
                outbox.aggregateType(),
                outbox.type(),
                outbox.payload());
    }
}
