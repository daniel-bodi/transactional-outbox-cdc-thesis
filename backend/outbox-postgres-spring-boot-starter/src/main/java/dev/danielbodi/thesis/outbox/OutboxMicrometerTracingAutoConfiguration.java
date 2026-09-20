package dev.danielbodi.thesis.outbox;

import dev.danielbodi.thesis.outbox.tracing.MicrometerOutboxTraceContextProvider;
import dev.danielbodi.thesis.outbox.tracing.OutboxTraceContextProvider;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @author danielbodi
 */
@AutoConfiguration(
        afterName = OutboxMicrometerTracingAutoConfiguration.BRAVE_AUTOCONFIGURATION_CLASS_NAME,
        before = {OutboxFallbackTracingAutoConfiguration.class, OutboxPersistenceAutoConfiguration.class})
@ConditionalOnClass({Tracer.class, Propagator.class})
@ConditionalOnBean({Tracer.class, Propagator.class})
public class OutboxMicrometerTracingAutoConfiguration {

    static final String BRAVE_AUTOCONFIGURATION_CLASS_NAME =
            "org.springframework.boot.micrometer.tracing.brave.autoconfigure.BraveAutoConfiguration";

    @Bean
    @ConditionalOnMissingBean(OutboxTraceContextProvider.class)
    OutboxTraceContextProvider micrometerOutboxTraceContextProvider(Tracer tracer, Propagator propagator) {
        return new MicrometerOutboxTraceContextProvider(tracer, propagator);
    }
}
