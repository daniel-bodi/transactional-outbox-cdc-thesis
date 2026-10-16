package dev.danielbodi.thesis.measurement.result.model;

import dev.danielbodi.thesis.measurement.scenario.Scenario;

import java.time.Instant;
import java.util.List;

/**
 * @author danielbodi
 */
public record CampaignResult(
        String id,
        List<Scenario> scenarios,
        int repetitions,
        int warmUpCount,
        int loadCount,
        int loadRatePerSecond,
        Instant startedAt,
        Instant finishedAt,
        int runCount,
        List<FailedRunEntry> failedRuns) {
}
