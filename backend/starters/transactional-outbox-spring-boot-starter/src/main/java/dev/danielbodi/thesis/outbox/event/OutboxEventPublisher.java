package dev.danielbodi.thesis.outbox.event;

import dev.danielbodi.thesis.event.IntegrationEvent;
import dev.danielbodi.thesis.event.IntegrationEventPublisher;
import dev.danielbodi.thesis.outbox.persistence.Outbox;
import dev.danielbodi.thesis.outbox.persistence.OutboxFactory;
import dev.danielbodi.thesis.outbox.persistence.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author danielbodi
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxEventPublisher implements IntegrationEventPublisher {

    private final OutboxFactory outboxFactory;
    private final OutboxRepository outboxRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(IntegrationEvent event) {
        final Outbox outbox = outboxFactory.from(event);

        outboxRepository.save(outbox);
        log.debug("Writing outbox event [id={}, type={}, aggregateType={}, aggregateId={}, traceContext={}]",
                outbox.id(), outbox.type(), outbox.aggregateType(), outbox.aggregateId(), outbox.traceContext());
    }
}
