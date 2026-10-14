package dev.danielbodi.thesis.measurement.docker;

import dev.danielbodi.thesis.measurement.scenario.PlannedOutage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author danielbodi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContainerKiller {

    private final DockerCompose dockerCompose;

    private FutureTask<Void> containerKillerTask;

    public void start(PlannedOutage plannedOutage, int ratePerSecond) {
        // try to kill the container approximately at the specified load request
        final Duration killAfter = Duration.ofSeconds(1)
                .dividedBy(ratePerSecond)
                .multipliedBy(plannedOutage.atLoadRequest() - 1L);

        // a FutureTask is a Runnable and a Future at once: a virtual thread runs it in the background,
        // and await() can wait for it with get(), which rethrows its exception (a plain Thread.join() would lose it)
        // no ExecutorService, since its close() would wait here until the killed container is started again
        containerKillerTask = new FutureTask<>(() -> {
            Thread.sleep(killAfter);
            log.info("Killing container [{}] approximately at load request [{}]",
                    plannedOutage.service(), plannedOutage.atLoadRequest());
            dockerCompose.kill(plannedOutage.service());

            Thread.sleep(plannedOutage.downFor());
            log.info("Starting container [{}] again after [{}]",
                    plannedOutage.service(), plannedOutage.downFor());
            dockerCompose.start(plannedOutage.service());

            // return null because Callable<V> requires a return value
            return null;
        });
        Thread.ofVirtual().start(containerKillerTask);
    }

    public void await() {
        try {
            containerKillerTask.get();
        } catch (ExecutionException e) {
            // a failed docker command fails the whole measurement run
            throw new IllegalStateException(e.getCause());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
