package dev.danielbodi.thesis.payment.common.event;

import dev.danielbodi.thesis.outbox.event.OutboxEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author danielbodi
 */
@Getter
@RequiredArgsConstructor
public class PaymentSucceededEvent implements OutboxEvent {

    public static final String AGGREGATE_TYPE = "Payment";
    public static final String EVENT_TYPE = "payment_succeeded";

    private final String aggregateId;
    private final String reference;

    @Override
    public String getAggregateType() {
        return AGGREGATE_TYPE;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
