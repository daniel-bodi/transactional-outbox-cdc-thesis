package dev.danielbodi.thesis.outbox.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author danielbodi
 */
@Slf4j
@RequiredArgsConstructor
public class MicrometerOutboxTraceContextProvider implements OutboxTraceContextProvider {

    static final String W3C_TRACEPARENT_HEADER = "traceparent";

    private final Tracer tracer;
    private final Propagator propagator;

    @PostConstruct
    void logActivation() {
        log.info("Outbox trace context provider active: [{}] with tracer [{}] and propagator [{}]",
                getClass().getSimpleName(), tracer.getClass().getSimpleName(), propagator.getClass().getSimpleName());
    }

    @Override
    public String currentTraceContext() {
        final Span current = tracer.currentSpan();
        if (current == null) {
            return null;
        }

        // propagator puts the current span's trace context into the carrier map; only W3C is supported,
        // because the Debezium connectors publish the value under the traceparent header
        final Map<String, String> carrier = new HashMap<>();
        propagator.inject(current.context(), carrier, Map::put);

        return carrier.get(W3C_TRACEPARENT_HEADER);
    }
}
