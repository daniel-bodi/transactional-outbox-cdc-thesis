package dev.danielbodi.thesis.payment.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * @author danielbodi
 */
@Configuration
public class PspClientConfiguration {

    @Bean
    RestClient pspRestClient(RestClient.Builder builder, @Value("${psp.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
