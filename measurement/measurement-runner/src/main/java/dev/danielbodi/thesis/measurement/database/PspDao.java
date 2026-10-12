package dev.danielbodi.thesis.measurement.database;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author danielbodi
 */
@Component
@RequiredArgsConstructor
public class PspDao {

    private final JdbcClient pspJdbcClient;

    // reference -> number of charges executed by the PSP mock
    public Map<UUID, Long> chargeCountsByReference() {
        return pspJdbcClient
                .sql("SELECT reference, COUNT(*) AS count FROM charge GROUP BY reference")
                .query((rs, _) -> Map.entry(rs.getObject("reference", UUID.class), rs.getLong("count")))
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // repeated requests answered by the idempotency key, without a new charge
    public long duplicateRequests() {
        return pspJdbcClient
                .sql("SELECT COALESCE(SUM(duplicate_requests), 0) FROM charge")
                .query(Long.class)
                .single();
    }
}
