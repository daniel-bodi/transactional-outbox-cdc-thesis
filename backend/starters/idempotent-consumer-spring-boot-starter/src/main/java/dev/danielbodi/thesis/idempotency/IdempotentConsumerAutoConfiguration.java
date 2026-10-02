package dev.danielbodi.thesis.idempotency;

import dev.danielbodi.thesis.idempotency.aspect.IdempotencyAspect;
import dev.danielbodi.thesis.idempotency.kafka.ConsumedRecordInterceptor;
import dev.danielbodi.thesis.idempotency.persistence.ProcessedEventsSchemaInitializer;
import dev.danielbodi.thesis.idempotency.persistence.ProcessedEventRepository;
import dev.danielbodi.thesis.idempotency.validation.IdempotentContractValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * @author danielbodi
 */
@AutoConfiguration(after = {DataSourceAutoConfiguration.class,
                            JdbcTemplateAutoConfiguration.class,
                            TransactionAutoConfiguration.class})
@ConditionalOnSingleCandidate(DataSource.class)
public class IdempotentConsumerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "idempotent-consumer.schema-initialization.enabled", matchIfMissing = true)
    ProcessedEventsSchemaInitializer processedEventsSchemaInitializer(DataSource dataSource) {
        return new ProcessedEventsSchemaInitializer(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    ProcessedEventRepository processedEventRepository(JdbcTemplate jdbcTemplate) {
        return new ProcessedEventRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    IdempotencyAspect idempotencyAspect(ProcessedEventRepository repository,
                                        TransactionTemplate transactionTemplate) {
        return new IdempotencyAspect(repository, transactionTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(RecordInterceptor.class)
    ConsumedRecordInterceptor consumedRecordInterceptor(@Value("${idempotent-consumer.event-id-header:id}") String headerName) {
        return new ConsumedRecordInterceptor(headerName);
    }

    @Bean
    @ConditionalOnMissingBean
    static IdempotentContractValidator idempotentContractValidator() {
        return new IdempotentContractValidator();
    }
}
