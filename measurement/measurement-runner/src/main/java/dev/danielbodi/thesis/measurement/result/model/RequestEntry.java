package dev.danielbodi.thesis.measurement.result.model;

import dev.danielbodi.thesis.measurement.load.RequestOutcome;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * @author danielbodi
 */
public record RequestEntry(
        int index,
        UUID customerId,
        Instant sentAt,
        Instant finishedAt,
        RequestOutcome outcome,
        HttpStatus httpStatus,
        String error) {
}
