package dev.danielbodi.thesis.subscription.activation;

import java.util.UUID;

/**
 * @author danielbodi
 */
public record PaymentFailedEvent(UUID reference) {
}
