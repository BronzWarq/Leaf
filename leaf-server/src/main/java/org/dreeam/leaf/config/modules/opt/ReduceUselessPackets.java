package org.dreeam.leaf.config.modules.opt;

import org.dreeam.leaf.config.ConfigModule;
import org.dreeam.leaf.config.ConfigCategory;

public class ReduceUselessPackets extends ConfigModule {

    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".reduce-packets";
    }

    public static boolean reduceUselessEntityMovePackets = false;
    public static boolean filterClientboundSetEntityMotionPacket = false;
    public static boolean disableUselessParticles = false;
    public static boolean coalesceTntExplosionEffects = false;

    @Override
    public void onLoaded() {
        reduceUselessEntityMovePackets = globalConfig.getBoolean(basePath() + ".reduce-entity-move-packets", reduceUselessEntityMovePackets);
        reduceUselessEntityMovePackets |= org.dreeam.leaf.config.modules.async.AsyncHighScaleProfile.enabled;
        filterClientboundSetEntityMotionPacket = globalConfig.getBoolean(basePath() + ".reduce-entity-motion-packets", filterClientboundSetEntityMotionPacket);
        filterClientboundSetEntityMotionPacket |= org.dreeam.leaf.config.modules.async.AsyncHighScaleProfile.enabled;
        disableUselessParticles = globalConfig.getBoolean(basePath() + ".disable-useless-particles", disableUselessParticles);
        disableUselessParticles |= org.dreeam.leaf.config.modules.async.AsyncHighScaleProfile.disableUselessParticles;
        coalesceTntExplosionEffects = globalConfig.getBoolean(basePath() + ".coalesce-tnt-explosion-effects", false,
            globalConfig.pickStringRegionBased(
                "Reduce repeated TNT explosion visuals and sounds per player and 16-block area per tick; knockback packets remain intact.",
                "减少每位玩家每刻在 16 方块区域内重复的 TNT 爆炸效果和声音；保留击退数据包。"));
    }
}
