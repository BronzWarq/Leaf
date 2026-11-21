package gg.pufferfish.pufferfish.util;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dreeam.leaf.config.modules.async.AsyncMobSpawning;
import org.dreeam.leaf.util.HighScaleMetricsRegistry;

import java.util.concurrent.locks.LockSupport;

public class AsyncExecutor implements Runnable {

    private final Logger LOGGER = LogManager.getLogger("Leaf");
    private final PriorityQueue<Runnable> jobs = new ObjectArrayFIFOQueue<>();
    public final Thread thread;
    private volatile boolean killswitch = false;

    public AsyncExecutor(String threadName) {
        this.thread = Thread.ofPlatform()
            .name(threadName)
            .priority(Thread.NORM_PRIORITY - 1)
            .daemon(false)
            .uncaughtExceptionHandler(Util::onThreadException)
            .unstarted(this);
    }

    public void start() {
        thread.start();
    }

    public void join(long millis) throws InterruptedException {
        synchronized (jobs) {
            killswitch = true;
            jobs.clear();
        }
        LockSupport.unpark(thread);
        thread.join(millis);
    }

    public void submit(Runnable runnable) {
        submit(runnable, false);
    }

    public boolean trySubmit(Runnable runnable) {
        return submit(runnable, AsyncMobSpawning.deferWhenFull);
    }

    private boolean submit(Runnable runnable, boolean deferWhenFull) {
        boolean runInline;
        int queueSize = 0;
        synchronized (jobs) {
            if (killswitch) {
                return false;
            }
            runInline = jobs.size() >= AsyncMobSpawning.maxQueuedTasks;
            if (runInline && deferWhenFull) {
                return false;
            }
            if (!runInline) {
                jobs.enqueue(runnable);
                queueSize = jobs.size();
            }
        }
        if (runInline) {
            HighScaleMetricsRegistry.recordMobSpawnInline();
            runJob(runnable);
            return true;
        }
        HighScaleMetricsRegistry.recordMobSpawnSubmitted(queueSize);
        LockSupport.unpark(thread);
        return true;
    }

    @Override
    public void run() {
        while (!killswitch) {
            try {
                Runnable runnable = null;

                synchronized (jobs) {
                    if (killswitch) {
                        return;
                    }
                    if (!jobs.isEmpty()) {
                        runnable = jobs.dequeue();
                    }
                }

                if (runnable == null) {
                    LockSupport.park();
                    continue;
                }

                runJob(runnable);
            } catch (Exception e) {
                LOGGER.error("Failed to execute async job for thread {}", thread.getName(), e);
            }
        }
    }

    private void runJob(final Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            LOGGER.error("Failed to execute async job for thread {}", thread.getName(), e);
        }
    }
}
