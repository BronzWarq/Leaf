package org.dreeam.leaf.async;

import net.minecraft.util.Util;
import org.dreeam.leaf.config.modules.async.AsyncPlayerDataSave;
import org.dreeam.leaf.util.HighScaleMetricsRegistry;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncPlayerDataSaving {

    private static final int MAX_PENDING_SAVES = 4096;
    public static ExecutorService IO_POOL = null;

    private AsyncPlayerDataSaving() {
    }

    public static void init() {
        if (IO_POOL == null) {
            IO_POOL = new ThreadPoolExecutor(
                1,
                1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_PENDING_SAVES),
                new com.google.common.util.concurrent.ThreadFactoryBuilder()
                    .setPriority(Thread.NORM_PRIORITY - 2)
                    .setNameFormat("Leaf IO Thread")
                    .setUncaughtExceptionHandler(Util::onThreadException)
                    .build(),
                // A late save during shutdown must run on the caller instead of being discarded.
                (runnable, executor) -> runnable.run()
            );
        } else {
            // Temp no-op
            //throw new IllegalStateException();
        }
    }

    public static Optional<Future<?>> submit(Runnable runnable) {
        final ExecutorService pool = IO_POOL;
        if (!AsyncPlayerDataSave.enabled || pool == null) {
            runnable.run();
            return Optional.empty();
        } else {
            final Future<?> future = pool.submit(runnable);
            if (pool instanceof ThreadPoolExecutor executor) {
                HighScaleMetricsRegistry.recordPlayerSaveSubmitted(executor.getQueue().size());
            }
            return Optional.of(future);
        }
    }
}
