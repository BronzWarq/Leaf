package org.dreeam.leaf.async.tracker;

import ca.spottedleaf.moonrise.common.list.ReferenceList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.dreeam.leaf.async.ThreadPool;
import org.dreeam.leaf.config.modules.async.MultithreadedTracker;
import org.dreeam.leaf.util.TrackerSlice;
import org.dreeam.leaf.util.HighScaleMetricsRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;

@NullMarked
public final class AsyncTracker {
    private static final String THREAD_NAME = "Leaf Async Tracker Thread";
    private static final int QUEUE = 1024;

    public static @Nullable ThreadPool TRACKER_EXECUTOR;

    private Future<TrackerCtx> @Nullable [] fut;
    private int futCount;
    // One tracker owns one world; the server joins its futures before the next tick.
    private TrackerCtx @Nullable [] contextPool = {};
    private TrackerSlice[] slicePool = {};
    private Future<TrackerCtx>[] futurePool = newFutureArray(0);
    private Object2ObjectOpenHashMap<ServerPlayerConnection, ObjectArrayList<Packet<?>>>[] packetMapPool = newPacketMapArray(0);

    /*
     * The tracker tasks only read the capture map after tick() has handed it
     * off. Keep two maps and swap them instead of cloning every entry on every
     * tick. The previous task has been joined by onTickEnd() before the map is
     * reused, so this remains single-writer/single-reader without additional
     * synchronization on the hot path.
     */
    private Reference2ReferenceOpenHashMap<ChunkMap.TrackedEntity, TrackerInput> capture = new Reference2ReferenceOpenHashMap<>();
    private Reference2ReferenceOpenHashMap<ChunkMap.TrackedEntity, TrackerInput> reusableCapture = new Reference2ReferenceOpenHashMap<>();
    private Entity[] entitySnapshot = {};
    private int entitySnapshotSize;

    public static void init() {
        if (TRACKER_EXECUTOR != null) {
            throw new IllegalStateException();
        }
        TRACKER_EXECUTOR = new ThreadPool(
            MultithreadedTracker.threads,
            QUEUE,
            THREAD_NAME
        );
    }

    public @Nullable TrackerInput get(final ChunkMap.@Nullable TrackedEntity tracker) {
        if (tracker == null) {
            return null;
        }
        TrackerInput i = capture.computeIfAbsent(tracker, _ -> new TrackerInput(Vec3.ZERO, Vec3.ZERO, false));
        i.trackingPosition = tracker.serverEntity.entity.trackingPosition();
        return i;
    }

    public void tick(final ServerLevel world, final ReferenceList<Entity> entities) {
        final Reference2ReferenceOpenHashMap<ChunkMap.TrackedEntity, TrackerInput> cap = capture;
        capture = reusableCapture;
        reusableCapture = cap;
        capture.clear();
        handlePlayer(world);
        int len = entities.size();
        if (len == 0) {
            if (entitySnapshotSize != 0) {
                Arrays.fill(entitySnapshot, 0, entitySnapshotSize, null);
                entitySnapshotSize = 0;
            }
            return;
        }
        if (entitySnapshot.length < len) {
            entitySnapshot = new Entity[len];
        } else if (entitySnapshotSize > len) {
            Arrays.fill(entitySnapshot, len, entitySnapshotSize, null);
        }
        System.arraycopy(entities.getRawDataUnchecked(), 0, entitySnapshot, 0, len);
        entitySnapshotSize = len;
        TrackerSlice slice = new TrackerSlice(entitySnapshot, 0, len);

        ThreadPool exec = Objects.requireNonNull(TRACKER_EXECUTOR);
        int min = MultithreadedTracker.minEntitiesPerTask;
        long taskThreshold = (long) exec.threadCount() * min;
        int maxSlices = len <= taskThreshold ? (len - 1) / min + 1 : exec.threadCount();
        if (slicePool.length < maxSlices) {
            slicePool = Arrays.copyOf(slicePool, maxSlices);
        }
        final int sliceCount = len <= taskThreshold
            ? slice.chunks(min, slicePool)
            : slice.splitEvenly(exec.threadCount(), slicePool);
        HighScaleMetricsRegistry.recordTrackerTick(len, sliceCount);
        if (contextPool.length < sliceCount) {
            contextPool = Arrays.copyOf(contextPool, sliceCount);
        }
        if (futurePool.length < sliceCount) {
            futurePool = newFutureArray(sliceCount);
        }
        Future<TrackerCtx>[] futures = futurePool;
        for (int i = 0; i < sliceCount; i++) {
            TrackerCtx context = contextPool[i];
            if (context == null) {
                context = new TrackerCtx(world);
                contextPool[i] = context;
            }
            futures[i] = exec.submitOrRun(new TrackerTask(world, slicePool[i], cap, context));
        }
        exec.unpark();
        this.fut = futures;
        this.futCount = sliceCount;
    }

