package org.dreeam.leaf.config.modules.async;

import org.dreeam.leaf.config.ConfigModule;
import org.dreeam.leaf.config.ConfigCategory;

public class AsyncMobSpawning extends ConfigModule {

    public String basePath() {
        return ConfigCategory.ASYNC.basePath() + ".async-mob-spawning";
    }

    public static boolean enabled = true;
    public static int maxQueuedTasks = 256;
    private static boolean asyncMobSpawningInitialized;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath(), """
                Whether or not asynchronous mob spawning should be enabled.
                On servers with many entities, this can improve performance by up to 15%. You must have
                paper's per-player-mob-spawns setting set to true for this to work.
                One quick note - this does not actually spawn mobs async (that would be very unsafe).
                This just offloads some expensive calculations that are required for mob spawning.
                max-queued-tasks bounds stale async spawn-count work. When full, the submitting
                server thread executes the new calculation inline to preserve result publication.""",
            """
                是否异步化生物生成.
                在实体较多的服务器上, 异步生成可最高带来 15% 的性能提升.
                须在Paper配置文件中打开 per-player-mob-spawns 才能生效.
                max-queued-tasks 限制过期的异步生成计数任务. 队列满时由提交任务的服务器线程执行新计算，以保证结果仍会发布.""");

        // This prevents us from changing the value during a reload.
        if (asyncMobSpawningInitialized) {
            globalConfig.getConfigSection(basePath());
            return;
        }
        asyncMobSpawningInitialized = true;

        enabled = globalConfig.getBoolean(basePath() + ".enabled", enabled);
        maxQueuedTasks = Math.max(1, Math.min(4096,
            globalConfig.getInt(basePath() + ".max-queued-tasks", maxQueuedTasks)));
        enabled |= AsyncHighScaleProfile.enabled;
    }
}
