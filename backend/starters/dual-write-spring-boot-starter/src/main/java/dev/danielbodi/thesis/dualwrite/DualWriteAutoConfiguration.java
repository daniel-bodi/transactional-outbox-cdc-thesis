package dev.danielbodi.thesis.dualwrite;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * @author danielbodi
 */
// after the Kafka auto-configuration, which defines the KafkaTemplate, and before the outbox auto-configuration,
// whose publisher backs off when another one is already defined
@AutoConfiguration(
        afterName = "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration",
        beforeName = "dev.danielbodi.thesis.outbox.OutboxPersistenceAutoConfiguration")
@ConditionalOnProperty(name = "dual-write.enabled", havingValue = "true")
public class DualWriteAutoConfiguration {

    @Bean
    DualWriteEventPublisher dualWriteEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                                    ObjectMapper objectMapper) {
        return new DualWriteEventPublisher(kafkaTemplate, objectMapper);
    }
}
