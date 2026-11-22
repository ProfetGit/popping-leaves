package io.github.profetgit.snappyleafdecay.selftest;

import net.fabricmc.loader.api.FabricLoader;

final class Scenarios {
    private Scenarios() {}

    static Script build() {
        Script s = new Script();
        s.run(mc -> TestCompat.createWorld(mc, "snappy_leaf_decaytest-" + System.currentTimeMillis()))
            .until("world to load", 2400, mc -> mc.player != null && mc.level != null && mc.gui.screen() == null)
            .waitTicks(40)
            .check("world loaded", mc -> mc.player.isAlive() ? null : "player not alive");
        String only = System.getProperty("snappy_leaf_decay.selftest.scene", "all");
        if (only.equals("all") || only.contains("smoke")) smoke(s);
        return s;
    }

    private static void smoke(Script s) {
        s.check("mod loaded", mc -> FabricLoader.getInstance().isModLoaded("snappy_leaf_decay") ? null : "snappy_leaf_decay not loaded")
            .check("player alive", mc -> mc.player.isAlive() ? null : "player not alive")
            .capture("smoke", 1)
            .until("smoke frame written", 200, mc -> !SelfTest.capturing())
            .waitTicks(10);
    }
}
