package org.dreeam.leaf.util;

import org.dreeam.leaf.config.modules.opt.HighScaleMetrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Low-overhead counters for high-scale load testing.
 *
 * <p>All recording methods are no-ops unless the diagnostic module is enabled.
 * The counters are intentionally process-local and are reset on demand through
 * the Leaf command.</p>
 */
public final class HighScaleMetricsRegistry {
    private static final LongAdder pathSubmitted = new LongAdder();
    private static final LongAdder pathRejected = new LongAdder();
    private static final AtomicInteger pathQueuePeak = new AtomicInteger();
    private static final LongAdder mobSpawnSubmitted = new LongAdder();
    private static final LongAdder mobSpawnInline = new LongAdder();
    private static final AtomicInteger mobSpawnQueuePeak = new AtomicInteger();
    private static final LongAdder playerSavesSubmitted = new LongAdder();
    private static final AtomicInteger playerSaveQueuePeak = new AtomicInteger();
    private static final LongAdder trackerTicks = new LongAdder();
    private static final LongAdder trackerTasks = new LongAdder();
    private static final LongAdder trackerEntities = new LongAdder();
    private static final LongAdder trackerCallerRuns = new LongAdder();
    private static final LongAdder randomTickRuns = new LongAdder();
    private static final LongAdder randomTickSamples = new LongAdder();
    private static final AtomicInteger randomTickQueuePeak = new AtomicInteger();

    private HighScaleMetricsRegistry() {
    }

    public static void recordPathSubmitted(final int queueSize) {
        if (!HighScaleMetrics.enabled) {
            return;
        }
        pathSubmitted.increment();
        updatePeak(pathQueuePeak, queueSize);
    }

    public static void recordPathRejected() {
        if (HighScaleMetrics.enabled) {
            pathRejected.increment();
        }
    }

    public static void recordMobSpawnSubmitted(final int queueSize) {
        if (!HighScaleMetrics.enabled) {
            return;
        }
        mobSpawnSubmitted.increment();
        updatePeak(mobSpawnQueuePeak, queueSize);
    }

    public static void recordMobSpawnInline() {
        if (HighScaleMetrics.enabled) {
            mobSpawnInline.increment();
        }
    }

    public static void recordPlayerSaveSubmitted(final int queueSize) {
        if (!HighScaleMetrics.enabled) {
            return;
        }
        playerSavesSubmitted.increment();
        updatePeak(playerSaveQueuePeak, queueSize);
    }

    public static void recordTrackerTick(final int entities, final int tasks) {
        if (!HighScaleMetrics.enabled) {
            return;
        }
        trackerTicks.increment();
        trackerEntities.add(entities);
        trackerTasks.add(tasks);
    }

    public static void recordTrackerCallerRun() {
        if (HighScaleMetrics.enabled) {
            trackerCallerRuns.increment();
        }
    }

    public static void recordRandomTick(final int samples) {
        if (!HighScaleMetrics.enabled) {
            return;
        }
        randomTickRuns.increment();
        randomTickSamples.add(samples);
        updatePeak(randomTickQueuePeak, samples);
    }

    public static Snapshot snapshot() {
        return new Snapshot(
            pathSubmitted.sum(),
            pathRejected.sum(),
            pathQueuePeak.get(),
            mobSpawnSubmitted.sum(),
            mobSpawnInline.sum(),
            mobSpawnQueuePeak.get(),
            playerSavesSubmitted.sum(),
            playerSaveQueuePeak.get(),
            trackerTicks.sum(),
            trackerTasks.sum(),
            trackerEntities.sum(),
            trackerCallerRuns.sum(),
            randomTickRuns.sum(),
            randomTickSamples.sum(),
            randomTickQueuePeak.get()
        );
    }

    public static void reset() {
        pathSubmitted.reset();
        pathRejected.reset();
        pathQueuePeak.set(0);
        mobSpawnSubmitted.reset();
        mobSpawnInline.reset();
        mobSpawnQueuePeak.set(0);
        playerSavesSubmitted.reset();
        playerSaveQueuePeak.set(0);
        trackerTicks.reset();
        trackerTasks.reset();
        trackerEntities.reset();
        trackerCallerRuns.reset();
        randomTickRuns.reset();
        randomTickSamples.reset();
        randomTickQueuePeak.set(0);
    }

    private static void updatePeak(final AtomicInteger peak, final int value) {
        peak.accumulateAndGet(value, Math::max);
    }

    public record Snapshot(
        long pathSubmitted,
        long pathRejected,
        int pathQueuePeak,
        long mobSpawnSubmitted,
        long mobSpawnInline,
        int mobSpawnQueuePeak,
        long playerSavesSubmitted,
        int playerSaveQueuePeak,
        long trackerTicks,
        long trackerTasks,
        long trackerEntities,
        long trackerCallerRuns,
        long randomTickRuns,
        long randomTickSamples,
        int randomTickQueuePeak
    ) {
    }
}
