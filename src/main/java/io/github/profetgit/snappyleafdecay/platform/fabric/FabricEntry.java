package io.github.profetgit.snappyleafdecay.platform.fabric;

//? fabric {
import io.github.profetgit.snappyleafdecay.SnappyLeafDecay;
import io.github.profetgit.snappyleafdecay.platform.Platform;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric and Quilt (Quilt loads the Fabric jar). */
public final class FabricEntry implements ModInitializer, Platform {
    @Override
    public void onInitialize() {
        SnappyLeafDecay.init(this);
    }

    @Override
    public String loader() {
        return FabricLoader.getInstance().isModLoaded("quilt_loader") ? "quilt" : "fabric";
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public List<ModInfo> mods() {
        return FabricLoader.getInstance().getAllMods().stream()
            .map(m -> new ModInfo(m.getMetadata().getId(), m.getMetadata().getName())).toList();
    }

}
//?}
