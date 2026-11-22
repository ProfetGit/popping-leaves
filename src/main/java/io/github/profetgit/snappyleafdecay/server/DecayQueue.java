package io.github.profetgit.snappyleafdecay.server;

import io.github.profetgit.snappyleafdecay.Config;
import io.github.profetgit.snappyleafdecay.Platform;
import io.github.profetgit.snappyleafdecay.SnappyLeafDecay;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * The server half (PRD 5.1): a leaf that becomes decaying is queued, and goes a few ticks after its earliest decaying
 * neighbour, so a cut tree loses its leaves in one ripple instead of one random tick each. At the due tick the block is
 * read again and vanilla's own removal path runs (same drops as {@code LeavesBlock.randomTick}). The queue is per level,
 * never saved and never static across worlds; what is lost on a restart falls back to vanilla random ticks.
 */
public final class DecayQueue {
    private static final Map<ServerLevel, Queue> QUEUES = new WeakHashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();
    /** Other mods that already speed leaf decay up: the server half stays off (ids from their listings, unverified). */
    private static final String[] OTHERS = {"leavesbegone", "leaves_be_gone", "fastleafdecay", "fast_leaf_decay", "leaves_us_in_peace",
        "accelerated_decay", "accelerateddecay", "shatterleaves", "shatter_leaves", "canopyfade", "canopy_fade", "leaf_decay_instant",
        "ripple_leaf_decay", "rapid_leaf_decay", "quick_leaf_decay", "modern_leaf_decay", "instant_decay", "minimalism_leaf_decay"};
    private static String other;
    private static boolean otherChecked;

    /** Test hooks: totals since the JVM started (the scenes read differences). */
    public static volatile int queuedTotal, removedTotal, staleTotal, firstRemovedTick = -1, lastRemovedTick = -1;
    public static volatile long maxTickNanos, sumTickNanos;
    public static volatile int busyTicks, maxRemovedInTick;
    /** Test hook: the nanos of every busy tick since the last reset (capped), for a percentile. */
    public static final it.unimi.dsi.fastutil.longs.LongArrayList TICK_NANOS = new it.unimi.dsi.fastutil.longs.LongArrayList();

    private DecayQueue() {}

    private static final class Queue {
        long ticks;
        final Long2LongOpenHashMap due = new Long2LongOpenHashMap();
        /** (due tick, packed pos) ordered by due tick, then insertion. */
        final PriorityQueue<long[]> heap = new PriorityQueue<>((a, b) -> a[0] != b[0] ? Long.compare(a[0], b[0]) : Long.compare(a[2], b[2]));
        long seq;
        /** Where logs were just cut away: (x, y, z, tick), newest last. */
        final java.util.ArrayList<long[]> origins = new java.util.ArrayList<>();
    }

    private static final int ORIGIN_LIFE = 200, ORIGIN_CAP = 128;
    private static final double ORIGIN_RANGE = 48;

