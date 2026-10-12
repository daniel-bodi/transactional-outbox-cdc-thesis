package dev.danielbodi.thesis.measurement.result.model;

import dev.danielbodi.thesis.measurement.load.RequestOutcome;

import java.util.Map;

/**
 * @author danielbodi
 */
public record Metrics(
        Latency latencyMillis,
        Map<String, Long> duplicateDeliveries,
        long duplicateRequests,
        Map<RequestOutcome, Long> requestOutcomes) {
}
