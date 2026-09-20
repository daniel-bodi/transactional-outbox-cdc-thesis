package dev.danielbodi.thesis.outbox;

import dev.danielbodi.thesis.outbox.event.OutboxEventPublisher;
import dev.danielbodi.thesis.outbox.persistence.OutboxFactory;
import dev.danielbodi.thesis.outbox.persistence.OutboxRepository;
import dev.danielbodi.thesis.outbox.persistence.OutboxSchemaInitializer;
import dev.danielbodi.thesis.outbox.tracing.MicrometerOutboxTraceContextProvider;
import dev.danielbodi.thesis.outbox.tracing.DefaultFallbackOutboxTraceContextProvider;
import dev.danielbodi.thesis.outbox.tracing.OutboxTraceContextProvider;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author danielbodi
 */
class OutboxStarterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    OutboxPersistenceAutoConfiguration.class,
                    OutboxMicrometerTracingAutoConfiguration.class,
                    OutboxFallbackTracingAutoConfiguration.class))
            .withPropertyValues("outbox.schema-initialization.enabled=false")
            // mandatory for auto-configuration
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(ObjectMapper.class, () -> mock(ObjectMapper.class));

    @Test
    void autoConfigurationRegistersOutboxBeans() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(OutboxRepository.class)
                .hasSingleBean(OutboxFactory.class)
                .hasSingleBean(OutboxEventPublisher.class));
    }

    @Test
    void autoConfigurationNotRegistersWhenApplicationDefinesItsOwnPublisher() {
        final OutboxEventPublisher applicationBean = mock(OutboxEventPublisher.class);

        contextRunner
                .withBean("applicationPublisher", OutboxEventPublisher.class, () -> applicationBean)
                .run(context -> assertThat(context)
                        .hasSingleBean(OutboxEventPublisher.class)
                        .getBean(OutboxEventPublisher.class)
                        .isSameAs(applicationBean));
    }

    @Test
    void autoConfigurationNotRegistersWhenApplicationDefinesItsOwnRepository() {
        final OutboxRepository applicationBean = mock(OutboxRepository.class);

        contextRunner
                .withBean("applicationRepository", OutboxRepository.class, () -> applicationBean)
                .run(context -> assertThat(context)
                        .hasSingleBean(OutboxRepository.class)
                        .getBean(OutboxRepository.class)
                        .isSameAs(applicationBean));
    }

    @Test
    void autoConfigurationOmitsSchemaInitializerWhenDisabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(OutboxSchemaInitializer.class));
    }

    @Test
    void autoConfigurationRegistersDefaultFallbackTraceContextProviderByDefault() {
        // case when micrometer-tracing dependency is not present, so OutboxMicrometerTracingAutoConfiguration
        // does not load at all — the fallback provider is used
        contextRunner
                .withClassLoader(new FilteredClassLoader(Tracer.class, Propagator.class))
                .run(context -> assertThat(context)
                        .hasSingleBean(OutboxTraceContextProvider.class)
                        .getBean(OutboxTraceContextProvider.class)
                        .isInstanceOf(DefaultFallbackOutboxTraceContextProvider.class));
    }

    @Test
    void autoConfigurationRegistersDefaultFallbackTraceContextProviderByDefaultWhenNoMicrometerBeansAvailable() {
        // case when micrometer-tracing dependency is present on the classpath, but no Tracer/Propagator beans are in the
        // context, so @ConditionalOnBean skips OutboxMicrometerTracingAutoConfiguration — the fallback provider is used
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(OutboxTraceContextProvider.class)
                .getBean(OutboxTraceContextProvider.class)
                .isInstanceOf(DefaultFallbackOutboxTraceContextProvider.class));
    }

    @Test
    void autoConfigurationRegistersMicrometerOutboxTraceContextProviderWhenTracerAndPropagatorAvailable() {
        contextRunner
                .withBean(Tracer.class, () -> mock(Tracer.class))
                .withBean(Propagator.class, () -> mock(Propagator.class))
                .run(context -> assertThat(context)
                        .hasSingleBean(OutboxTraceContextProvider.class)
                        .getBean(OutboxTraceContextProvider.class)
                        .isInstanceOf(MicrometerOutboxTraceContextProvider.class));
    }

    @Test
    void autoConfigurationNotRegistersWhenApplicationDefinesItsOwnOutboxTraceContextProvider() {
        final OutboxTraceContextProvider applicationBean = mock(OutboxTraceContextProvider.class);

        contextRunner
                .withBean("applicationTraceContextProvider", OutboxTraceContextProvider.class, () -> applicationBean)
                .run(context -> assertThat(context)
                        .hasSingleBean(OutboxTraceContextProvider.class)
                        .getBean(OutboxTraceContextProvider.class)
                        .isSameAs(applicationBean));
    }
}
