package dev.danielbodi.thesis.measurement.evaluation.model;

import java.util.List;
import java.util.UUID;

/**
 * @author danielbodi
 */
public record InvariantResult(Verdict verdict, List<UUID> violations) {
}
