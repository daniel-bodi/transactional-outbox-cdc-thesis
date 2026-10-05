package dev.danielbodi.thesis.subscription.common.event;

import dev.danielbodi.thesis.outbox.event.OutboxEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * @author danielbodi
 */
@Getter
@RequiredArgsConstructor
public class PaymentRequestedEvent implements OutboxEvent {

    public static final String AGGREGATE_TYPE = "Subscription";
    public static final String EVENT_TYPE = "payment_requested";

    private final String aggregateId;
    private final String reference;
    private final BigDecimal amount;

    @Override
    public String getAggregateType() {
        return AGGREGATE_TYPE;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
