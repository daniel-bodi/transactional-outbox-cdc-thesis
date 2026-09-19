package dev.danielbodi.thesis.outbox.persistence;

import dev.danielbodi.thesis.outbox.event.OutboxEvent;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * @author danielbodi
 */
@RequiredArgsConstructor
public class OutboxFactory {

    private final ObjectMapper objectMapper;

    public Outbox from(OutboxEvent outboxEvent) {
        return from(outboxEvent.getTraceId(),
                outboxEvent.getAggregateId(),
                outboxEvent.getAggregateType(),
                outboxEvent.getEventType(),
                outboxEvent);
    }

    public Outbox from(String traceId, String aggregateId, String aggregateType, String eventType, Object payload) {
        return new Outbox(
                UUID.randomUUID(),
                traceId,
                aggregateId,
                aggregateType,
                eventType,
                objectMapper.writeValueAsString(payload));
    }
}
