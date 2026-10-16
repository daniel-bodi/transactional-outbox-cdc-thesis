package dev.danielbodi.thesis.measurement.result.model;

import dev.danielbodi.thesis.measurement.scenario.Scenario;
import dev.danielbodi.thesis.measurement.scenario.Variant;

import java.time.Instant;
import java.util.Map;

/**
 * @author danielbodi
 */
public record RunMetadata(
        String id,
        String campaignId,
        int repetition,
        Scenario scenario,
        Variant variant,
        Map<String, String> environment,
        int warmUpCount,
        int loadCount,
        int loadRatePerSecond,
        Instant startedAt,
        Instant finishedAt) {
}
