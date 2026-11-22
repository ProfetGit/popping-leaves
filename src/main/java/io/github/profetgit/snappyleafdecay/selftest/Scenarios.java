package io.github.profetgit.snappyleafdecay.selftest;

import io.github.profetgit.snappyleafdecay.client.Decays;
import io.github.profetgit.snappyleafdecay.server.DecayQueue;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;

/**
 * Scenes (select with --var scene=a,b): smoke, oak (the wave, PRD pillar 1), classify (decay vs break, 100%), perf (pillars
 * 2 and 3), loot (pillar 4), stale (a decay never touches what stopped being a decaying leaf).
 */
final class Scenarios {
    static final int Y = 100;

    private Scenarios() {}

    static Script build() {
        Script s = new Script();
        s.run(mc -> TestCompat.createWorld(mc, "snappy_leaf_decaytest-" + System.currentTimeMillis()))
            .until("world to load", 2400, mc -> mc.player != null && mc.level != null && mc.gui.screen() == null)
            .waitTicks(40)
            .check("world loaded", mc -> mc.player.isAlive() ? null : "player not alive");
        String only = System.getProperty("snappy_leaf_decay.selftest.scene", "all");
        if (only.equals("all") || only.contains("smoke")) smoke(s);
        if (only.equals("all") || only.contains("oak")) oak(s);
        if (only.equals("all") || only.contains("classify")) classify(s);
        if (only.equals("all") || only.contains("perf")) perf(s);
        if (only.equals("all") || only.contains("loot")) loot(s);
        if (only.equals("all") || only.contains("stale")) stale(s);
        if (only.equals("all") || only.contains("aside")) aside(s);
        if (only.equals("all") || only.contains("types")) types(s);
        if (only.equals("all") || only.contains("calm")) calm(s);
        if (only.equals("all") || only.contains("close")) close(s);
        return s;
    }

