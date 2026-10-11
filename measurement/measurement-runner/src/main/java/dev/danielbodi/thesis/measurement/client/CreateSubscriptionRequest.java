package dev.danielbodi.thesis.measurement.client;

import java.util.UUID;

/**
 * @author danielbodi
 */
public record CreateSubscriptionRequest(UUID customerId, UUID planId) {
}
