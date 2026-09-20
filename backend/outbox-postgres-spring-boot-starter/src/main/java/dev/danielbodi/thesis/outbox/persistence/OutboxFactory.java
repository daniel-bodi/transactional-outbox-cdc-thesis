package dev.danielbodi.thesis.outbox.persistence;

import dev.danielbodi.thesis.outbox.event.OutboxEvent;
import dev.danielbodi.thesis.outbox.tracing.OutboxTraceContextProvider;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * @author danielbodi
 */
@RequiredArgsConstructor
public class OutboxFactory {

    private final ObjectMapper objectMapper;
    private final OutboxTraceContextProvider traceContextProvider;

    public Outbox from(OutboxEvent event) {
        final UUID id = UUID.randomUUID();
        final String payload = objectMapper.writeValueAsString(event);
        final String traceId = traceContextProvider.currentTraceContext();

        return new Outbox(id, traceId, event.getAggregateId(), event.getAggregateType(), event.getEventType(), payload);
    }
}
