package dev.danielbodi.thesis.outbox.event;

/**
 * @author danielbodi
 */
public interface OutboxEvent {

    String getAggregateId();
    String getAggregateType();

    default String getEventType() {
        return getClass().getSimpleName();
    }
}
