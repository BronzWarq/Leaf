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
    public static boolean skipPrimedTntSaves = false;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath(), """
                Enables the coordinated high-scale profile for servers with many concurrent players.
                This enables asynchronous entity tracking and pathfinding, spatial entity and
                mob-spawning and biome-cache optimizations, optimized powered-rail updates,
                faster structure-generation future sequencing, asynchronous player-data saving,
                optimized player movement processing,
                filtering of zero-delta entity movement packets, and filtering of redundant
                entity-motion packets.
                Test plugins and gameplay mechanics before production use.
                skip-primed-tnt-saves is an opt-in TNT-heavy mode that avoids persisting
                primed TNT when chunks unload; it can discard active TNT on unload/restart.
                This does not enable parallel world ticking, alternate world-generation RNG, or
                packet optimizations that are incompatible with packet-intercepting plugins.
                Requires a restart when enabling asynchronous modules.""",
            """
                为大量并发玩家启用高负载配置组合.
                将启用异步实体跟踪、异步寻路、实体和生物生成空间优化、生成用生物群系缓存、优化铁轨更新，并过滤位移为零的实体移动数据包.
                同时启用更快的结构生成任务编排、异步玩家数据保存、优化玩家移动处理和冗余实体运动数据包过滤.
                正式使用前请测试插件和玩法.
                skip-primed-tnt-saves 是 TNT 高负载模式的可选开关，区块卸载时不保存已激活的 TNT；卸载或重启可能丢弃尚未爆炸的 TNT.
                不会启用并行世界 tick、改变世界生成随机数，也不会启用与数据包拦截插件冲突的优化.
                启用异步模块后需要重启服务器.""");

        enabled = globalConfig.getBoolean(basePath() + ".enabled", enabled);
        skipPrimedTntSaves = enabled && globalConfig.getBoolean(basePath() + ".skip-primed-tnt-saves", skipPrimedTntSaves);
        if (enabled) {
            LOGGER.warn("High-scale profile is enabled; verify plugins and gameplay mechanics before production use.");
        }
    }
}
