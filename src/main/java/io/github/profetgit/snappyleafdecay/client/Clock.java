package io.github.profetgit.snappyleafdecay.client;

import net.minecraft.client.Minecraft;

/**
 * The animation clock (PRD 5.2): our own tick counter plus the partial tick. It stops while the game is paused and while
 * the tick rate is frozen. Never the level's game time (time packets make it jump) or the wall clock.
 */
public final class Clock {
    private static long ticks;
    private static boolean frozen;

    private Clock() {}

    static void tick(Minecraft mc) {
        if (mc.level == null || mc.isPaused()) return;
        frozen = !mc.level.tickRateManager().runsNormally();
        if (!frozen) ticks++;
    }

    static void reset() {
        ticks = 0;
        frozen = false;
    }

    public static long ticks() {
        return ticks;
    }

    /** Ticks plus the partial tick of the frame being drawn. */
    public static double now() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || frozen) return ticks;
        return ticks + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    /** The clock stands still (paused, frozen, no level): nothing may stay hidden or half-played then. */
    public static boolean stopped() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null || mc.isPaused() || frozen;
    }
}
