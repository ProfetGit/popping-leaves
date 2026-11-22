package io.github.profetgit.snappyleafdecay.client;

import io.github.profetgit.snappyleafdecay.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entry: the clock, the decay detection tick, and a clean slate on every level change. */
public final class SnappyLeafDecayClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Popping Leaves");

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Clock.tick(mc);
            Config.pollReload();
            if (mc.level != null) Decays.tick();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> reset());
        LOG.info("Popping Leaves client loaded");
    }

    private static void reset() {
        Clock.reset();
        Decays.clear();
    }
}
