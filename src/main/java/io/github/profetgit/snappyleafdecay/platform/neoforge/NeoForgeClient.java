package io.github.profetgit.snappyleafdecay.platform.neoforge;

//? neoforge {
/*import io.github.profetgit.snappyleafdecay.SnappyLeafDecay;
import io.github.profetgit.snappyleafdecay.client.SettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Client only (a second entry of the same mod): the settings button in NeoForge's mod list.
@Mod(value = SnappyLeafDecay.ID, dist = Dist.CLIENT)
public final class NeoForgeClient {
    public NeoForgeClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new SettingsScreen(parent));
    }
}
*///?}
