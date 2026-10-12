package dev.danielbodi.thesis.measurement.result.model;

import dev.danielbodi.thesis.measurement.evaluation.model.Invariant;

import java.util.Map;

/**
 * @author danielbodi
 */
// the result file of one run
public record RunResult(
        RunMetadata metadata,
        boolean converged,
        Map<Invariant, InvariantOutcome> invariants,
        EntityCounts counts,
        Metrics metrics,
        RawData rawData) {
}
