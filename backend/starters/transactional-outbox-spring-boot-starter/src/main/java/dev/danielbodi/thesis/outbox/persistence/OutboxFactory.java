package dev.danielbodi.thesis.outbox.persistence;

import dev.danielbodi.thesis.event.IntegrationEvent;
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

    public Outbox from(IntegrationEvent event) {
        final UUID id = UUID.randomUUID();
        final String payload = objectMapper.writeValueAsString(event);
        final String traceContext = traceContextProvider.currentTraceContext();

        return new Outbox(id, traceContext, event.getAggregateId(), event.getAggregateType(), event.getEventType(), payload);
    }
}
