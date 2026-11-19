package org.dreeam.leaf.config.modules.async;

import org.dreeam.leaf.config.ConfigCategory;
import org.dreeam.leaf.config.ConfigModule;

/**
 * Opt-in bundle for servers that prioritize high player/entity throughput.
 *
 * <p>This deliberately does not enable parallel world ticking, alternate
 * world-generation RNG, or packet changes with known plugin incompatibilities.
 */
public final class AsyncHighScaleProfile extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".high-scale-profile";
    }

    public static boolean enabled = false;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath(), """
                Enables the coordinated high-scale profile for servers with many concurrent players.
                This enables asynchronous entity tracking and pathfinding, spatial entity and
                mob-spawning and biome-cache optimizations, and filtering of zero-delta entity movement packets.
                Test plugins and gameplay mechanics before production use.
                This does not enable parallel world ticking, alternate world-generation RNG, or
                packet optimizations that are incompatible with packet-intercepting plugins.
                Requires a restart when enabling asynchronous modules.""",
            """
                为大量并发玩家启用高负载配置组合.
                将启用异步实体跟踪、异步寻路、实体和生物生成空间优化、生成用生物群系缓存，并过滤位移为零的实体移动数据包.
                正式使用前请测试插件和玩法.
                不会启用并行世界 tick、改变世界生成随机数，也不会启用与数据包拦截插件冲突的优化.
                启用异步模块后需要重启服务器.""");

        enabled = globalConfig.getBoolean(basePath() + ".enabled", enabled);
        if (enabled) {
            LOGGER.warn("High-scale profile is enabled; verify plugins and gameplay mechanics before production use.");
        }
    }
}