    private static boolean decaying(BlockState s) {
        return s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT) && s.getValue(LeavesBlock.DISTANCE) == LeavesBlock.DECAY_DISTANCE;
    }

    /** Whether the server half runs for this level (config, other mods, {@code randomTickSpeed} 0). */
    private static boolean active(ServerLevel level) {
        if (!Config.server().enabled) return false;
        if (!otherChecked) {
            otherChecked = true;
            for (String id : OTHERS) {
                if (Platform.isModLoaded(id)) {
                    other = id;
                    SnappyLeafDecay.LOG.info("Popping Leaves: {} already speeds up leaf decay, the server half stays off", id);
                    break;
                }
            }
        }
        return other == null && (!Config.server().respectRandomTickSpeed || level.getGameRules().get(GameRules.RANDOM_TICK_SPEED) > 0);
    }

    /** From {@code LeavesBlock.tick} (after the distance update): queue the leaf when it has just become decaying. */
    public static void leafTicked(ServerLevel level, BlockPos pos) {
        if (!decaying(level.getBlockState(pos)) || !active(level)) return;
        Queue q = QUEUES.computeIfAbsent(level, l -> new Queue());
        long key = pos.asLong();
        if (q.due.containsKey(key)) return;
        Config.Server c = Config.server();
        double nearest = Double.MAX_VALUE;
        for (int i = q.origins.size() - 1; i >= 0; i--) {
            long[] o = q.origins.get(i);
            if (q.ticks - o[3] > ORIGIN_LIFE) {
                q.origins.remove(i);
                continue;
            }
            double dx = o[0] - pos.getX(), dy = o[1] - pos.getY(), dz = o[2] - pos.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < nearest) nearest = d2;
        }
        long due;
        if (nearest <= ORIGIN_RANGE * ORIGIN_RANGE) {
            due = q.ticks + c.delay + Math.min(c.maxRipple, Math.round(Math.sqrt(nearest) * c.stagger)) + RANDOM.nextInt(c.jitter + 1);
        } else {
            // no cut seen nearby (a leaf that was already dying, a block placed over a trunk): a short random spread
            due = q.ticks + c.delay + RANDOM.nextInt(3 * c.jitter + 6);
        }
        q.due.put(key, due);
        q.heap.add(new long[] {due, key, q.seq++});
        queuedTotal++;
    }

    /**
     * From {@code LeavesBlock.updateShape}: a leaf that was next to a log (distance 1) saw that neighbour become something
     * else, so a log was cut there. The ripple of the decay starts from these spots.
     */
    public static void leafLostNeighbour(ServerLevel level, BlockPos neighbour) {
        if (!Config.server().enabled) return;
        Queue q = QUEUES.computeIfAbsent(level, l -> new Queue());
        long[] o = {neighbour.getX(), neighbour.getY(), neighbour.getZ(), q.ticks};
        for (int i = q.origins.size() - 1; i >= 0; i--) {
            long[] e = q.origins.get(i);
            if (e[0] == o[0] && e[1] == o[1] && e[2] == o[2]) {
                e[3] = o[3];
                return;
            }
        }
        if (q.origins.size() >= ORIGIN_CAP) q.origins.remove(0);
        q.origins.add(o);
    }

    /** END_LEVEL_TICK. */
    public static void tick(ServerLevel level) {
        Queue q = QUEUES.get(level);
        if (q == null) return;
        if (!level.tickRateManager().runsNormally()) return;
        q.ticks++;
        if (q.heap.isEmpty()) return;
        long t0 = System.nanoTime();
        int cap = Config.server().maxPerTick, done = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (done < cap && !q.heap.isEmpty() && q.heap.peek()[0] <= q.ticks) {
            long[] e = q.heap.poll();
            q.due.remove(e[1]);
            pos.set(e[1]);
            if (!level.hasChunkAt(pos)) continue;
            BlockState s = level.getBlockState(pos);
            if (!decaying(s)) {
                staleTotal++;
                continue;
            }
            Block.dropResources(s, level, pos);
            level.removeBlock(pos, false);
            done++;
        }
        if (done > 0) {
            removedTotal += done;
            if (firstRemovedTick < 0) firstRemovedTick = (int) q.ticks;
            lastRemovedTick = (int) q.ticks;
            busyTicks++;
            maxRemovedInTick = Math.max(maxRemovedInTick, done);
            long ns = System.nanoTime() - t0;
            sumTickNanos += ns;
            if (TICK_NANOS.size() < 4096) TICK_NANOS.add(ns);
            if (ns > maxTickNanos) maxTickNanos = ns;
        }
    }

    public static int pending(ServerLevel level) {
        Queue q = QUEUES.get(level);
        return q == null ? 0 : q.due.size();
    }

    public static long ticks(ServerLevel level) {
        Queue q = QUEUES.get(level);
        return q == null ? 0 : q.ticks;
    }

    public static void resetStats() {
        queuedTotal = removedTotal = staleTotal = busyTicks = maxRemovedInTick = 0;
        firstRemovedTick = lastRemovedTick = -1;
        maxTickNanos = sumTickNanos = 0;
        TICK_NANOS.clear();
    }

    /** SERVER_STOPPED: nothing may outlive its world in this JVM. */
    public static void clear() {
        QUEUES.clear();
    }
}