    static void cmd(MinecraftServer s, String c) {
        s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c);
    }

    private static void smoke(Script s) {
        s.check("mod loaded", mc -> FabricLoader.getInstance().isModLoaded("snappy_leaf_decay") ? null : "snappy_leaf_decay not loaded")
            .check("player alive", mc -> mc.player.isAlive() ? null : "player not alive")
            .capture("smoke", 1)
            .until("smoke frame written", 200, mc -> !SelfTest.capturing())
            .waitTicks(10);
    }

    /** A flat stone pad, clear air above it, the player a few blocks back looking at the origin. */
    private static void stage(MinecraftServer srv, int z, int half, int height) {
        cmd(srv, "gamerule send_command_feedback false");
        cmd(srv, "gamerule random_tick_speed 0");
        io.github.profetgit.snappyleafdecay.Config.server().respectRandomTickSpeed = false;
        io.github.profetgit.snappyleafdecay.Config.server().enabled = true;
        cmd(srv, "fill " + (-half - 4) + " " + (Y - 1) + " " + (-half - 24) + " " + (half + 4) + " " + (Y - 1) + " " + (half + 4) + " minecraft:stone");
        cmd(srv, "fill " + (-half - 4) + " " + Y + " " + (-half - 24) + " " + (half + 4) + " " + (Y + height) + " " + (half + 4) + " minecraft:air");
        cmd(srv, "kill @e[type=item]");
        cmd(srv, "gamemode creative @p");
        cmd(srv, "tp @p 0.5 " + Y + " " + z + " 0 -12");
    }

    private static final String LEAF = "minecraft:oak_leaves[persistent=false,distance=1]";

    /** Logs in a lattice ({@code cols} = coordinates on both axes) and a leaf box around them; returns nothing, count with {@link #leaves}. */
    private static void grove(MinecraftServer srv, int[] cols, int half, int y1, int y2) {
        for (int x : cols) for (int z : cols) cmd(srv, "fill " + x + " " + Y + " " + z + " " + x + " " + (Y + y2 + 1) + " " + z + " minecraft:oak_log");
        cmd(srv, "fill " + (-half) + " " + (Y + y1) + " " + (-half) + " " + half + " " + (Y + y2) + " " + half + " " + LEAF + " replace minecraft:air");
    }

    private static void cut(MinecraftServer srv, int[] cols, int y2) {
        for (int x : cols) for (int z : cols) cmd(srv, "fill " + x + " " + Y + " " + z + " " + x + " " + (Y + y2 + 1) + " " + z + " minecraft:air");
    }

    private static int leaves(Minecraft mc, int half, int y1, int y2) {
        int n = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -half; x <= half; x++) for (int y = Y + y1; y <= Y + y2; y++) for (int z = -half; z <= half; z++) {
            if (mc.level.getBlockState(p.set(x, y, z)).getBlock() instanceof LeavesBlock) n++;
        }
        return n;
    }

    private static long cutTick(MinecraftServer srv) {
        return DecayQueue.ticks(srv.overworld());
    }

    /** One oak: the wave from the last log to the last leaf (PRD pillar 1: 1.0-2.0 s = 20-40 ticks, guess). */
    private static void oak(Script s) {
        int[] n = {0}, base = new int[4];
        long[] cutAt = {0}, cutClient = {0};
        int[] cols = {0};
        s.server(srv -> {
            stage(srv, -9, 8, 14);
            grove(srv, cols, 2, 3, 6);
        }).waitTicks(60).run(mc -> n[0] = leaves(mc, 2, 3, 6)).check("oak_stands", mc -> {
            if (n[0] < 80) return "only " + n[0] + " leaves built";
            if (Decays.started != 0) return "pops before the cut: " + Decays.started;
            return null;
        }).run(mc -> {
            base[0] = Decays.started;
            base[1] = Decays.breaks;
            base[2] = Decays.overBudget;
            DecayQueue.resetStats();
            Decays.resetTimes();
            cutClient[0] = io.github.profetgit.snappyleafdecay.client.Clock.ticks();
            if (!TestCompat.hudHidden(mc)) TestCompat.toggleHud(mc);
            SelfTest.startCapture("oak", 200);
        }).server(srv -> {
            cutAt[0] = cutTick(srv);
            cut(srv, cols, 6);
        }).until("oak leaves gone", 400, mc -> leaves(mc, 2, 3, 6) == 0).waitTicks(30).check("oak_wave", mc -> {
            SelfTest.stopCapture();
            if (TestCompat.hudHidden(mc)) TestCompat.toggleHud(mc);
            long first = DecayQueue.firstRemovedTick - cutAt[0], last = DecayQueue.lastRemovedTick - cutAt[0];
            io.github.profetgit.snappyleafdecay.client.SnappyLeafDecayClient.LOG.info(
                "[selftest] oak wave: {} leaves, first removal {} ticks after the cut, last {} (span {}), removed {}, stale {}, busy ticks {}, max per tick {}; "
                    + "client: {} pops, {} lite, {} over budget, {} breaks, {} sheds, {} chips, {} sounds; client ticks from the cut to the first pop {}, to the last {}, fps {}",
                n[0], first, last, last - first, DecayQueue.removedTotal, DecayQueue.staleTotal, DecayQueue.busyTicks, DecayQueue.maxRemovedInTick,
                Decays.started - base[0], Decays.lite, Decays.overBudget - base[2], Decays.breaks - base[1], Decays.sheds, Decays.chipsMade, Decays.sounds,
                Decays.firstStartTick - cutClient[0], Decays.lastStartTick - cutClient[0], mc.getFps());
            if (DecayQueue.removedTotal != n[0]) return "removed " + DecayQueue.removedTotal + " of " + n[0];
            if (last > 40) return "last leaf " + last + " ticks after the cut (> 40)";
            if (last < 20) return "last leaf " + last + " ticks after the cut (< 20, too fast to read)";
            int pops = Decays.started - base[0] + Decays.overBudget - base[2];
            if (pops != n[0]) return "pops " + pops + " of " + n[0] + " decays";
            if (Decays.breaks - base[1] != 0) return "decays seen as breaks: " + (Decays.breaks - base[1]);
            if (Decays.active() != 0 || Decays.pending() != 0) return "pops left over: " + Decays.active() + " active, " + Decays.pending() + " pending";
            return null;
        });
    }

    /** Decay (no event) vs player break (2001) vs a removal of an attached leaf (Timber-like, no event): 200 each. */
    private static void classify(Script s) {
        int[] base = new int[4];
        String dying = "minecraft:oak_leaves[persistent=false,distance=7]";
        s.server(srv -> {
            stage(srv, -9, 8, 24);
            // the queue would remove the decaying leaves itself: this scene tests the client's classification alone
            io.github.profetgit.snappyleafdecay.Config.server().enabled = false;
            cmd(srv, "fill -5 " + (Y + 2) + " -1 4 " + (Y + 6) + " 2 " + dying + " replace minecraft:air");
            cmd(srv, "fill -5 " + (Y + 8) + " -1 4 " + (Y + 12) + " 2 " + dying + " replace minecraft:air");
            cmd(srv, "fill -3 " + (Y + 15) + " 0 -3 " + (Y + 21) + " 0 minecraft:oak_log");
            cmd(srv, "fill 1 " + (Y + 15) + " 0 1 " + (Y + 21) + " 0 minecraft:oak_log");
            cmd(srv, "fill -5 " + (Y + 16) + " -1 4 " + (Y + 20) + " 2 minecraft:oak_leaves[persistent=false,distance=1] replace minecraft:air");
        }).waitTicks(30).run(mc -> {
            base[0] = Decays.started + Decays.overBudget;
            base[1] = Decays.breaks;
        });
        // A: 200 decaying leaves go without an event: all decays (20 per tick)
        for (int i = 0; i < 10; i++) {
            int x = -5 + i;
            s.server(srv -> cmd(srv, "fill " + x + " " + (Y + 2) + " -1 " + x + " " + (Y + 6) + " 2 minecraft:air replace " + "minecraft:oak_leaves")).waitTicks(2);
        }
        s.waitTicks(20).check("classify_decay", mc -> {
            int seen = Decays.started + Decays.overBudget - base[0], breaks = Decays.breaks - base[1];
            if (seen != 200) return "decays seen " + seen + " of 200";
            if (breaks != 0) return "decays seen as breaks: " + breaks;
            return null;
        }).run(mc -> {
            base[0] = Decays.started + Decays.overBudget;
            base[1] = Decays.breaks;
        });
        // B: 200 decaying leaves broken by a player's destroy (event 2001): none is a decay
        for (int i = 0; i < 10; i++) {
            int x = -5 + i;
            s.server(srv -> {
                for (int y = Y + 8; y <= Y + 12; y++) for (int z = -1; z <= 2; z++) cmd(srv, "setblock " + x + " " + y + " " + z + " minecraft:air destroy");
            }).waitTicks(2);
        }
        s.waitTicks(20).check("classify_break", mc -> {
            int seen = Decays.started + Decays.overBudget - base[0], breaks = Decays.breaks - base[1];
            io.github.profetgit.snappyleafdecay.client.SnappyLeafDecayClient.LOG.info("[selftest] classify break: {} breaks, {} seen as decay, events {}, changes {}, expired events {}, pending {}, other leaf losses {} ({})",
                breaks, seen, Decays.eventsSeen, Decays.changesSeen, Decays.expiredEvents, Decays.pending(), Decays.otherLoss, Decays.otherState);
            if (seen != 0) return "breaks seen as decays: " + seen;
            if (breaks != 200) return "breaks seen " + breaks + " of 200";
            return null;
        }).run(mc -> {
            base[0] = Decays.started + Decays.overBudget;
            base[1] = Decays.breaks;
        });
        // C: attached leaves (distance 1..5) removed without an event, the way Timber and Veinminer do: not decays
        s.server(srv -> cmd(srv, "fill -5 " + (Y + 16) + " -1 4 " + (Y + 20) + " 2 minecraft:air replace minecraft:oak_leaves"));
        s.waitTicks(20).check("classify_attached", mc -> {
            int seen = Decays.started + Decays.overBudget - base[0];
            if (seen != 0) return "attached leaves seen as decays: " + seen;
            return null;
        });
    }

    /** About 1,900 leaves: server cost of the queue (pillar 2) and the client's cost per frame with the pops (pillar 3). */
    private static void perf(Script s) {
        int[] cols = {-4, 0, 4};
        int[] n = {0}, base = new int[3];
        long[] cutAt = {0};
        long[] ns = {0};
        s.server(srv -> {
            stage(srv, -20, 10, 20);
            grove(srv, cols, 6, 1, 12);
        }).waitTicks(80).run(mc -> n[0] = leaves(mc, 6, 1, 12)).check("perf_stands", mc -> n[0] < 1800 ? "only " + n[0] + " leaves" : null).run(mc -> {
            base[0] = Decays.started;
            base[1] = Decays.overBudget;
            base[2] = Decays.framesActive;
            ns[0] = Decays.spentNanos;
            DecayQueue.resetStats();
            SelfTest.startCapture("perf", 240);
        }).server(srv -> {
            cutAt[0] = cutTick(srv);
            cut(srv, cols, 12);
        }).until("perf leaves gone", 600, mc -> leaves(mc, 6, 1, 12) == 0).waitTicks(40).check("perf_cost", mc -> {
            SelfTest.stopCapture();
            long last = DecayQueue.lastRemovedTick - cutAt[0];
            int frames = Decays.framesActive - base[2];
            double clientMs = frames == 0 ? 0 : (Decays.spentNanos - ns[0]) / 1e6 / frames;
            double serverMax = DecayQueue.maxTickNanos / 1e6, serverMean = DecayQueue.busyTicks == 0 ? 0 : DecayQueue.sumTickNanos / 1e6 / DecayQueue.busyTicks;
            io.github.profetgit.snappyleafdecay.client.SnappyLeafDecayClient.LOG.info(
                "[selftest] perf: {} leaves, last removal {} ticks after the cut, server worst tick {} ms, mean {} ms over {} busy ticks (max {} per tick); "
                    + "client {} ms per frame over {} frames, {} pops, {} lite, {} over budget, {} chips made, peak chips alive n/a, {} chip ms total",
                n[0], last, String.format("%.2f", serverMax), String.format("%.3f", serverMean), DecayQueue.busyTicks, DecayQueue.maxRemovedInTick,
                String.format("%.3f", clientMs), frames, Decays.started - base[0], Decays.lite, Decays.overBudget - base[1], Decays.chipsMade, Decays.chipNanos / 1_000_000);
            if (DecayQueue.removedTotal != n[0]) return "removed " + DecayQueue.removedTotal + " of " + n[0];
            if (serverMax > 2.0) return String.format("server worst tick %.2f ms (> 2)", serverMax);
            if (serverMean > 1.5) return String.format("server mean %.3f ms (> 1.5)", serverMean);
            if (clientMs > 1.0) return String.format("client %.3f ms per frame (> 1)", clientMs);
            return null;
        });
    }

    /** Loot equals vanilla's: saplings within a binomial band of 5% per oak leaf (pillar 4). */
    private static void loot(Script s) {
        int[] cols = {-3, 0, 3};
        int[] n = {0}, saplings = {0};
        s.server(srv -> {
            stage(srv, -20, 8, 20);
            grove(srv, cols, 4, 1, 12);
        }).waitTicks(80).run(mc -> n[0] = leaves(mc, 4, 1, 12)).server(srv -> {
            cmd(srv, "kill @e[type=item]");
            cut(srv, cols, 12);
        }).until("loot leaves gone", 600, mc -> leaves(mc, 4, 1, 12) == 0).waitTicks(40).server(srv -> {
            int sum = 0;
            for (ItemEntity e : srv.overworld().getEntities(EntityTypes.ITEM, new AABB(-12, Y - 2, -12, 12, Y + 20, 12), x -> true)) {
                if (e.getItem().is(Items.OAK_SAPLING)) sum += e.getItem().getCount();
            }
            saplings[0] = sum;
        }).check("loot_vanilla_rate", mc -> {
            double mean = n[0] * 0.05, sd = Math.sqrt(n[0] * 0.05 * 0.95);
            io.github.profetgit.snappyleafdecay.client.SnappyLeafDecayClient.LOG.info("[selftest] loot: {} leaves, {} saplings (expected {} +- {})", n[0], saplings[0],
                String.format("%.1f", mean), String.format("%.1f", sd));
            if (saplings[0] < mean - 4 * sd || saplings[0] > mean + 4 * sd) return saplings[0] + " saplings from " + n[0] + " leaves";
            return null;
        });
    }

    private static final String[] TYPES = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "azalea"};

    /** Every leaf type, two rows of five small canopies, cut at once: the stills the critic judges (M2). */
    private static void types(Script s) {
        int[] base = new int[3];
        s.server(srv -> {
            stage(srv, -9, 14, 14);
            for (int i = 0; i < TYPES.length; i++) {
                int x = (i % 5) * 4 - 8, z = (i / 5) * 4;
                cmd(srv, "fill " + x + " " + Y + " " + z + " " + x + " " + (Y + 4) + " " + z + " minecraft:oak_log");
                cmd(srv, "fill " + (x - 1) + " " + (Y + 2) + " " + (z - 1) + " " + (x + 1) + " " + (Y + 4) + " " + (z + 1) + " minecraft:" + TYPES[i]
                    + "_leaves[persistent=false,distance=1] replace minecraft:air");
            }
            cmd(srv, "tp @p 0.5 " + (Y + 1) + " -17 0 -4");
        }).waitTicks(60).run(mc -> {
            base[0] = Decays.started;
            base[1] = Decays.chipsMade;
            DecayQueue.resetStats();
            if (!TestCompat.hudHidden(mc)) TestCompat.toggleHud(mc);
            SelfTest.startCapture("types", 170);
        }).server(srv -> {
            for (int i = 0; i < TYPES.length; i++) {
                int x = (i % 5) * 4 - 8, z = (i / 5) * 4;
                cmd(srv, "fill " + x + " " + Y + " " + z + " " + x + " " + (Y + 4) + " " + z + " minecraft:air");
            }
        }).waitTicks(100).check("types_all_pop", mc -> {
            SelfTest.stopCapture();
            if (TestCompat.hudHidden(mc)) TestCompat.toggleHud(mc);
            int pops = Decays.started - base[0], chips = Decays.chipsMade - base[1];
            io.github.profetgit.snappyleafdecay.client.SnappyLeafDecayClient.LOG.info("[selftest] types: {} pops, {} chips, {} leaves removed", pops, chips, DecayQueue.removedTotal);
            if (DecayQueue.removedTotal < TYPES.length * 20) return "only " + DecayQueue.removedTotal + " leaves removed";
            if (pops != DecayQueue.removedTotal) return "pops " + pops + " of " + DecayQueue.removedTotal + " decays";
            if (chips < pops * 2) return "only " + chips + " chips for " + pops + " pops";
            return null;
        });
    }

    /** One canopy from five blocks away: what a single pop looks like (leaf shrinking out, chips leaving). */
    private static void close(Script s) {
        int[] cols = {0};
        s.server(srv -> {
            stage(srv, -9, 8, 14);
            grove(srv, cols, 2, 3, 6);
            cmd(srv, "tp @p 0.5 " + (Y + 2) + " -7 0 -14");
        }).waitTicks(60).run(mc -> {
            if (!TestCompat.hudHidden(mc)) TestCompat.toggleHud(mc);
            SelfTest.startCapture("close", 150);
        }).server(srv -> cut(srv, cols, 6)).waitTicks(120).run(mc -> {
            SelfTest.stopCapture();
            if (TestCompat.hudHidden(mc)) TestCompat.toggleHud(mc);
        });
    }

    /** Calm mode: no hold, one chip per pop, no sheds beyond the same count; sounds never above the per-tick cap. */
    private static void calm(Script s) {
        int[] cols = {0};
        int[] base = new int[3];
        s.run(mc -> io.github.profetgit.snappyleafdecay.Config.client().calm = true).server(srv -> {
            stage(srv, -9, 8, 14);
            grove(srv, cols, 2, 3, 6);
        }).waitTicks(60).run(mc -> {
            base[0] = Decays.started;
            base[1] = Decays.chipsMade;
        }).server(srv -> cut(srv, cols, 6)).until("calm leaves gone", 400, mc -> leaves(mc, 2, 3, 6) == 0).waitTicks(30).check("calm_one_chip", mc -> {
            io.github.profetgit.snappyleafdecay.Config.client().calm = false;
            int pops = Decays.started - base[0], chips = Decays.chipsMade - base[1];
            if (pops < 80) return "only " + pops + " pops";
            if (chips > pops) return chips + " chips for " + pops + " pops in calm mode";
            if (Decays.maxSoundsInTick > Decays.SOUNDS_PER_TICK) return "sounds in one tick " + Decays.maxSoundsInTick;
            return null;
        });
    }

    /** randomTickSpeed 0 and the setting on: the server half stands aside, nothing is queued, the tree stays. */
    private static void aside(Script s) {
        int[] cols = {0};
        int[] n = {0};
        s.server(srv -> {
            stage(srv, -9, 8, 14);
            io.github.profetgit.snappyleafdecay.Config.server().respectRandomTickSpeed = true;
            grove(srv, cols, 2, 3, 6);
        }).waitTicks(40).run(mc -> {
            n[0] = leaves(mc, 2, 3, 6);
            DecayQueue.resetStats();
        }).server(srv -> cut(srv, cols, 6)).waitTicks(100).check("aside_rule", mc -> {
            io.github.profetgit.snappyleafdecay.Config.server().respectRandomTickSpeed = false;
            int left = leaves(mc, 2, 3, 6);
            if (DecayQueue.queuedTotal != 0) return "queued " + DecayQueue.queuedTotal + " with random_tick_speed 0";
            if (left != n[0]) return "leaves left " + left + " of " + n[0];
            return null;
        });
    }

    /** The log comes back two ticks after the cut: leaves that recover are never removed, and a persistent leaf stays. */
    private static void stale(Script s) {
        int[] cols = {0};
        int[] n = {0};
        s.server(srv -> {
            stage(srv, -9, 8, 14);
            grove(srv, cols, 2, 3, 6);
            cmd(srv, "setblock 2 " + (Y + 6) + " 2 minecraft:oak_leaves[persistent=true,distance=1]");
        }).waitTicks(60).run(mc -> {
            n[0] = leaves(mc, 2, 3, 6);
            DecayQueue.resetStats();
        }).server(srv -> cut(srv, cols, 6)).waitTicks(2).server(srv -> cmd(srv, "fill 0 " + Y + " 0 0 " + (Y + 6) + " 0 minecraft:oak_log")).waitTicks(80)
            .check("stale_recovers", mc -> {
                int left = leaves(mc, 2, 3, 6);
                io.github.profetgit.snappyleafdecay.client.SnappyLeafDecayClient.LOG.info("[selftest] stale: {} of {} leaves left, {} removed, {} stale at the due tick",
                    left, n[0], DecayQueue.removedTotal, DecayQueue.staleTotal);
                if (!(mc.level.getBlockState(new BlockPos(2, Y + 6, 2)).getBlock() instanceof LeavesBlock)) return "the persistent leaf is gone";
                if (left < n[0] * 0.8) return "only " + left + " of " + n[0] + " leaves left after the log came back";
                return null;
            });
    }
}
