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

        outboxRepository.save(outbox);
        log.debug("Writing outbox event [id={}, type={}, aggregateType={}, aggregateId={}, traceContext={}]",
                outbox.id(), outbox.type(), outbox.aggregateType(), outbox.aggregateId(), outbox.traceId());
    }
}
