package dev.danielbodi.thesis.measurement.evaluation;

import dev.danielbodi.thesis.measurement.database.StatusCounts;
import dev.danielbodi.thesis.measurement.database.SubscriptionDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * @author danielbodi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConvergenceService {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration LOG_INTERVAL = Duration.ofSeconds(2);

    private final SubscriptionDao subscriptionDao;

    // true: every subscription left PENDING state
    // false: the timeout is over first
    public boolean awaitNoPending(Duration timeout) {
        final Instant start = Instant.now();
        final Instant deadline = start.plus(timeout);

        Instant nextLog = start;

        while (Instant.now().isBefore(deadline)) {
            final Optional<StatusCounts> optionalCounts = subscriptionDao.tryStatusCounts();
            if (optionalCounts.isPresent() && optionalCounts.get().pending() == 0) {
                return true;
            }

            // a status line every few seconds, so a long wait does not look like a hanging run
            if (Instant.now().isAfter(nextLog)) {
                log.info("[+{}s] {}",
                        Duration.between(start, Instant.now()).toSeconds(),
                        optionalCounts.isEmpty() ? "subscription database unavailable" : optionalCounts.get());
                nextLog = nextLog.plus(LOG_INTERVAL);
            }

            try {
                Thread.sleep(POLL_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        return false;
    }
}
