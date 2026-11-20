package org.dreeam.leaf.world;

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup;
import io.papermc.paper.entity.activation.ActivationType;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.dreeam.leaf.config.modules.opt.DynamicActivationofBrain;
import org.dreeam.leaf.util.KDTree2D;
import org.dreeam.leaf.util.KDTree3D;

import java.util.Arrays;

public final class EntityActivation {
    private static final ActivationType[] ACTIVATION_TYPES = ActivationType.values();
    private static final ServerPlayer[] EMPTY_PLAYERS = {};

    private final ObjectArrayList<Entity> entityListCache = new ObjectArrayList<>();
    private final LongOpenHashSet chunks = new LongOpenHashSet();
    private final KDTree2D kdTree2 = new KDTree2D();
    private final KDTree3D kdTree3 = new KDTree3D();
    private final double[] ranges = new double[ACTIVATION_TYPES.length];
    private final double[][] coordinates2 = new double[2][];
    private final double[][] coordinates3 = new double[3][];
    private ServerPlayer[] players = EMPTY_PLAYERS;
    private double[] playerX = {};
    private double[] playerY = {};
    private double[] playerZ = {};
    private int[] playerIndices = {};
    private int lastPlayerCount;
    private boolean playerTreeInitialized;
    private boolean lastDab;

    public void activateEntities(ServerLevel world) {
        final int miscActivationRange = world.spigotConfig.miscActivationRange;
        final int raiderActivationRange = world.spigotConfig.raiderActivationRange;
        final int animalActivationRange = world.spigotConfig.animalActivationRange;
        final int monsterActivationRange = world.spigotConfig.monsterActivationRange;
        final int waterActivationRange = world.spigotConfig.waterActivationRange;
        final int flyingActivationRange = world.spigotConfig.flyingMonsterActivationRange;
        final int villagerActivationRange = world.spigotConfig.villagerActivationRange;

        world.wakeupInactiveRemainingAnimals = Math.min(world.wakeupInactiveRemainingAnimals + 1, world.spigotConfig.wakeUpInactiveAnimals);
        world.wakeupInactiveRemainingVillagers = Math.min(world.wakeupInactiveRemainingVillagers + 1, world.spigotConfig.wakeUpInactiveVillagers);
        world.wakeupInactiveRemainingMonsters = Math.min(world.wakeupInactiveRemainingMonsters + 1, world.spigotConfig.wakeUpInactiveMonsters);
        world.wakeupInactiveRemainingFlying = Math.min(world.wakeupInactiveRemainingFlying + 1, world.spigotConfig.wakeUpInactiveFlying);

        int maxRange = Math.max(monsterActivationRange, animalActivationRange);
        maxRange = Math.max(maxRange, raiderActivationRange);
        maxRange = Math.max(maxRange, miscActivationRange);
        maxRange = Math.max(maxRange, flyingActivationRange);
        maxRange = Math.max(maxRange, waterActivationRange);
        maxRange = Math.max(maxRange, villagerActivationRange);
        maxRange = Math.min((world.spigotConfig.simulationDistance << 4) - 8, maxRange);

        Arrays.fill(ranges, 0.0);
        ranges[ActivationType.WATER.ordinal()] = waterActivationRange;
        ranges[ActivationType.FLYING_MONSTER.ordinal()] = flyingActivationRange;
        ranges[ActivationType.VILLAGER.ordinal()] = villagerActivationRange;
        ranges[ActivationType.MONSTER.ordinal()] = monsterActivationRange;
        ranges[ActivationType.ANIMAL.ordinal()] = animalActivationRange;
        ranges[ActivationType.RAIDER.ordinal()] = raiderActivationRange;
        ranges[ActivationType.MISC.ordinal()] = miscActivationRange;
        for (int i = 0; i < ranges.length; i++) {
            if (ranges[i] > 0.0) {
                ranges[i] = ranges[i] * ranges[i];
            }
        }

        final long currentTick = MinecraftServer.currentTick; // cast to long
        final ObjectArrayList<Entity> entities = this.entityListCache;
        final LongOpenHashSet chunks = this.chunks;
        final ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup lookup = ((ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemLevel) world).moonrise$getEntityLookup();
        final KDTree2D kdTree2 = this.kdTree2;
        final KDTree3D kdTree3 = this.kdTree3;
        final boolean tickMarkers = world.paperConfig().entities.markers.tick;
        final boolean dab = DynamicActivationofBrain.enabled;
        final var worldPlayers = world.players();
        ensurePlayerCapacity(worldPlayers.size());
        int playerSize = 0;
        boolean playerTreeDirty = !playerTreeInitialized || lastDab != dab;
        for (final ServerPlayer p : worldPlayers) {
            p.activatedTick = currentTick;
            if (world.spigotConfig.ignoreSpectatorActivation && p.isSpectator()) {
                continue;
            }
            if (!world.purpurConfig.idleTimeoutTickNearbyEntities && p.isAfk()) {
                continue; // Purpur - AFK API
            }
            final double x = p.getX();
            final double y = p.getY();
            final double z = p.getZ();
            if (playerSize >= lastPlayerCount
                || players[playerSize] != p
                || playerX[playerSize] != x
                || playerY[playerSize] != y
                || playerZ[playerSize] != z) {
                playerTreeDirty = true;
            }
            players[playerSize] = p;
            playerX[playerSize] = x;
            playerY[playerSize] = y;
            playerZ[playerSize] = z;
            playerSize++;
        }

        playerTreeDirty |= lastPlayerCount != playerSize;
        if (lastPlayerCount > playerSize) {
            Arrays.fill(players, playerSize, lastPlayerCount, null);
        }
        lastPlayerCount = playerSize;

        if (playerSize == 0) {
            entities.clear();
            chunks.clear();
            return;
        }

        if (playerIndices.length < playerSize) {
            playerIndices = Arrays.copyOf(playerIndices, Math.max(playerSize, Math.max(playerIndices.length << 1, 16)));
        }
        coordinates2[0] = playerX;
        coordinates2[1] = playerZ;
        if (playerTreeDirty) {
            kdTree2.build(coordinates2, playerIndices, playerSize);
            if (dab) {
                coordinates3[0] = playerX;
                coordinates3[1] = playerY;
                coordinates3[2] = playerZ;
                kdTree3.build(coordinates3, playerIndices, playerSize);
            }
            playerTreeInitialized = true;
            lastDab = dab;
        }

        final double worldHeight = world.getHeight();
        getEntities(world, players, playerSize, maxRange, worldHeight, lookup, chunks, entities);
        final Object[] raw = entities.elements();
        final int size = entities.size();

        if (size != 0) {
            activateEntities(size,
                raw,
                tickMarkers,
                currentTick,
                ranges,
                kdTree2,
                dab,
                DynamicActivationofBrain.dontEnableIfInWater,
                kdTree3,
                DynamicActivationofBrain.startDistanceSquared,
                Math.pow(2.0, -DynamicActivationofBrain.activationDistanceMod),
                DynamicActivationofBrain.maximumActivationPrio
            );
        }
        entities.clear();
        chunks.clear();
    }

