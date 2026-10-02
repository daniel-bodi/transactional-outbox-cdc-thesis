package dev.danielbodi.thesis.idempotency;

import dev.danielbodi.thesis.idempotency.aspect.IdempotencyAspect;
import dev.danielbodi.thesis.idempotency.kafka.ConsumedRecordInterceptor;
import dev.danielbodi.thesis.idempotency.persistence.ProcessedEventsSchemaInitializer;
import dev.danielbodi.thesis.idempotency.persistence.ProcessedEventRepository;
import dev.danielbodi.thesis.idempotency.validation.IdempotentContractValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author danielbodi
 */
class IdempotentConsumerStarterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdempotentConsumerAutoConfiguration.class))
            .withPropertyValues("idempotent-consumer.schema-initialization.enabled=false")
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class));

    @Test
    void autoConfigurationRegistersIdempotentConsumerBeans() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(ProcessedEventRepository.class)
                .hasSingleBean(IdempotencyAspect.class)
                .hasSingleBean(ConsumedRecordInterceptor.class)
                .hasSingleBean(IdempotentContractValidator.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void autoConfigurationBacksOffWhenApplicationDefinesItsOwnRecordInterceptor() {
        final RecordInterceptor<Object, Object> applicationBean = mock(RecordInterceptor.class);

        contextRunner
                .withBean("applicationInterceptor", RecordInterceptor.class, () -> applicationBean)
                .run(context -> assertThat(context)
                        .doesNotHaveBean(ConsumedRecordInterceptor.class)
                        .hasSingleBean(RecordInterceptor.class));
    }

    @Test
    void autoConfigurationBacksOffWhenApplicationDefinesItsOwnRepository() {
        final ProcessedEventRepository applicationBean = mock(ProcessedEventRepository.class);

        contextRunner
                .withBean("applicationRepository", ProcessedEventRepository.class, () -> applicationBean)
                .run(context -> assertThat(context)
                        .hasSingleBean(ProcessedEventRepository.class)
                        .getBean(ProcessedEventRepository.class)
                        .isSameAs(applicationBean));
    }

    @Test
    void autoConfigurationOmitsSchemaInitializerWhenDisabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ProcessedEventsSchemaInitializer.class));
    }
}
