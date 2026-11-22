package io.github.profetgit.snappyleafdecay;

import io.github.profetgit.snappyleafdecay.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entry, called by each loader's entry class (platform/): the config. Everything else runs from mixins. */
public final class SnappyLeafDecay {
    public static final String ID = "snappy_leaf_decay";
    public static final Logger LOG = LoggerFactory.getLogger("Popping Leaves");

    private static boolean started;

    private SnappyLeafDecay() {}

    public static synchronized void init(Platform platform) {
        Platform.set(platform);
        if (started) return;
        started = true;
        Config.load();
        LOG.info("Popping Leaves loaded on {}", platform.loader());
    }
}
