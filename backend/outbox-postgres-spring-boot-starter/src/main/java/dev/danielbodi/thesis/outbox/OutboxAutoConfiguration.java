package dev.danielbodi.thesis.outbox;

import dev.danielbodi.thesis.outbox.event.OutboxEventPublisher;
import dev.danielbodi.thesis.outbox.persistence.OutboxFactory;
import dev.danielbodi.thesis.outbox.persistence.OutboxRepository;
import dev.danielbodi.thesis.outbox.persistence.OutboxSchemaInitializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;

/**
 * @author danielbodi
 */
@AutoConfiguration(after = {DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class})
@ConditionalOnSingleCandidate(DataSource.class)
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "outbox.schema-initialization.enabled", matchIfMissing = true)
    OutboxSchemaInitializer outboxSchemaInitializer(DataSource dataSource) {
        return new OutboxSchemaInitializer(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxRepository outboxRepository(JdbcTemplate jdbcTemplate) {
        return new OutboxRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxFactory outboxEntityFactory(ObjectMapper objectMapper) {
        return new OutboxFactory(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxEventPublisher outboxEventPublisher(OutboxFactory outboxFactory, OutboxRepository outboxRepository) {
        return new OutboxEventPublisher(outboxFactory, outboxRepository);
    }
}
