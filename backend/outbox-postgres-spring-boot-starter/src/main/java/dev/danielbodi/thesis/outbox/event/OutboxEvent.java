package dev.danielbodi.thesis.outbox.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author danielbodi
 */
public interface OutboxEvent {

    @JsonIgnore
    String getAggregateId();

    @JsonIgnore
    String getAggregateType();

    @JsonIgnore
    default String getEventType() {
        return getClass().getSimpleName();
    }
}
