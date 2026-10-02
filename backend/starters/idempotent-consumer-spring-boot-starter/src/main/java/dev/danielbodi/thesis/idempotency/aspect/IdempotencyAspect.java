package dev.danielbodi.thesis.idempotency.aspect;

import dev.danielbodi.thesis.idempotency.kafka.ConsumedRecordContext;
import dev.danielbodi.thesis.idempotency.persistence.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * @author danielbodi
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final ProcessedEventRepository repository;
    private final TransactionTemplate transactionTemplate;

    @Around("@annotation(dev.danielbodi.thesis.idempotency.annotation.Idempotent)")
    public Object handle(ProceedingJoinPoint pjp) throws Throwable {
        final String consumerGroup = ConsumedRecordContext.currentConsumerGroup();
        final UUID eventId = ConsumedRecordContext.currentEventId();
        try {
            return transactionTemplate.execute(status -> {
                if (!repository.insertIfAbsent(consumerGroup, eventId)) {
                    log.debug("Duplicate event [{}] for consumer group [{}], skipping", eventId, consumerGroup);
                    return null;
                }
                try {
                    return pjp.proceed();
                } catch (Throwable t) {
                    status.setRollbackOnly();
                    throw new AspectThrowableCarrier(t);
                }
            });
        } catch (AspectThrowableCarrier carrier) {
            throw carrier.getCause();
        }
    }

    private static final class AspectThrowableCarrier extends RuntimeException {
        AspectThrowableCarrier(Throwable cause) {
            super(cause);
        }
    }
}
