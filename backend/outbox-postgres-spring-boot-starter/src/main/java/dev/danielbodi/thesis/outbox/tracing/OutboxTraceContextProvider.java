package dev.danielbodi.thesis.outbox.tracing;

/**
 * @author danielbodi
 */
public interface OutboxTraceContextProvider {

    /**
     * Returns the current trace context, or {@code null} when none is available.
     */
    String currentTraceContext();
}
