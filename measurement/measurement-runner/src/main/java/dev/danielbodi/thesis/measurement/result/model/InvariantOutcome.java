package dev.danielbodi.thesis.measurement.result.model;

import dev.danielbodi.thesis.measurement.evaluation.model.Verdict;

import java.util.List;
import java.util.UUID;

/**
 * @author danielbodi
 */
public record InvariantOutcome(String thesisId, Verdict verdict, int violationCount, List<UUID> violations) {
}
