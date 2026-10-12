package dev.danielbodi.thesis.measurement.database;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
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

    private StatusCounts statusCounts() {
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

    // `created_at` is the database clock, `completed_at` is the JVM clock of the subscription-service
    // both run on the same host clock under Docker, so they can be compared
    public List<SubscriptionRow> subscriptions() {
        return subscriptionJdbcClient
                .sql("""
                        SELECT s.id, s.customer_id, pr.id AS payment_reference, s.status, s.created_at, s.completed_at
                        FROM subscription s
                        JOIN payment_request pr ON pr.subscription_id = s.id
                        """)
                .query((rs, _) -> new SubscriptionRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("customer_id", UUID.class),
                        rs.getObject("payment_reference", UUID.class),
                        rs.getString("status"),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                        Optional.ofNullable(rs.getObject("completed_at", OffsetDateTime.class))
                                .map(OffsetDateTime::toInstant)
                                .orElse(null)))
                .list();
    }

    public long duplicateDeliveries() {
        return subscriptionJdbcClient
                .sql("SELECT COALESCE(SUM(duplicate_deliveries), 0) FROM processed_events")
                .query(Long.class)
                .single();
    }
}
