package dev.danielbodi.thesis.pspmock.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author danielbodi
 */
@ConfigurationProperties("psp.idempotency-key")
public record IdempotencyKeyProperties(boolean enabled) {
}
