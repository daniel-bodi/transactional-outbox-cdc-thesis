package dev.danielbodi.thesis.event;

/**
 * @author danielbodi
 */
public interface IntegrationEventPublisher {
    void publish(IntegrationEvent event);
}
