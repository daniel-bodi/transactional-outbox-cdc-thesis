package dev.danielbodi.thesis.measurement.database;

import java.time.Instant;
import java.util.UUID;

/**
 * @author danielbodi
 */
public record SubscriptionRow(
        UUID subscriptionId,
        UUID customerId,
        UUID paymentReference,
        String status,
        Instant createdAt,
        Instant completedAt) {
}
