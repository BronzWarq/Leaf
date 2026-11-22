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
    private static final LongAdder mobSpawnDeferred = new LongAdder();
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
    private static final LongAdder tntDeferredEntityTicks = new LongAdder();
    private static final LongAdder tntBudgetLimitedWorldTicks = new LongAdder();
    private static final AtomicInteger tntDeferredPerWorldTickPeak = new AtomicInteger();
    private static final LongAdder tntFuseExpired = new LongAdder();
    private static final LongAdder tntRuleBlocked = new LongAdder();
    private static final LongAdder tntExplosionPrimeCancelled = new LongAdder();
    private static final LongAdder tntPreExplosionCancelled = new LongAdder();
    private static final LongAdder tntBlockExplosionCancelled = new LongAdder();
    private static final LongAdder tntBlockPrimeCancelled = new LongAdder();
    private static final LongAdder tntDirectSpawnRejected = new LongAdder();
    private static final LongAdder tntExplosionSpawnRejected = new LongAdder();

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

    public static void recordMobSpawnDeferred() {
        if (HighScaleMetrics.enabled) {
            mobSpawnDeferred.increment();
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

    public static void recordTntTickBudget(final int attempted, final int limit) {
        if (!HighScaleMetrics.enabled || limit <= 0 || attempted <= limit) {
            return;
        }
        final int deferred = attempted - limit;
        tntDeferredEntityTicks.add(deferred);
        tntBudgetLimitedWorldTicks.increment();
        updatePeak(tntDeferredPerWorldTickPeak, deferred);
    }

    public static void recordTntFuseExpired() {
        if (HighScaleMetrics.enabled) tntFuseExpired.increment();
    }

    public static void recordTntRuleBlocked() {
        if (HighScaleMetrics.enabled) tntRuleBlocked.increment();
    }

    public static void recordTntExplosionPrimeCancelled() {
        if (HighScaleMetrics.enabled) tntExplosionPrimeCancelled.increment();
    }

    public static void recordTntPreExplosionCancelled() {
        if (HighScaleMetrics.enabled) tntPreExplosionCancelled.increment();
    }

    public static void recordTntBlockExplosionCancelled() {
        if (HighScaleMetrics.enabled) tntBlockExplosionCancelled.increment();
    }

    public static void recordTntBlockPrimeCancelled() {
        if (HighScaleMetrics.enabled) tntBlockPrimeCancelled.increment();
    }

    public static void recordTntDirectSpawnRejected() {
        if (HighScaleMetrics.enabled) tntDirectSpawnRejected.increment();
    }

    public static void recordTntExplosionSpawnRejected() {
        if (HighScaleMetrics.enabled) tntExplosionSpawnRejected.increment();
    }

    public static Snapshot snapshot() {
        return new Snapshot(
            pathSubmitted.sum(),
            pathRejected.sum(),
            pathQueuePeak.get(),
            mobSpawnSubmitted.sum(),
            mobSpawnInline.sum(),
            mobSpawnDeferred.sum(),
            mobSpawnQueuePeak.get(),
            playerSavesSubmitted.sum(),
            playerSaveQueuePeak.get(),
            trackerTicks.sum(),
            trackerTasks.sum(),
            trackerEntities.sum(),
            trackerCallerRuns.sum(),
            randomTickRuns.sum(),
            randomTickSamples.sum(),
            randomTickQueuePeak.get(),
            tntDeferredEntityTicks.sum(),
            tntBudgetLimitedWorldTicks.sum(),
            tntDeferredPerWorldTickPeak.get(),
            tntFuseExpired.sum(),
            tntRuleBlocked.sum(),
            tntExplosionPrimeCancelled.sum(),
            tntPreExplosionCancelled.sum(),
            tntBlockExplosionCancelled.sum(),
            tntBlockPrimeCancelled.sum(),
            tntDirectSpawnRejected.sum(),
            tntExplosionSpawnRejected.sum()
        );
    }

    public static void reset() {
        pathSubmitted.reset();
        pathRejected.reset();
        pathQueuePeak.set(0);
        mobSpawnSubmitted.reset();
        mobSpawnInline.reset();
        mobSpawnDeferred.reset();
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
        tntDeferredEntityTicks.reset();
        tntBudgetLimitedWorldTicks.reset();
        tntDeferredPerWorldTickPeak.set(0);
        tntFuseExpired.reset();
        tntRuleBlocked.reset();
        tntExplosionPrimeCancelled.reset();
        tntPreExplosionCancelled.reset();
        tntBlockExplosionCancelled.reset();
        tntBlockPrimeCancelled.reset();
        tntDirectSpawnRejected.reset();
        tntExplosionSpawnRejected.reset();
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
        long mobSpawnDeferred,
        int mobSpawnQueuePeak,
        long playerSavesSubmitted,
        int playerSaveQueuePeak,
        long trackerTicks,
        long trackerTasks,
        long trackerEntities,
        long trackerCallerRuns,
        long randomTickRuns,
        long randomTickSamples,
        int randomTickQueuePeak,
        long tntDeferredEntityTicks,
        long tntBudgetLimitedWorldTicks,
        int tntDeferredPerWorldTickPeak,
        long tntFuseExpired,
        long tntRuleBlocked,
        long tntExplosionPrimeCancelled,
        long tntPreExplosionCancelled,
        long tntBlockExplosionCancelled,
        long tntBlockPrimeCancelled,
        long tntDirectSpawnRejected,
        long tntExplosionSpawnRejected
    ) {
    }
}
