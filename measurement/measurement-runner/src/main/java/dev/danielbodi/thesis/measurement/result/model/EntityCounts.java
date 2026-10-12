package dev.danielbodi.thesis.measurement.result.model;

/**
 * @author danielbodi
 */
public record EntityCounts(SubscriptionStatusCounts subscriptions, long payments, long charges) {
}
