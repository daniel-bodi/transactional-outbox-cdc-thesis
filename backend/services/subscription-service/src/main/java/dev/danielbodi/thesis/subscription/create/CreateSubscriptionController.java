package dev.danielbodi.thesis.subscription.create;

import dev.danielbodi.thesis.subscription.create.service.CreateSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @author danielbodi
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/subscriptions")
public class CreateSubscriptionController {

    private final CreateSubscriptionService createSubscriptionService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSubscriptionResponse createSubscription(@RequestBody CreateSubscriptionRequest request) {
        final UUID customerId = request.customerId();
        final UUID planId = request.planId();

        log.info("Subscription creation requested for customer: [{}] with plan: [{}]", customerId, planId);

        final UUID subscriptionId = createSubscriptionService.createSubscription(customerId, planId);

        log.info("Subscription created with id: [{}] for customer: [{}] with plan: [{}]", subscriptionId, customerId, planId);

        return new CreateSubscriptionResponse(subscriptionId);
    }
}
