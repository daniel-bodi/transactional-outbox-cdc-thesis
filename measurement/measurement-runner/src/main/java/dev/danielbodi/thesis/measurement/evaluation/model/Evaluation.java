package dev.danielbodi.thesis.measurement.evaluation.model;

import dev.danielbodi.thesis.measurement.database.StatusCounts;
import dev.danielbodi.thesis.measurement.database.SubscriptionRow;
import dev.danielbodi.thesis.measurement.load.RequestOutcome;

import java.util.List;
import java.util.Map;

/**
 * @author danielbodi
 */
public record Evaluation(
        boolean converged,
        Map<Invariant, InvariantResult> invariantResults,
        StatusCounts statusCounts,
        long payments,
        long charges,
        Map<String, Long> duplicateDeliveries,
        long duplicateRequests,
        Map<RequestOutcome, Long> requestOutcomes,
        Latency latency,
        List<SubscriptionRow> subscriptions) {
}
