package dev.danielbodi.thesis.outbox.persistence;

import java.util.UUID;

/**
 * @author danielbodi
 */
public record Outbox(
        UUID id,
        String traceId,
        String aggregateId,
        String aggregateType,
        String type,
        String payload
) {
}
