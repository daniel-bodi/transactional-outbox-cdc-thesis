package dev.danielbodi.thesis.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author danielbodi
 */
public interface IntegrationEvent {

    @JsonIgnore
    String getAggregateId();

    @JsonIgnore
    String getAggregateType();

    @JsonIgnore
    String getEventType();
}
