package dev.danielbodi.thesis.subscription.create;

import java.util.UUID;

/**
 * @author danielbodi
 */
public record CreateSubscriptionRequest(UUID customerId, UUID planId) {
}
