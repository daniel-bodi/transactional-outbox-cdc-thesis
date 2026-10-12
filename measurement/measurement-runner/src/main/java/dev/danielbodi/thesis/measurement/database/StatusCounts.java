package dev.danielbodi.thesis.measurement.database;

/**
 * @author danielbodi
 */
// the subscriptions by status, the live indicator of a run
public record StatusCounts(long pending, long active, long failed) {

    @Override
    public String toString() {
        return "PENDING=%d ACTIVE=%d FAILED=%d".formatted(pending, active, failed);
    }
}
