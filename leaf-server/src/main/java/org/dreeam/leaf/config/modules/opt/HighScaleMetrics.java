package org.dreeam.leaf.config.modules.opt;

import org.dreeam.leaf.config.ConfigCategory;
import org.dreeam.leaf.config.ConfigModule;
import org.dreeam.leaf.util.HighScaleMetricsRegistry;

/** Diagnostics for high-scale load tests. */
public final class HighScaleMetrics extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".high-scale-metrics";
    }

    public static boolean enabled = true;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath(), """
                Enables lightweight counters for high-scale load testing.
                Use /leaf metrics to inspect pathfinding, mob-spawn, player-save,
                tracker, random-tick queue pressure, and TNT ticks deferred by the
                Spigot limit. Deferred entity-ticks are not unique TNT counts.
                Enabled by default; set enabled to false to disable the counters.""", """
                启用用于高负载测试的轻量计数器.
                使用 /leaf metrics 查看寻路、生物生成、玩家保存、实体跟踪、random tick 队列压力及 TNT 延迟 tick 数.
                默认启用；将 enabled 设为 false 可关闭计数器.""");
        enabled = globalConfig.getBoolean(basePath() + ".enabled", true);
        if (!enabled) {
            HighScaleMetricsRegistry.reset();
        }
    }
}
