package dev.danielbodi.thesis.measurement.evaluation;

import dev.danielbodi.thesis.measurement.database.StatusCounts;

import java.util.Map;

/**
 * @author danielbodi
 */
public record Evaluation(
        Map<Invariant, InvariantResult> invariantResults,
        StatusCounts subscriptions,
        long payments,
        long charges,
        Map<String, Long> duplicateDeliveries,
        long duplicateRequests) {
}
