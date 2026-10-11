package dev.danielbodi.thesis.measurement.client;

import dev.danielbodi.thesis.measurement.properties.MeasurementProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * @author danielbodi
 */
@Configuration
public class SubscriptionServiceClientConfiguration {

    @Bean
    RestClient subscriptionServiceRestClient(RestClient.Builder builder, MeasurementProperties properties) {
        return builder.baseUrl(properties.subscriptionServiceUrl()).build();
    }
}
