package dev.danielbodi.thesis.fault;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.StringUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * @author danielbodi
 */
@Slf4j
public class FaultInjector {

    // the exit code of a process killed by SIGKILL, so the container looks killed from the outside too
    static final int EXIT_CODE = 137;

    private final String activeCrashPointName;
    private final int targetReachCount;
    private final Path markerFile;
    private final boolean alreadyCrashed;
    private final IntConsumer haltFunction;
    private final AtomicInteger timesReached = new AtomicInteger();

    public FaultInjector(String activeCrashPointName, int targetReachCount, Path markerFile, IntConsumer haltFunction) {
        this.activeCrashPointName = activeCrashPointName;
        this.targetReachCount = targetReachCount;
        this.markerFile = markerFile;
        // the marker is only written right before a halt, so it can only exist after a restart
        this.alreadyCrashed = Files.exists(markerFile);
        this.haltFunction = haltFunction;

        if (StringUtils.hasText(activeCrashPointName)) {
            checkMarkerFileDirectoryWritable();
            logActivation();
        }
    }

    public void reach(String point) {
        if (isActive(point) && timesReached.incrementAndGet() == targetReachCount) {
            crash(point);
        }
    }

    public void reachAfterCommit(String point) {
        if (!isActive(point)) {
            return;
        }
        // spring-tx callback after the current transaction has successfully committed
        // after the commit but before the JVM continues (e.g. before the HTTP response or the Kafka offset commit)
        final TransactionSynchronization synchronization = new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                reach(point);
            }

            // runs before every other after-commit callback, e.g. before the dual-write send
            @Override
            public int getOrder() {
                return Ordered.HIGHEST_PRECEDENCE;
            }
        };
        TransactionSynchronizationManager.registerSynchronization(synchronization);
    }

    private boolean isActive(String point) {
        return !alreadyCrashed && point.equals(activeCrashPointName);
    }

    private void crash(String point) {
        try {
            Files.createFile(markerFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write crash point marker file: [%s]".formatted(markerFile), e);
        }

        log.warn("Crash point [{}] reached [{}] times, halting the JVM immediately", point, targetReachCount);
        haltFunction.accept(EXIT_CODE);
    }

    private void checkMarkerFileDirectoryWritable() {
        final Path markerDirectory = markerFile.toAbsolutePath().getParent();
        if (!Files.isWritable(markerDirectory)) {
            throw new IllegalStateException(
                    "Crash point marker's directory is not writable: [%s]".formatted(markerDirectory));
        }
    }

    private void logActivation() {
        if (alreadyCrashed) {
            log.info("Crash point [{}] already fired before a restart, fault injection disabled",
                    activeCrashPointName);
            return;
        }
        log.info("Crash point [{}] active, application crashes when point reached [{}] times",
                activeCrashPointName, targetReachCount);
    }
}
