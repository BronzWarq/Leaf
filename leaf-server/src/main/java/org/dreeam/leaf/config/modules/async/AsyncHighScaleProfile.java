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
    public static boolean compatibilityMode = true;
    public static boolean skipPrimedTntSaves = false;
    public static boolean optimizeNonFlushPacketSending = false;
    public static boolean disableUselessParticles = false;
    public static boolean sleepingBlockEntities = false;
    public static boolean throttleMobSpawning = false;
    public static boolean onlyTickItemsInHand = false;
    public static boolean fasterRuntimeRandom = false;
    public static boolean fasterWorldgenRandom = false;
    public static boolean protectWeakProjectiles = false;
    public static boolean reuseRandomTickBlockPos = false;
    public static boolean onlyPlayerPushable = false;

    @Override
    public void onLoaded() {
        globalConfig.addCommentRegionBased(basePath(), """
                Enables the coordinated high-scale profile for servers with many concurrent players.
                compatibility-mode is enabled by default and blocks options that can change gameplay,
                world generation, persistence, or plugin packet behavior. Set it to false only after
                testing the affected mechanics on a staging server.
                This enables asynchronous entity tracking and pathfinding, spatial entity and
                mob-spawning and biome-cache optimizations, optimized powered-rail updates,
                faster structure-generation future sequencing, asynchronous player-data saving,
                optimized player movement processing,
                filtering of zero-delta entity movement packets, and filtering of redundant
                entity-motion packets. The optional particle reduction can suppress
                server-side auxiliary particles that clients already reproduce locally.
                Test plugins and gameplay mechanics before production use.
                skip-primed-tnt-saves is an opt-in TNT-heavy mode that avoids persisting
                primed TNT when chunks unload; it can discard active TNT on unload/restart.
                optimize-non-flush-packets is an opt-in network mode and is incompatible with
                ProtocolLib and plugins that intercept packet scheduling.
                disable-useless-particles is an opt-in visual/network mode that suppresses
                server-side sprint, splash, status, and similar auxiliary particles; clients
                may still render their own equivalent effects.
                sleeping-block-entities is an opt-in farm mode that sleeps inactive hoppers,
                containers, furnaces, and supported block entities until their state changes.
                throttle-mob-spawning is an opt-in farm mode that backs off repeated failed
                natural-spawn attempts in saturated chunks; it can delay mob-farm recovery.
                only-tick-items-in-hand is an opt-in player-throughput mode that skips inventory
                item ticks outside the main hand; custom items and plugins may depend on those ticks.
                faster-runtime-random is an opt-in CPU optimization for runtime random calls used by
                explosions and entity logic; it does not enable the world-generation RNG option.
                worldgen-fast-random is an opt-in world-generation CPU optimization that changes
                the random source used by terrain and structure generation; it can change generated
                terrain and slime-chunk locations for a seed and should be enabled only for new worlds.
                protect-weak-projectiles is an opt-in elytra/firework safety mode that prevents
                projectiles from moving into entity-unticking chunks; a rocket can be discarded at
                an unloaded boundary instead of forcing more chunk work.
                reuse-random-tick-block-pos is an opt-in allocation reduction for random ticks;
                plugins or blocks that retain the provided position may observe the reused object.
                only-player-pushable is an opt-in dense-farm mode that restricts entity pushing
                to players; mob farms relying on mob-to-mob pushing will behave differently.
                This does not enable parallel world ticking or packet optimizations that are
                incompatible with packet-intercepting plugins. World-generation RNG remains off
                unless worldgen-fast-random is explicitly enabled.
                Requires a restart when enabling asynchronous modules.""",
            """
                为大量并发玩家启用高负载配置组合.
                compatibility-mode 默认启用，会阻止可能改变玩法、世界生成、持久化或插件数据包行为的选项. 只有在测试服务器验证相关机制后才应将其设置为 false.
                将启用异步实体跟踪、异步寻路、实体和生物生成空间优化、生成用生物群系缓存、优化铁轨更新，并过滤位移为零的实体移动数据包.
                同时启用更快的结构生成任务编排、异步玩家数据保存、优化玩家移动处理和冗余实体运动数据包过滤.
                可选的粒子优化可以省略客户端通常会自行生成的服务器辅助粒子.
                正式使用前请测试插件和玩法.
                skip-primed-tnt-saves 是 TNT 高负载模式的可选开关，区块卸载时不保存已激活的 TNT；卸载或重启可能丢弃尚未爆炸的 TNT.
                optimize-non-flush-packets 是可选网络模式，与 ProtocolLib 和拦截数据包调度的插件不兼容.
                disable-useless-particles 是可选视觉/网络模式，会省略服务器生成的疾跑、溅水、状态和类似辅助粒子；客户端仍可能自行显示相应效果.
                sleeping-block-entities 是可选的农场模式，在状态变化前暂停不活跃的漏斗、容器、熔炉和其他受支持的方块实体.
                throttle-mob-spawning 是可选的农场模式，在生物已达到限制的区块中降低重复失败的自然生成尝试；可能延迟生物农场恢复.
                only-tick-items-in-hand 是可选的玩家吞吐模式，跳过主手之外的物品栏物品 tick；自定义物品和插件可能依赖这些 tick.
                faster-runtime-random 是可选的运行时随机数 CPU 优化，用于爆炸和实体逻辑；不会启用世界生成随机数选项.
                worldgen-fast-random 是可选的世界生成 CPU 优化，会改变地形和结构生成使用的随机源；可能改变同一种子下的地形和史莱姆区块位置，只应对新世界启用.
                protect-weak-projectiles 是可选的鞘翅/烟花保护模式，阻止弹射物进入实体未 tick 的区块；火箭可能在未加载边界被移除，而不是继续增加区块工作.
                reuse-random-tick-block-pos 是可选的 random tick 分配优化；保存传入位置的插件或方块可能观察到复用的对象.
                only-player-pushable 是可选的密集农场模式，只允许实体推动玩家；依赖生物之间相互推动的农场行为会改变.
                默认不会启用并行世界 tick、改变世界生成随机数，也不会启用与数据包拦截插件冲突的优化；只有明确启用 worldgen-fast-random 才会改变世界生成随机数.
                启用异步模块后需要重启服务器.""");

        enabled = globalConfig.getBoolean(basePath() + ".enabled", enabled);
        compatibilityMode = globalConfig.getBoolean(basePath() + ".compatibility-mode", true,
            globalConfig.pickStringRegionBased(
                "Keep gameplay, world-generation, persistence, and plugin compatibility safeguards enabled.",
                "保持玩法、世界生成、持久化和插件兼容性保护启用."));
        skipPrimedTntSaves = enabled && !compatibilityMode && globalConfig.getBoolean(basePath() + ".skip-primed-tnt-saves", skipPrimedTntSaves);
        optimizeNonFlushPacketSending = enabled && !compatibilityMode && globalConfig.getBoolean(basePath() + ".optimize-non-flush-packets", optimizeNonFlushPacketSending);
        disableUselessParticles = enabled && !compatibilityMode && globalConfig.getBoolean(basePath() + ".disable-useless-particles", disableUselessParticles,
            globalConfig.pickStringRegionBased(
                "Suppresses server-side auxiliary particles such as sprint, splash, status, and poof effects.",
                "省略疾跑、溅水、状态和 poof 等服务器辅助粒子."));
        sleepingBlockEntities = enabled && !compatibilityMode && globalConfig.getBoolean(basePath() + ".sleeping-block-entities", sleepingBlockEntities);
        throttleMobSpawning = enabled && !compatibilityMode && globalConfig.getBoolean(basePath() + ".throttle-mob-spawning", throttleMobSpawning);
        onlyTickItemsInHand = enabled && !compatibilityMode && globalConfig.getBoolean(basePath() + ".only-tick-items-in-hand", onlyTickItemsInHand);
        fasterRuntimeRandom = enabled && !compatibilityMode && globalConfig.getBoolean(basePath() + ".faster-runtime-random", fasterRuntimeRandom);
        fasterWorldgenRandom = enabled && !compatibilityMode && globalConfig.getBoolean(basePath() + ".worldgen-fast-random", fasterWorldgenRandom,
            globalConfig.pickStringRegionBased(
                "Changes the random source used by world generation; use only for new worlds.",
                "改变世界生成使用的随机源；只应对新世界启用."));
        protectWeakProjectiles = enabled && !compatibilityMode && globalConfig.getBoolean(basePath() + ".protect-weak-projectiles", protectWeakProjectiles,
            globalConfig.pickStringRegionBased(
                "Prevents projectiles from entering weak loaded chunks; fireworks may be discarded at an unloaded boundary.",
                "阻止弹射物进入弱加载区块；烟花可能在未加载边界被移除."));
        reuseRandomTickBlockPos = enabled && !compatibilityMode && globalConfig.getBoolean(basePath() + ".reuse-random-tick-block-pos", reuseRandomTickBlockPos,
            globalConfig.pickStringRegionBased(
                "Reuses the random-tick BlockPos object; disable if plugins retain the supplied position.",
                "复用 random tick 的 BlockPos 对象；如果插件保存传入位置请关闭."));
        onlyPlayerPushable = enabled && !compatibilityMode && globalConfig.getBoolean(basePath() + ".only-player-pushable", onlyPlayerPushable,
            globalConfig.pickStringRegionBased(
                "Restricts entity pushing to players; mob-to-mob pushing farms will behave differently.",
                "将实体推动限制为玩家；生物之间相互推动的农场行为会改变."));
        if (enabled) {
            LOGGER.warn("High-scale profile is enabled; verify plugins and gameplay mechanics before production use.");
            if (!compatibilityMode) {
                LOGGER.warn("High-scale compatibility mode is disabled; gameplay, world-generation, persistence, and plugin compatibility changes are allowed.");
            }
        }
    }
}
