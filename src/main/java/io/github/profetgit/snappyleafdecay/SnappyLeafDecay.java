package io.github.profetgit.snappyleafdecay;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entry. */
public final class SnappyLeafDecay implements ModInitializer {
    public static final String ID = "snappy_leaf_decay";
    public static final Logger LOG = LoggerFactory.getLogger("Snappy Leaf Decay");

    @Override
    public void onInitialize() {
        LOG.info("Snappy Leaf Decay loaded");
    }
}