    private static void activateEntities(int size, Object[] entities, boolean tickMarkers, long currentTick, double[] ranges, KDTree2D kdTree2, boolean dab, boolean dontEnableIfInWater, KDTree3D kdTree3, double startSq, double scale, int maxPriority) {
        for (int i = 0; i < size; i++) {
            final Entity entity = (Entity) entities[i];
            if (!tickMarkers && entity instanceof Marker) {
                continue;
            }
            if (currentTick <= entity.activatedTick) {
                continue;
            }
            final Vec3 p = entity.position;
            if (entity.defaultActivationState) {
                entity.activatedTick = currentTick;
            } else {
                final double max = ranges[entity.activationType.ordinal()];
                final double near = kdTree2.nearestSqr(p.x, p.z, max);
                if (near != max) {
                    entity.activatedTick = currentTick;
                }
            }
            final int priority;
            if (dab
                && entity.getType().dabEnabled
                && (!dontEnableIfInWater || !entity.isInWater() || entity.is(net.minecraft.tags.EntityTypeTags.CAN_BREATHE_UNDER_WATER))) {
                final double distSq = kdTree3.nearestSqr(p.x, p.y, p.z, 16384.0);
                //noinspection MathClampMigration
                priority = distSq > startSq ? Math.min(maxPriority, Math.max((int) (distSq * scale), 1)) : 1;
            } else {
                priority = 1;
            }
            entity.activatedPriority = priority;
        }
    }

    private void ensurePlayerCapacity(final int required) {
        if (players.length >= required) {
            return;
        }
        final int capacity = Math.max(required, Math.max(players.length << 1, 16));
        players = Arrays.copyOf(players, capacity);
        playerX = Arrays.copyOf(playerX, capacity);
        playerY = Arrays.copyOf(playerY, capacity);
        playerZ = Arrays.copyOf(playerZ, capacity);
    }

    private static void getEntities(ServerLevel world, ServerPlayer[] players, int playerSize, int maxRange, double worldHeight, EntityLookup lookup, LongOpenHashSet chunks, ObjectArrayList<Entity> entities) {
        for (int k = 0; k < playerSize; k++) {
            final ServerPlayer player = players[k];
            final AABB box = player.getBoundingBox().inflate(maxRange, worldHeight, maxRange);
            lookup.leaf$getEntities(box, chunks, entities);
            ca.spottedleaf.moonrise.common.PlatformHooks.get().addToGetEntities(world, null, box, null, entities);
        }
    }
}
