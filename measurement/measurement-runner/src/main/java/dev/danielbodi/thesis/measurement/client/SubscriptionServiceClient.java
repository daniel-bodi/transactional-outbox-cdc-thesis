package dev.danielbodi.thesis.measurement.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * @author danielbodi
 */
@Component
@RequiredArgsConstructor
public class SubscriptionServiceClient {

    // the seeded "basic" plan of the subscription-service
    private static final UUID PLAN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final RestClient subscriptionServiceRestClient;

    public HttpStatusCode createSubscription(UUID customerId) {
        final CreateSubscriptionRequest request = new CreateSubscriptionRequest(customerId, PLAN_ID);

        return subscriptionServiceRestClient.post()
                .uri("/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity()
                .getStatusCode();
    }
}
