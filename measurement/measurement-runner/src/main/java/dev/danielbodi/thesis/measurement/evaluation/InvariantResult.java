package dev.danielbodi.thesis.measurement.evaluation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @author danielbodi
 */
public record InvariantResult(Verdict verdict, List<UUID> violations) {

    public static InvariantResult of(Collection<UUID> violations) {
        return new InvariantResult(violations.isEmpty() ? Verdict.PASS : Verdict.FAIL, List.copyOf(violations));
    }
}
