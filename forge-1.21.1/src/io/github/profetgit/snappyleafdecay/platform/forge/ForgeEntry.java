package io.github.profetgit.snappyleafdecay.platform.forge;

import io.github.profetgit.snappyleafdecay.SnappyLeafDecay;
import io.github.profetgit.snappyleafdecay.client.SettingsScreen;
import io.github.profetgit.snappyleafdecay.platform.Platform;
import java.nio.file.Path;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("snappy_leaf_decay")
public final class ForgeEntry implements Platform {
    public ForgeEntry() {
        SnappyLeafDecay.init(this);
        if (FMLEnvironment.dist == Dist.CLIENT) registerScreen();
    }

    private static void registerScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new SettingsScreen(parent)));
    }

    @Override
    public String loader() {
        return "forge";
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