    private static void handlePlayer(final ServerLevel world) {
        for (final ServerPlayer player : world.players()) {
            player.updateDataBeforeSync();

            if (!player.syncVelocity) {
                continue;
            }
            player.syncVelocity = false;
            boolean cancelled = false;

            org.bukkit.entity.Player craftPlayer = player.getBukkitEntity();
            org.bukkit.util.Vector velocity = craftPlayer.getVelocity();

            PlayerVelocityEvent event = new PlayerVelocityEvent(craftPlayer, velocity.clone());
            if (!event.callEvent()) {
                cancelled = true;
            } else if (velocity != event.getVelocity() && !velocity.equals(event.getVelocity())) {
                craftPlayer.setVelocity(event.getVelocity());
            }
            if (cancelled) {
                continue;
            }
            ChunkMap.TrackedEntity trackedEntity = player.moonrise$getTrackedEntity();
            //noinspection ConstantValue
            if (trackedEntity == null) {
                continue;
            }
            trackedEntity.sendToTrackingPlayersAndSelf(new ClientboundSetEntityMotionPacket(player));
        }
    }

    public void onEntitiesTickEnd() {
        Future<TrackerCtx>[] task = this.fut;
        if (task == null) {
            return;
        }
        final int taskCount = this.futCount;
        for (int i = 0; i < taskCount; i++) {
            if (!task[i].isDone()) {
                return;
            }
        }
        this.fut = null;
        this.futCount = 0;
        handle(task, taskCount);
    }

    public void onTickEnd() {
        Future<TrackerCtx>[] task = this.fut;
        final int taskCount = this.futCount;
        this.fut = null;
        this.futCount = 0;
        if (task == null) {
            return;
        }
        handle(task, taskCount);
    }

    @SuppressWarnings("unchecked")
    private static Object2ObjectOpenHashMap<ServerPlayerConnection, ObjectArrayList<Packet<?>>>[] newPacketMapArray(final int length) {
        return (Object2ObjectOpenHashMap<ServerPlayerConnection, ObjectArrayList<Packet<?>>>[]) new Object2ObjectOpenHashMap[length];
    }

    @SuppressWarnings("unchecked")
    private static Future<TrackerCtx>[] newFutureArray(final int length) {
        return (Future<TrackerCtx>[]) new Future[length];
    }

    private void handle(final Future<TrackerCtx>[] futures, final int futureCount) {
        final int packetMapCount = futureCount - 1;
        boolean interrupted = false;
        try {
            TrackerCtx ctx;
            for (;;) {
                try {
                    ctx = futures[0].get();
                    break;
                } catch (final InterruptedException e) {
                    // Drain the task before reusing its context and capture map.
                    interrupted = true;
                }
            }
            if (packetMapPool.length < packetMapCount) {
                packetMapPool = newPacketMapArray(packetMapCount);
            }
            for (int i = 0; i < packetMapCount; i++) {
                TrackerCtx other;
                for (;;) {
                    try {
                        other = futures[i + 1].get();
                        break;
                    } catch (final InterruptedException e) {
                        interrupted = true;
                    }
                }
                packetMapPool[i] = ctx.join(other);
            }
            ctx.handle(packetMapPool, packetMapCount);
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            Arrays.fill(packetMapPool, 0, Math.min(packetMapCount, packetMapPool.length), null);
            Arrays.fill(futures, 0, futureCount, null);
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
