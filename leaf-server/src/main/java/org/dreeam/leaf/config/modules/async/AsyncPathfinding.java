package org.dreeam.leaf.config.modules.async;

import org.dreeam.leaf.async.path.PathfindTaskRejectPolicy;
import org.dreeam.leaf.config.ConfigModule;
import org.dreeam.leaf.config.ConfigCategory;
import org.dreeam.leaf.config.LeafConfig;

public class AsyncPathfinding extends ConfigModule {

    private static final int MAX_QUEUE_SIZE = 16_384;

    public String basePath() {
        return ConfigCategory.ASYNC.basePath() + ".async-pathfinding";
    }

    public static boolean enabled = false;
    public static int asyncPathfindingMaxThreads = 0;
    public static int asyncPathfindingKeepalive = 60;
    public static int asyncPathfindingQueueSize = 0;
    public static PathfindTaskRejectPolicy asyncPathfindingRejectPolicy = PathfindTaskRejectPolicy.FLUSH_ALL;
    private static boolean asyncPathfindingInitialized;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath() + ".reject-policy", """
                The policy to use when the queue is full and a new task is submitted.
                FLUSH_ALL: All pending tasks will be run on server thread.
                CALLER_RUNS: Newly submitted task will be run on server thread.
                High-scale compatibility mode forces CALLER_RUNS to avoid draining the whole backlog in one tick.""",
            """
                当队列满时, 新提交的任务将使用以下策略处理.
                FLUSH_ALL: 所有等待中的任务都将在主线程上运行.
                CALLER_RUNS: 新提交的任务将在主线程上运行.
                高负载兼容模式会强制使用 CALLER_RUNS，避免单个 tick 执行整个等待队列."""
        );
        if (asyncPathfindingInitialized) {
            globalConfig.getConfigSection(basePath());
            return;
        }
        asyncPathfindingInitialized = true;

        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        enabled = globalConfig.getBoolean(basePath() + ".enabled", enabled);
        enabled |= AsyncHighScaleProfile.enabled;
        asyncPathfindingMaxThreads = globalConfig.getInt(basePath() + ".max-threads", asyncPathfindingMaxThreads);
        asyncPathfindingKeepalive = globalConfig.getInt(basePath() + ".keepalive", asyncPathfindingKeepalive);
        asyncPathfindingQueueSize = globalConfig.getInt(basePath() + ".queue-size", asyncPathfindingQueueSize);

        if (asyncPathfindingMaxThreads <= 0) {
            asyncPathfindingMaxThreads = Math.max(availableProcessors / 4, 1);
        }
        final int maxWorkerThreads = Math.max(availableProcessors, 1);
        if (asyncPathfindingMaxThreads > maxWorkerThreads) {
            LeafConfig.LOGGER.warn("Async pathfinding requested {} threads, clamping to {} CPU workers.",
                asyncPathfindingMaxThreads, maxWorkerThreads);
            asyncPathfindingMaxThreads = maxWorkerThreads;
        }
        asyncPathfindingKeepalive = Math.max(asyncPathfindingKeepalive, 1);

        if (!enabled) {
            asyncPathfindingMaxThreads = 0;
        }

        if (asyncPathfindingQueueSize <= 0) {
            asyncPathfindingQueueSize = asyncPathfindingMaxThreads * 256;
        }
        if (asyncPathfindingQueueSize > MAX_QUEUE_SIZE) {
            LeafConfig.LOGGER.warn("Async pathfinding queue requested {} tasks, clamping to {} to bound retained path state.",
                asyncPathfindingQueueSize, MAX_QUEUE_SIZE);
            asyncPathfindingQueueSize = MAX_QUEUE_SIZE;
        }

        asyncPathfindingRejectPolicy = PathfindTaskRejectPolicy.fromString(globalConfig.getString(basePath() + ".reject-policy",
            availableProcessors >= 12 && asyncPathfindingQueueSize < 512 && !AsyncHighScaleProfile.enabled
                ? PathfindTaskRejectPolicy.FLUSH_ALL.toString()
                : PathfindTaskRejectPolicy.CALLER_RUNS.toString())
        );
        if (AsyncHighScaleProfile.enabled && AsyncHighScaleProfile.compatibilityMode
            && asyncPathfindingRejectPolicy != PathfindTaskRejectPolicy.CALLER_RUNS) {
            LeafConfig.LOGGER.info("High-scale compatibility mode is forcing CALLER_RUNS for async pathfinding backpressure.");
            asyncPathfindingRejectPolicy = PathfindTaskRejectPolicy.CALLER_RUNS;
        }

        if (enabled) {
            LeafConfig.LOGGER.info("Using {} threads for Async Pathfinding", asyncPathfindingMaxThreads);
            org.dreeam.leaf.async.path.AsyncPathProcessor.init();
        }
    }
}
