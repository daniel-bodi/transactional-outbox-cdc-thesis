package dev.danielbodi.thesis.outbox;

import dev.danielbodi.thesis.outbox.tracing.DefaultFallbackOutboxTraceContextProvider;
import dev.danielbodi.thesis.outbox.tracing.OutboxTraceContextProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers the default fallback trace context provider unless another
 * {@link OutboxTraceContextProvider} bean has already been contributed — for
 * example by {@link OutboxMicrometerTracingAutoConfiguration} or the application.
 *
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
