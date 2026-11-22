package io.github.profetgit.snappyleafdecay.platform.neoforge;

//? neoforge {
/*import io.github.profetgit.snappyleafdecay.SnappyLeafDecay;
import io.github.profetgit.snappyleafdecay.platform.Platform;
import java.nio.file.Path;
import java.util.List;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(SnappyLeafDecay.ID)
public final class NeoForgeEntry implements Platform {
    public NeoForgeEntry() {
        SnappyLeafDecay.init(this);
    }

    @Override
    public String loader() {
        return "neoforge";
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public List<ModInfo> mods() {
        return ModList.get().getMods().stream().map(m -> new ModInfo(m.getModId(), m.getDisplayName())).toList();
    }

}
*///?}
