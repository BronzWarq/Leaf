package me.earthme.luminol.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import me.earthme.luminol.data.BufferedLinearRegionFile;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.*;

public class BufferedLinearRegionFileFlusher implements Runnable {
    private static final Logger logger = LogUtils.getLogger();

    private final Set<BufferedLinearRegionFile> inManagement = new ObjectArraySet<>();
    private final ScheduledFuture<?> flusherChecker;
    private final Executor ioWorkerPool;
    private final long flushOfWriteTimeoutMs;
    private volatile boolean shuttingDown;

    public BufferedLinearRegionFileFlusher(int nIoThreads, long scanInterval, long flushOfWriteTimeoutMs) {
        Validate.isTrue(nIoThreads > 0, "Number of I/O threads must > 0!");
        Validate.isTrue(scanInterval > 0, "Scan interval must > 0");
        Validate.isTrue(flushOfWriteTimeoutMs > 0, "Flush of write timeout must > 0");

        this.ioWorkerPool = Executors.newFixedThreadPool(nIoThreads, new ThreadFactoryBuilder()
            .setNameFormat("BufferedLinearRegionFile I/O Worker %d")
            .setDaemon(true)
            .build()
        );
        this.flusherChecker = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("BufferedLinearRegionFile Flusher Checker")
                .setDaemon(true)
                .build())
            .scheduleWithFixedDelay(this, scanInterval, scanInterval, TimeUnit.MILLISECONDS);
        this.flushOfWriteTimeoutMs = flushOfWriteTimeoutMs;
    }

    public void shutdown() {
        synchronized (this) {
            if (this.shuttingDown) {
                return;
            }
            this.shuttingDown = true;
            this.flusherChecker.cancel(false);
        }

        this.forceFlushAll(true);

        ((ExecutorService) this.ioWorkerPool).shutdown();
        for (; ; ) {
            try {
                if (((ExecutorService) this.ioWorkerPool).awaitTermination(60, TimeUnit.SECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void forceFlushAll() {
        this.forceFlushAll(false);
    }

    private void forceFlushAll(final boolean allowDuringShutdown) {
        final BufferedLinearRegionFile[] copied;

        synchronized (this) {
            if (this.shuttingDown && !allowDuringShutdown) {
                return;
            }
            copied = this.inManagement.toArray(new BufferedLinearRegionFile[0]);
        }

        for (BufferedLinearRegionFile file : copied) {
            // try acquiring the read lock
            if (!file.readLock(false)) {
                // should not happen
                continue;
            }

            boolean closed;

            try {
                // check if the file is closed
                closed = file.isClosedRaw();
            } finally {
                file.releaseReadLock();
            }

            if (closed) {
                // closed already, should be removed soon
                continue;
            }

            // skip non sync-required files
            if (!file.shouldSync()) {
                continue;
            }

            this.submitIoTask(() -> {
                try {
                    file.syncIfNeeded();
                } catch (IOException e) {
                    logger.error("Failed to sync master file: ", e);
                }
            }, allowDuringShutdown);
        }
    }

    @Override
    public void run() {
        if (this.shuttingDown) {
            return;
        }
        final long currentNanos = System.nanoTime();
        final BufferedLinearRegionFile[] copied;

        synchronized (this) {
            copied = this.inManagement.toArray(new BufferedLinearRegionFile[0]);
        }

        for (BufferedLinearRegionFile file : copied) {
            // try acquiring the read lock
            if (!file.readLock(true)) {
                // if the read lock is unacquirable, it might mean there is another operations is processing(might be a writing operation)
                continue;
            }

            boolean closed;

            try {
                // check if the file is closed
                closed = file.isClosedRaw();
            } finally {
                file.releaseReadLock();
            }

            if (closed) {
                // closed already, should be removed soon
                continue;
            }

            // skip non sync-required files
            if (!file.shouldSync()) {
                continue;
            }

            final long lastWriteNanos = file.getLastWritten();
            final long timeElapsed = (currentNanos - lastWriteNanos) / 1_000_000; // Convert to milliseconds

            // if deadline(timeout) reached
            if (timeElapsed >= this.flushOfWriteTimeoutMs) {
                // already marked to flush
                if (!file.markAsBeingSynced()) {
                    continue;
                }

                this.submitIoTask(() -> {
                    try {
                        file.syncIfNeeded();
                    } catch (IOException e) {
                        logger.error("Failed to sync master file: ", e);
                    }
                }, false);
            }
        }
    }

    private void submitIoTask(final Runnable task, final boolean allowDuringShutdown) {
        synchronized (this) {
            if (this.shuttingDown && !allowDuringShutdown) {
                return;
            }
            try {
                this.ioWorkerPool.execute(task);
            } catch (RejectedExecutionException exception) {
                logger.debug("Skipped buffered region file I/O task during executor shutdown", exception);
            }
        }
    }

    public void removeFile(BufferedLinearRegionFile fileToRemove) {
        synchronized (this) {
            this.inManagement.remove(fileToRemove);
        }
    }

    public void addFile(BufferedLinearRegionFile fileToAdd) {
        synchronized (this) {
            this.inManagement.add(fileToAdd);
        }
    }
}
