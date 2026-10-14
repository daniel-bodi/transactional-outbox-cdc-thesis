package dev.danielbodi.thesis.measurement.result.model;

import java.util.List;

/**
 * @author danielbodi
 */
// everything the figures may need later: every load request, subscription and container event with its timestamp
public record RawData(
        List<RequestEntry> requests,
        List<SubscriptionEntry> subscriptions,
        List<ContainerEventEntry> containerEvents) {
}
