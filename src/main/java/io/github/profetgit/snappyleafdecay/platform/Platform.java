package io.github.profetgit.snappyleafdecay.platform;

import java.nio.file.Path;
import java.util.List;

/** What the mod needs from the mod loader. Each loader's entry class implements it and calls {@link #set} first. */
public interface Platform {
    record ModInfo(String id, String name) {}

    String loader();

    Path configDir();

    boolean isModLoaded(String modId);

    List<ModInfo> mods();

    static Platform get() {
        return Holder.current;
    }

    static void set(Platform platform) {
        Holder.current = platform;
    }

    final class Holder {
        private static volatile Platform current;

        private Holder() {
        }
    }
}
