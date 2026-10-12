package dev.danielbodi.thesis.measurement.result.model;

import java.time.Instant;
import java.util.UUID;

/**
 * @author danielbodi
 */
public record SubscriptionEntry(
        UUID subscriptionId,
        UUID customerId,
        UUID paymentReference,
        String status,
        Instant createdAt,
        Instant completedAt) {
}
