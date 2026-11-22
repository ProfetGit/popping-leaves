package io.github.profetgit.snappyleafdecay;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** What the mod asks the loader. The only class (with the entry points) that names FabricLoader, so a port touches one file. */
public final class Platform {
    private Platform() {}

    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}
