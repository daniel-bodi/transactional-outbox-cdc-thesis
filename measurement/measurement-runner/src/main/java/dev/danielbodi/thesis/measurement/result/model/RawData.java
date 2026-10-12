package dev.danielbodi.thesis.measurement.result.model;

import java.util.List;

/**
 * @author danielbodi
 */
// everything the figures may need later: every load request and every subscription with its timestamps
public record RawData(List<RequestEntry> requests, List<SubscriptionEntry> subscriptions) {
}
