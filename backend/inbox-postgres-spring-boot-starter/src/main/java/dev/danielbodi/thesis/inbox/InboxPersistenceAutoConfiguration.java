package dev.danielbodi.thesis.inbox;

import dev.danielbodi.thesis.inbox.aspect.IdempotencyAspect;
import dev.danielbodi.thesis.inbox.kafka.EventIdRecordInterceptor;
import dev.danielbodi.thesis.inbox.persistence.InboxSchemaInitializer;
import dev.danielbodi.thesis.inbox.persistence.ProcessedEventRepository;
import dev.danielbodi.thesis.inbox.validation.IdempotentContractValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
@ConditionalOnClass(TransactionTemplate.class)
public class InboxPersistenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "inbox.schema-initialization.enabled", matchIfMissing = true)
    InboxSchemaInitializer inboxSchemaInitializer(DataSource dataSource) {
        return new InboxSchemaInitializer(dataSource);
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
    EventIdRecordInterceptor eventIdRecordInterceptor(@Value("${inbox.event-id-header:id}") String headerName) {
        return new EventIdRecordInterceptor(headerName);
    }

    @Bean
    @ConditionalOnMissingBean
    static IdempotentContractValidator idempotentContractValidator() {
        return new IdempotentContractValidator();
    }
}
