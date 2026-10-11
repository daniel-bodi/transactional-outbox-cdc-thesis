package dev.danielbodi.thesis.outbox;

import dev.danielbodi.thesis.outbox.tracing.DefaultFallbackOutboxTraceContextProvider;
import dev.danielbodi.thesis.outbox.tracing.OutboxTraceContextProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @author danielbodi
 */
@AutoConfiguration(
        after = OutboxMicrometerTracingAutoConfiguration.class,
        before = OutboxPersistenceAutoConfiguration.class)
public class OutboxFallbackTracingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OutboxTraceContextProvider.class)
    OutboxTraceContextProvider defaultFallbackOutboxTraceContextProvider() {
        return new DefaultFallbackOutboxTraceContextProvider();
    }
}
