package dev.danielbodi.thesis.outbox.tracing;

/**
 * @author danielbodi
 */
public interface OutboxTraceContextProvider {

    String currentTraceContext();
}
