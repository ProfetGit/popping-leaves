package io.github.profetgit.snappyleafdecay.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.snappyleafdecay.Config;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The client half (PRD 5.2): a leaf that disappears while its last state was decaying and no break event came with it is a
 * decay. It holds a beat with a short squash, shrinks away about its centre and sprays chips cut from its own texture.
 * A leaf that has just become decaying sheds one chip (the anticipation; the block itself stays in the chunk mesh).
 * Player breaks (level event 2001), Timber's and Veinminer's removals are not decays and get nothing from here.
 */
public final class Decays {
    /** Pops with the whole effect, then with no chips, per client tick (PRD 10); beyond, vanilla only. */
    static final int FULL_BUDGET = 16, LITE_BUDGET = 64;
    /** Chips within this distance: all, then one, then none; copies out to {@link #RANGE}. */
    static final double FULL_RANGE = 24, NEAR_RANGE = 48, RANGE = 64;
    static final int MAX_CHIPS = 600;
    /** Chips made per frame at most (they cost about 55 us each with many mods): a big collapse spreads them over frames. */
    static final int CHIPS_PER_FRAME = 24;
    public static final int SOUNDS_PER_TICK = 6;
    static final int SHED_PER_TICK = 8;

    private static final class Pop {
        final BlockPos pos;
        final BlockState state;
        final double t0;
        final boolean lite;
        final PopRenderState whole = new PopRenderState();
        boolean chipped;

        Pop(BlockPos pos, BlockState state, double t0, boolean lite) {
            this.pos = pos;
            this.state = state;
            this.t0 = t0;
            this.lite = lite;
        }
    }

    private record Pending(long tick, BlockState state) {}

    private static final List<Pop> POPS = new ArrayList<>();
    private static final Long2ObjectOpenHashMap<Pending> PENDING = new Long2ObjectOpenHashMap<>();
    private static final Long2LongOpenHashMap EVENTS = new Long2LongOpenHashMap();
    private static final RandomSource RANDOM = RandomSource.create();
    private static long budgetTick = -1;
    private static int budgetUsed, shedUsed, soundsUsed, frameChips;

    /** Chips alive (counted by {@link Chip}); reset on level change. */
    public static int liveChips;
    /** Test hooks. */
    public static int started, lite, overBudget, breaks, sheds, sounds, chipsMade, farSkipped, framesActive, eventsSeen, changesSeen, expiredEvents, otherLoss, maxSoundsInTick;
    public static long firstStartTick = -1, lastStartTick = -1;
    public static String otherState = "";
    public static long spentNanos, chipNanos;

    private Decays() {}

    public static void resetTimes() {
        firstStartTick = lastStartTick = -1;
    }

    static void clear() {
        POPS.clear();
        PENDING.clear();
        EVENTS.clear();
        liveChips = 0;
    }

    public static int active() {
        return POPS.size();
    }

    public static int pending() {
        return PENDING.size();
    }

    private static boolean on() {
        return Config.client().enabled;
    }

