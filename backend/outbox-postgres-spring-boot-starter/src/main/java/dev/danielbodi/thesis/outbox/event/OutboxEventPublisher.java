package dev.danielbodi.thesis.outbox.event;

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
public class OutboxEventPublisher {

    private final OutboxFactory outboxFactory;
    private final OutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(OutboxEvent outboxEvent) {
        final Outbox outbox = outboxFactory.from(outboxEvent);
        save(outbox);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String traceId, String aggregateId, String aggregateType, String eventType, Object payload) {
        final Outbox outbox = outboxFactory.from(traceId, aggregateId, aggregateType, eventType, payload);
        save(outbox);
    }

    private void save(Outbox outbox) {
        outboxRepository.save(outbox);
        log.debug("Writing outbox event [id={}, type={}, aggregateType={}, aggregateId={}, traceId={}]",
                outbox.id(), outbox.type(), outbox.aggregateType(), outbox.aggregateId(), outbox.traceId());
    }
}
