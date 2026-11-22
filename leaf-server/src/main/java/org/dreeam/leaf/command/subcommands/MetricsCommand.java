package org.dreeam.leaf.command.subcommands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import org.dreeam.leaf.command.LeafCommand;
import org.dreeam.leaf.command.PermissionedLeafSubcommand;
import org.dreeam.leaf.config.modules.opt.HighScaleMetrics;
import org.dreeam.leaf.util.HighScaleMetricsRegistry;

public final class MetricsCommand extends PermissionedLeafSubcommand {
    public static final String LITERAL_ARGUMENT = "metrics";
    public static final String PERM = LeafCommand.BASE_PERM + "." + LITERAL_ARGUMENT;

    public MetricsCommand() {
        super(PERM, PermissionDefault.OP);
    }

    @Override
    public boolean execute(final CommandSender sender, final String subCommand, final String[] args) {
        if (!HighScaleMetrics.enabled) {
            sender.sendMessage(Component.text("High-scale metrics are disabled. Enable performance.high-scale-metrics.enabled first."));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            HighScaleMetricsRegistry.reset();
            sender.sendMessage(Component.text("High-scale metrics reset."));
            return true;
        }

        final HighScaleMetricsRegistry.Snapshot snapshot = HighScaleMetricsRegistry.snapshot();
        sender.sendMessage(Component.text("High-scale metrics (since last reset):"));
        sender.sendMessage(Component.text("  pathfinding: submitted=" + snapshot.pathSubmitted()
            + ", rejected=" + snapshot.pathRejected()
            + ", queue-peak=" + snapshot.pathQueuePeak()));
        sender.sendMessage(Component.text("  mob-spawn: submitted=" + snapshot.mobSpawnSubmitted()
            + ", inline=" + snapshot.mobSpawnInline()
            + ", deferred=" + snapshot.mobSpawnDeferred()
            + ", queue-peak=" + snapshot.mobSpawnQueuePeak()));
        sender.sendMessage(Component.text("  player-save: submitted=" + snapshot.playerSavesSubmitted()
            + ", queue-peak=" + snapshot.playerSaveQueuePeak()));
        sender.sendMessage(Component.text("  tracker: ticks=" + snapshot.trackerTicks()
            + ", tasks=" + snapshot.trackerTasks()
            + ", entities=" + snapshot.trackerEntities()
            + ", caller-runs=" + snapshot.trackerCallerRuns()));
        sender.sendMessage(Component.text("  random-tick: runs=" + snapshot.randomTickRuns()
            + ", samples=" + snapshot.randomTickSamples()
            + ", queue-peak=" + snapshot.randomTickQueuePeak()));
        sender.sendMessage(Component.text("  tnt-limit: deferred-entity-ticks=" + snapshot.tntDeferredEntityTicks()
            + ", limited-world-ticks=" + snapshot.tntBudgetLimitedWorldTicks()
            + ", deferred-per-world-tick-peak=" + snapshot.tntDeferredPerWorldTickPeak()));
        sender.sendMessage(Component.text("  tnt-fuse: expired=" + snapshot.tntFuseExpired()
            + ", gamerule-blocked=" + snapshot.tntRuleBlocked()
            + ", explosion-prime-cancelled=" + snapshot.tntExplosionPrimeCancelled()));
        sender.sendMessage(Component.text("  tnt-events: pre-explosion-cancelled=" + snapshot.tntPreExplosionCancelled()
            + ", block-explosion-cancelled=" + snapshot.tntBlockExplosionCancelled()
            + ", block-prime-cancelled=" + snapshot.tntBlockPrimeCancelled()));
        sender.sendMessage(Component.text("  tnt-spawn: direct-rejected=" + snapshot.tntDirectSpawnRejected()
            + ", explosion-rejected=" + snapshot.tntExplosionSpawnRejected()));
        sender.sendMessage(Component.text("  tnt-effects: packets-sent=" + snapshot.tntEffectPacketsSent()
            + ", with-knockback=" + snapshot.tntEffectPacketsWithKnockback()
            + ", suppressed=" + snapshot.tntEffectPacketsSuppressed()));
        return true;
    }
}