    private static boolean decaying(BlockState s) {
        return s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT) && s.getValue(LeavesBlock.DISTANCE) == LeavesBlock.DECAY_DISTANCE;
    }

    private static boolean gone(BlockState s) {
        return s.isAir() || !s.getFluidState().isEmpty();
    }

    /** Every client block change (ClientLevelMixin). */
    public static void blockChanged(BlockPos pos, BlockState old, BlockState now) {
        if (!on()) return;
        if (decaying(old) && gone(now)) changesSeen++;
        if (old.getBlock() instanceof LeavesBlock && gone(now) && !decaying(old)) {
            otherLoss++;
            otherState = old.toString();
        }
        if (decaying(old) && gone(now)) {
            long key = pos.asLong();
            long ev = EVENTS.remove(key);
            if (ev != 0 && Clock.ticks() - (ev - 1) <= 1) {
                breaks++;
                return;
            }
            PENDING.put(key, new Pending(Clock.ticks(), old));
        } else if (decaying(now) && old.getBlock() instanceof LeavesBlock && !decaying(old)) {
            shed(pos, now);
        }
    }

    /** LevelEventHandler.levelEvent: 2001 is a block destroyed by a player (or the like): not ours. */
    public static void levelEvent(int type, BlockPos pos) {
        if (type != 2001 || !on()) return;
        eventsSeen++;
        long key = pos.asLong();
        if (PENDING.remove(key) != null) {
            breaks++;
        } else {
            EVENTS.put(key, Clock.ticks() + 1);
        }
    }

    /** Every client tick (END_CLIENT_TICK), after the clock. */
    static void tick() {
        long t = Clock.ticks();
        if (!PENDING.isEmpty()) {
            var it = PENDING.long2ObjectEntrySet().fastIterator();
            List<long[]> due = null;
            while (it.hasNext()) {
                var e = it.next();
                if (t - e.getValue().tick() >= 1) {
                    if (due == null) due = new ArrayList<>();
                    due.add(new long[] {e.getLongKey()});
                }
            }
            if (due != null) {
                for (long[] k : due) {
                    Pending p = PENDING.remove(k[0]);
                    start(BlockPos.of(k[0]), p.state());
                }
            }
        }
        if (!EVENTS.isEmpty()) EVENTS.values().removeIf(v -> {
            boolean old = t - (v - 1) > 1;
            if (old) expiredEvents++;
            return old;
        });
    }

    private static double distSq(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        // the camera, not the player: in a replay the two can be far apart
        return mc.gameRenderer.mainCamera().position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private static void newTick() {
        if (budgetTick != Clock.ticks()) {
            budgetTick = Clock.ticks();
            budgetUsed = 0;
            shedUsed = 0;
            soundsUsed = 0;
        }
    }

    private static void start(BlockPos pos, BlockState state) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        double d2 = distSq(pos);
        if (d2 > RANGE * RANGE) {
            farSkipped++;
            return;
        }
        newTick();
        boolean full = ++budgetUsed <= FULL_BUDGET && d2 <= NEAR_RANGE * NEAR_RANGE;
        if (!full && budgetUsed > FULL_BUDGET + LITE_BUDGET) {
            overBudget++;
            return;
        }
        Pop p = new Pop(pos.immutable(), state, Clock.now(), !full);
        p.whole.init(level, p.pos, state);
        POPS.add(p);
        started++;
        if (firstStartTick < 0) firstStartTick = Clock.ticks();
        lastStartTick = Clock.ticks();
        if (p.lite) lite++;
        if (Config.client().sound && full && soundsUsed < SOUNDS_PER_TICK) {
            soundsUsed++;
            sounds++;
            if (soundsUsed > maxSoundsInTick) maxSoundsInTick = soundsUsed;
            SoundType st = state.getSoundType();
            level.playLocalSound(p.pos, st.getBreakSound(), SoundSource.BLOCKS, (st.getVolume() + 1.0f) / 2.0f * 0.5f,
                st.getPitch() * (0.72f + RANDOM.nextFloat() * 0.16f), false);
        }
    }

    /** A leaf that has just become decaying lets go of one chip: the anticipation without touching the chunk mesh. */
    private static void shed(BlockPos pos, BlockState state) {
        Config.Client c = Config.client();
        Minecraft mc = Minecraft.getInstance();
        if (!c.shed || mc.level == null || liveChips >= MAX_CHIPS) return;
        newTick();
        if (shedUsed >= SHED_PER_TICK || distSq(pos) > FULL_RANGE * FULL_RANGE) return;
        shedUsed++;
        sheds++;
        Chip chip = new Chip(mc.level, pos.getX() + 0.2 + RANDOM.nextDouble() * 0.6, pos.getY() + 0.1, pos.getZ() + 0.2 + RANDOM.nextDouble() * 0.6, state, pos);
        chip.fly((RANDOM.nextDouble() - 0.5) * 0.05, -0.02, (RANDOM.nextDouble() - 0.5) * 0.05);
        chip.scale((float) (c.chipSize * 0.7));
        mc.particleEngine.add(chip);
    }

    /** End of every frame: a new chip budget. */
    public static void frame() {
        frameChips = 0;
    }

    /** The hold before the shrink: none in Calm mode. */
    private static double hold() {
        Config.Client c = Config.client();
        return c.calm ? 0 : c.hold;
    }

    /** {sxz, sy, s} at {@code t} ticks: a short squash during the hold, then it springs back while the leaf shrinks away. */
    static double[] shape(double t) {
        Config.Client c = Config.client();
        double hold = hold(), shrink = Math.max(1, c.shrink);
        double squash = c.squash * Config.get().amplitude();
        if (t < hold) {
            double a = t / Math.max(0.05, hold);
            a = 1 - (1 - a) * (1 - a);
            return new double[] {1 + squash * 0.5 * a, 1 - squash * a, 1};
        }
        double u = Math.min(1, (t - hold) / shrink);
        double back = Math.max(0, 1 - u / 0.35);
        return new double[] {1 + squash * 0.5 * back, 1 - squash * back, 1 - u * u};
    }

    private static int chips(Pop p) {
        Minecraft mc = Minecraft.getInstance();
        Config.Client c = Config.client();
        if (mc.level == null || p.lite || !p.state.shouldSpawnTerrainParticles()) return 0;
        int n = c.calm ? 1 : c.chips;
        switch (mc.options.particles().get()) {
            case MINIMAL -> n = 0;
            case DECREASED -> n /= 2;
            default -> {
            }
        }
        if (distSq(p.pos) > FULL_RANGE * FULL_RANGE) n = Math.min(n, 1);
        n = Math.min(n, MAX_CHIPS - liveChips);
        double speed = 0.2 * c.chipSpeed * Config.get().amplitude();
        double cx = p.pos.getX() + 0.5, cy = p.pos.getY() + 0.5, cz = p.pos.getZ() + 0.5;
        for (int i = 0; i < n; i++) {
            double ox = RANDOM.nextDouble() - 0.5, oy = RANDOM.nextDouble() - 0.5, oz = RANDOM.nextDouble() - 0.5;
            Chip chip = new Chip(mc.level, cx + ox * 0.8, cy + oy * 0.8, cz + oz * 0.8, p.state, p.pos);
            chip.fly(ox * speed * 2, (oy + 0.6) * speed, oz * speed * 2);
            chip.scale((float) (c.chipSize * (0.75 + 0.5 * RANDOM.nextFloat())));
            mc.particleEngine.add(chip);
        }
        chipsMade += n;
        return Math.max(0, n);
    }

    /** In the transient-block pass, every frame. */
    public static void submit(RenderPass pass) {
        long t0 = System.nanoTime();
        try {
            if (POPS.isEmpty()) return;
            framesActive++;
            PoseStack ps = pass.ps;
            Vec3 cam = pass.cam;
            double now = Clock.now(), hold = hold(), shrink = Math.max(1, Config.client().shrink), life = hold + shrink;
            for (int i = POPS.size() - 1; i >= 0; i--) {
                Pop p = POPS.get(i);
                double t = now - p.t0;
                if (t >= life) {
                    if (p.chipped || p.lite) POPS.remove(i);
                    else if (frameChips < CHIPS_PER_FRAME) {
                        // never leave a pop's chips unmade because the frame budget ran out
                        p.chipped = true;
                        frameChips += chips(p);
                        POPS.remove(i);
                    }
                    continue;
                }
                if (t >= hold && !p.chipped && frameChips < CHIPS_PER_FRAME) {
                    p.chipped = true;
                    long c0 = System.nanoTime();
                    frameChips += chips(p);
                    chipNanos += System.nanoTime() - c0;
                }
                double[] sc = shape(t);
                ps.pushPose();
                ps.translate(p.pos.getX() + 0.5 - cam.x, p.pos.getY() - cam.y, p.pos.getZ() + 0.5 - cam.z);
                ps.scale((float) sc[0], (float) sc[1], (float) sc[0]);
                ps.translate(0, 0.5, 0);
                ps.scale((float) sc[2], (float) sc[2], (float) sc[2]);
                ps.translate(-0.5, -0.5, -0.5);
                p.whole.draw(pass);
                ps.popPose();
            }
        } finally {
            spentNanos += System.nanoTime() - t0;
        }
    }
}
