package dev.danielbodi.thesis.payment.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.ByteArrayJacksonJsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

/**
 * @author danielbodi
 */
@Configuration
public class KafkaConfiguration {

    @Bean
    RecordMessageConverter kafkaRecordMessageConverter() {
        return new ByteArrayJacksonJsonMessageConverter();
    }
}
