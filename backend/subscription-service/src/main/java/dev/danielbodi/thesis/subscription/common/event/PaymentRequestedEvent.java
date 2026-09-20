package dev.danielbodi.thesis.subscription.common.event;

import dev.danielbodi.thesis.outbox.event.OutboxEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Raised when a subscription has been created and payment for it must be collected.
 * <p>
 * The payload names the target of the charge as {@code reference} rather than a
 * subscription identifier, so that the payment side does not learn about the
 * concept of a subscription — that belongs to another bounded context.
 *
 * @author danielbodi
 */
@Getter
@RequiredArgsConstructor
public class PaymentRequestedEvent implements OutboxEvent {

    public static final String AGGREGATE_TYPE = "Subscription";
    public static final String EVENT_TYPE = "PaymentRequested";

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
