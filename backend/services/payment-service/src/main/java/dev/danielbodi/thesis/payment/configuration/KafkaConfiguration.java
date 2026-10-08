package dev.danielbodi.thesis.payment.configuration;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.converter.ByteArrayJacksonJsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

import java.util.Map;

/**
 * @author danielbodi
 */
@Configuration
@EnableConfigurationProperties(ConsumerRetryProperties.class)
public class KafkaConfiguration {

    @Bean
    RecordMessageConverter kafkaRecordMessageConverter() {
        // the consumer's value deserializer (ByteArrayDeserializer) uses byte arrays,
        // so this bean deserializes the raw bytes into a JSON object and maps it to the correct Java type
        return new ByteArrayJacksonJsonMessageConverter();
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(ProducerFactory<Object, Object> producerFactory, ConsumerRetryProperties retry) {
        // the error handler uses the ConsumerRecord (not the value converted by ByteArrayJacksonJsonMessageConverter),
        // so its value is still the raw bytes
        final KafkaTemplate<Object, Object> deadLetterTemplate = new KafkaTemplate<>(producerFactory,
                Map.of(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class));

        final ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(retry.maxRetries());
        backOff.setInitialInterval(retry.initialInterval().toMillis());
        backOff.setMultiplier(retry.multiplier());
        backOff.setMaxInterval(retry.maxInterval().toMillis());

        final DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(new DeadLetterPublishingRecoverer(deadLetterTemplate), backOff);

        // failures that a retry cannot fix go to the dead letter topic immediately
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                IllegalStateException.class,
                DataIntegrityViolationException.class);

        return errorHandler;
    }
}
