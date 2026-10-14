package dev.danielbodi.thesis.measurement.result.model;

import dev.danielbodi.thesis.measurement.load.RequestOutcome;

import java.util.List;
import java.util.Map;

/**
 * @author danielbodi
 */
public record Timeline(
        List<Outage> outages,
        Long recoveryMillis,
        Map<RequestOutcome, Long> requestOutcomesDuringOutages,
        long committedWithoutCreatedResponse) {
}
