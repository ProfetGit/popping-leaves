package io.github.profetgit.snappyleafdecay;

import io.github.profetgit.snappyleafdecay.server.DecayQueue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entry: the config and the server half's tick. */
public final class SnappyLeafDecay implements ModInitializer {
    public static final String ID = "snappy_leaf_decay";
    public static final Logger LOG = LoggerFactory.getLogger("Popping Leaves");

    @Override
    public void onInitialize() {
        Config.load();
        ServerTickEvents.END_LEVEL_TICK.register(DecayQueue::tick);
        ServerTickEvents.END_SERVER_TICK.register(server -> Config.pollReload());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DecayQueue.clear());
        LOG.info("Popping Leaves loaded");
    }
}
