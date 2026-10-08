package dev.danielbodi.thesis.fault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author danielbodi
 */
class FaultInjectorTest {

    @TempDir
    private Path tempDir;

    // records the exit codes instead of halting the test JVM
    private final List<Integer> halts = new ArrayList<>();

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void inactiveCrashPointNeverHalts() {
        final FaultInjector faultInjector = createFaultInjector(null, 1);

        faultInjector.reach("CP1");

        assertThat(halts).isEmpty();
    }

    @Test
    void activeCrashPointHaltsAtTheTargetReach() {
        final FaultInjector faultInjector = createFaultInjector("CP2", 3);

        faultInjector.reach("CP2");
        faultInjector.reach("CP2");
        final List<Integer> haltsBeforeTarget = List.copyOf(halts);
        faultInjector.reach("CP2");

        assertSoftly(softly -> {
            softly.assertThat(haltsBeforeTarget).isEmpty();
            softly.assertThat(halts).containsExactly(FaultInjector.EXIT_CODE);
            softly.assertThat(Files.exists(marker())).isTrue();
        });
    }

    @Test
    void activeCrashPointHaltsOnlyOnceAfterTheTargetIsReached() {
        final FaultInjector faultInjector = createFaultInjector("CP2", 5);

        for (int i = 0; i < 15; i++) {
            faultInjector.reach("CP2");
        }

        assertSoftly(softly -> {
            softly.assertThat(halts).containsExactly(FaultInjector.EXIT_CODE);
            softly.assertThat(Files.exists(marker())).isTrue();
        });
    }

    @Test
    void inactiveCrashPointsAreNotCounted() {
        final FaultInjector faultInjector = createFaultInjector("CP2", 1);

        faultInjector.reach("CP1");
        faultInjector.reach("CP3");

        assertThat(halts).isEmpty();
    }

    @Test
    void restartedProcessDoesNotHaltAgain() throws Exception {
        Files.createFile(marker());
        final FaultInjector faultInjector = createFaultInjector("CP2", 1);

        faultInjector.reach("CP2");

        assertThat(halts).isEmpty();
    }

    @Test
    void afterCommitCrashPointHaltsOnlyOnceTheTransactionHasCommitted() {
        final FaultInjector faultInjector = createFaultInjector("CP1", 1);

        TransactionSynchronizationManager.initSynchronization();

        faultInjector.reachAfterCommit("CP1");
        final List<Integer> haltsBeforeCommit = List.copyOf(halts);

        TransactionSynchronizationUtils.triggerAfterCommit();

        assertSoftly(softly -> {
            softly.assertThat(haltsBeforeCommit).isEmpty();
            softly.assertThat(halts).containsExactly(FaultInjector.EXIT_CODE);
        });
    }

    private FaultInjector createFaultInjector(String activeCrashPointName, int targetReachCount) {
        return new FaultInjector(activeCrashPointName, targetReachCount, marker(), halts::add);
    }

    private Path marker() {
        return tempDir.resolve("crash-point-fired");
    }
}
