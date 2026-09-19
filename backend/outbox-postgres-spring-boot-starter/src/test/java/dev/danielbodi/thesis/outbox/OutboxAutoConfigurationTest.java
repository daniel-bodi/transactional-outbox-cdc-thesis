package dev.danielbodi.thesis.outbox;

import dev.danielbodi.thesis.outbox.event.OutboxEventPublisher;
import dev.danielbodi.thesis.outbox.persistence.OutboxFactory;
import dev.danielbodi.thesis.outbox.persistence.OutboxRepository;
import dev.danielbodi.thesis.outbox.persistence.OutboxSchemaInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author danielbodi
 */
class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
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
}
