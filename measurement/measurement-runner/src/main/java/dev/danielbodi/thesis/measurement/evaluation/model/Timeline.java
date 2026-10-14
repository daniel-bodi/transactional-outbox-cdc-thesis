package dev.danielbodi.thesis.measurement.evaluation.model;

import dev.danielbodi.thesis.measurement.docker.ContainerEvent;
import dev.danielbodi.thesis.measurement.load.RequestOutcome;

import java.util.List;
import java.util.Map;

/**
 * @author danielbodi
 */
public record Timeline(
        List<Outage> outages,
        // T: from the first crash until the last subscription affected by the outages completed
        // null without an outage, if a container never became healthy again, or if an affected subscription never completed
        Long recoveryMillis,
        // A: the outcomes of the requests sent while a container was unavailable
        Map<RequestOutcome, Long> requestOutcomesDuringOutages,
        // the requests without a 201 response whose subscription was committed anyway (e.g. the crashed one)
        long committedWithoutCreatedResponse,
        List<ContainerEvent> containerEvents) {
}
