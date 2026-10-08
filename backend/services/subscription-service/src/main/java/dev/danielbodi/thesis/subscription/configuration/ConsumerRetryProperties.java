package dev.danielbodi.thesis.subscription.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author danielbodi
 */
@ConfigurationProperties("consumer.retry")
public record ConsumerRetryProperties(int maxRetries, Duration initialInterval, double multiplier, Duration maxInterval) {
}
