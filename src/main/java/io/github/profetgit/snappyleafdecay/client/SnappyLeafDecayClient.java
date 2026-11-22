package io.github.profetgit.snappyleafdecay.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entry. */
public final class SnappyLeafDecayClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Snappy Leaf Decay");

    @Override
    public void onInitializeClient() {
        LOG.info("Snappy Leaf Decay client loaded");
    }
}
