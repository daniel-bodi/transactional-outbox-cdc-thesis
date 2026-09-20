package dev.danielbodi.thesis.outbox.tracing;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * @author danielbodi
 */
@Slf4j
public class DefaultFallbackOutboxTraceContextProvider implements OutboxTraceContextProvider {

    @PostConstruct
    void logActivation() {
        log.info("Outbox trace context provider active: [{}] — no trace context will be recorded",
                getClass().getSimpleName());
    }

    @Override
    public String currentTraceContext() {
        return null;
    }
}
