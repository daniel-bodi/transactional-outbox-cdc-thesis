package dev.danielbodi.thesis.measurement.database;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author danielbodi
 */
@Component
@RequiredArgsConstructor
public class SubscriptionDao {

    private final JdbcClient subscriptionJdbcClient;

    public Optional<StatusCounts> tryStatusCounts() {
        try {
            return Optional.of(statusCounts());
        } catch (DataAccessException _) {
            // empty if the subscription database is not reachable
            // e.g. while a scenario has stopped it
            return Optional.empty();
        }
    }

    public StatusCounts statusCounts() {
        final Map<String, Long> counts = subscriptionJdbcClient
                .sql("SELECT status, COUNT(*) AS count FROM subscription GROUP BY status")
                .query((rs, _) -> Map.entry(rs.getString("status"), rs.getLong("count")))
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new StatusCounts(
                counts.getOrDefault("PENDING", 0L),
                counts.getOrDefault("ACTIVE", 0L),
                counts.getOrDefault("FAILED", 0L));
    }

    // the reference of a payment request is its id
    public Set<UUID> paymentRequestReferences() {
        return Set.copyOf(subscriptionJdbcClient
                .sql("SELECT id FROM payment_request")
                .query(UUID.class)
                .list());
    }

    public Set<UUID> pendingPaymentRequestReferences() {
        return Set.copyOf(subscriptionJdbcClient
                .sql("""
                        SELECT pr.id
                        FROM payment_request pr
                        JOIN subscription s ON s.id = pr.subscription_id
                        WHERE s.status = 'PENDING'
                        """)
                .query(UUID.class)
                .list());
    }

    public long duplicateDeliveries() {
        return subscriptionJdbcClient
                .sql("SELECT COALESCE(SUM(duplicate_deliveries), 0) FROM processed_events")
                .query(Long.class)
                .single();
    }
}
