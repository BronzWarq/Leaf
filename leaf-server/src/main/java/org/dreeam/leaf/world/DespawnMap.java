package org.dreeam.leaf.world;

import io.papermc.paper.configuration.WorldConfiguration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.dreeam.leaf.util.KDTree3D;

import java.util.Arrays;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Consumer;

public final class DespawnMap implements Consumer<Entity> {

    private static final MobCategory[] CATEGORIES = MobCategory.values();

    private final KDTree3D tree = new KDTree3D();
    private final double[] hard = new double[CATEGORIES.length];
    private final double[] sort = new double[CATEGORIES.length];
    private final double[][] coordinates = new double[3][];
    private double[] playerX = {};
    private double[] playerY = {};
    private double[] playerZ = {};
    private int[] playerIndices = {};
    private boolean difficultyIsPeaceful = true;

    public boolean tick(final ServerLevel world, final EntityTickList entityTickList) {
        if (!org.dreeam.leaf.config.modules.opt.OptimizeDespawn.enabled) {
            return false;
        }
        for (int i = 0; i < CATEGORIES.length; i++) {
            sort[i] = CATEGORIES[i].getNoDespawnDistance();
            hard[i] = CATEGORIES[i].getDespawnDistance();
        }
        boolean fallback = false;
        for (Map.Entry<MobCategory, WorldConfiguration.Entities.Spawning.DespawnRangePair> e : world.paperConfig().entities.spawning.despawnRanges.entrySet()) {
            OptionalInt softVertical = e.getValue().soft().verticalLimit.value();
            OptionalInt softHorizontal = e.getValue().soft().horizontalLimit.value();
            OptionalInt hardVertical = e.getValue().hard().verticalLimit.value();
            OptionalInt hardHorizontal = e.getValue().hard().horizontalLimit.value();
            if (softVertical.isPresent() && softHorizontal.isPresent() && softVertical.getAsInt() == softHorizontal.getAsInt()) {
                sort[e.getKey().ordinal()] = softVertical.getAsInt();
            } else if (softVertical.isPresent() || softHorizontal.isPresent()) {
                fallback = true;
            }
            if (hardVertical.isPresent() && hardHorizontal.isPresent() && hardVertical.getAsInt() == hardHorizontal.getAsInt()) {
                hard[e.getKey().ordinal()] = hardVertical.getAsInt();
            } else if (hardVertical.isPresent() || hardHorizontal.isPresent()) {
                fallback = true;
            }
        }

        this.difficultyIsPeaceful = world.getDifficulty() == Difficulty.PEACEFUL;

        if (fallback) {
            return false;
        }
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (sort[i] > 0.0) {
                sort[i] = sort[i] * sort[i];
            }
            if (hard[i] > 0.0) {
                hard[i] = hard[i] * hard[i];
            }
            if (sort[i] > hard[i]) {
                sort[i] = hard[i];
            }
        }
        final var worldPlayers = world.players();
        ensurePlayerCapacity(worldPlayers.size());
        int i = 0;
        for (final ServerPlayer p : worldPlayers) {
            if (EntitySelector.PLAYER_AFFECTS_SPAWNING.test(p)) {
                playerX[i] = p.getX();
                playerY[i] = p.getY();
                playerZ[i] = p.getZ();
                i++;
            }
        }
        if (playerIndices.length != i) {
            playerIndices = new int[i];
        }
        coordinates[0] = playerX;
        coordinates[1] = playerY;
        coordinates[2] = playerZ;
        tree.build(coordinates, playerIndices);
        entityTickList.forEach(this);
        return true;
    }

    private void ensurePlayerCapacity(final int required) {
        if (playerX.length >= required) {
            return;
        }
        final int capacity = Math.max(required, Math.max(playerX.length << 1, 16));
        playerX = Arrays.copyOf(playerX, capacity);
        playerY = Arrays.copyOf(playerY, capacity);
        playerZ = Arrays.copyOf(playerZ, capacity);
    }

    private boolean checkDespawn(final Entity entity) {
        EntityType<?> t = entity.getType();
        if (entity instanceof EnderDragon) {
            // [EnderDragon#checkDespawn()]
            return false;
        } else if (entity instanceof WitherBoss witherBoss) {
            // [WitherBoss#checkDespawn()]
            if (difficultyIsPeaceful && !t.isAllowedInPeaceful()) {
                return true;
            }
            witherBoss.noActionTime = 0;
            return false;
        } else if (entity instanceof final Mob mob) {
            // [Mob#checkDespawn()]
            if (difficultyIsPeaceful && !t.isAllowedInPeaceful()) {
                return true;
            }
            boolean isPersistent = mob.isPersistenceRequired() || mob.requiresCustomPersistence();

            final int category = t.getCategory().ordinal();
            final double hardDist = this.hard[category];
            double maxDist = hardDist + 0.0001;
            if (mob instanceof PatrollingMonster) {
                // [PatrollingMonster#removeWhenFarAway]
                maxDist = Math.max(16384.0001, maxDist);
            }
            final Vec3 pos = mob.position;
            final double dist = this.tree.nearestSqr(pos.x, pos.y, pos.z, maxDist);
            if (dist == Double.POSITIVE_INFINITY) {
                return false;
            }
            if (!isPersistent && dist > hardDist) {
                return mob.removeWhenFarAway(dist);
            }
            if (dist > this.sort[category]) {
                return !isPersistent && mob.noActionTime > 600
                    && mob.getRandom().nextInt(800) == 0
                    && mob.removeWhenFarAway(dist);
            }
            mob.noActionTime = 0;
            return false;
        } else if (entity instanceof ShulkerBullet) {
            // [ShulkerBullet#checkDespawn()]
            return difficultyIsPeaceful;
        } else {
            // [Entity#checkDespawn()]
            return false;
        }
    }

    @Override
    public void accept(final Entity entity) {
        if (!entity.isRemoved() && checkDespawn(entity)) {
            entity.discard(EntityRemoveEvent.Cause.DESPAWN);
        }
    }
}
