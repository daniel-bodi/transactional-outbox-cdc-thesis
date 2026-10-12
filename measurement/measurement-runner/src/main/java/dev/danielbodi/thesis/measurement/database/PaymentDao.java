package dev.danielbodi.thesis.measurement.database;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * @author danielbodi
 */
@Component
@RequiredArgsConstructor
public class PaymentDao {

    private final JdbcClient paymentJdbcClient;

    // the completed payments (SUCCEEDED or FAILED) by reference
    public Set<UUID> completedPaymentReferences() {
        return Set.copyOf(paymentJdbcClient
                .sql("SELECT reference FROM payment")
                .query(UUID.class)
                .list());
    }

    // D: redeliveries skipped by the idempotent consumer of the payment-service
    public long duplicateDeliveries() {
        return paymentJdbcClient
                .sql("SELECT COALESCE(SUM(duplicate_deliveries), 0) FROM processed_events")
                .query(Long.class)
                .single();
    }
}
