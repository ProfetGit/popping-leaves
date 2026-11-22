package io.github.profetgit.snappyleafdecay.client;

import io.github.profetgit.snappyleafdecay.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/**
 * The settings button in Mod Menu (PRD 5.4): the switches and the strength; the feel numbers stay in the config file
 * (`config/snappy_leaf_decay.json`, live reload). Changes apply at once and are written when the screen closes. Labels are
 * plain English: a Fabric mod's language files are only loaded with Fabric API on the client, which this mod requires,
 * but literals keep the screen working with any resource pack.
 */
public final class SettingsScreen extends OptionsSubScreen {
    public SettingsScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.literal("Snappy Leaf Decay"));
    }

    @Override
    protected void addOptions() {
        list.addSmall(options());
    }

    public static OptionInstance<?>[] options() {
        Config c = Config.get();
        return new OptionInstance<?>[] {
            OptionInstance.createBoolean("Leaf pop", OptionInstance.cachedConstantTooltip(Component.literal("Off: decaying leaves vanish like vanilla.")),
                c.client.enabled, v -> c.client.enabled = v),
            new OptionInstance<>("Intensity", OptionInstance.cachedConstantTooltip(Component.literal("How big the squash and the spray are.")),
                (caption, v) -> Component.literal("Intensity: " + v * 10 + "%"), new OptionInstance.IntRange(5, 15),
                Math.max(5, Math.min(15, Math.round(c.client.intensity / 10f))), v -> c.client.intensity = v * 10),
            OptionInstance.createBoolean("Reduced motion", OptionInstance.cachedConstantTooltip(Component.literal("Smaller movement, no hold, one chip per leaf.")),
                c.client.calm, v -> c.client.calm = v),
            OptionInstance.createBoolean("Rustle", OptionInstance.cachedConstantTooltip(Component.literal("A soft leaf sound for the first few leaves of every tick.")),
                c.client.sound, v -> c.client.sound = v),
            OptionInstance.createBoolean("Shed chip", OptionInstance.cachedConstantTooltip(Component.literal("A leaf lets go of one chip when it starts to decay.")),
                c.client.shed, v -> c.client.shed = v),
            OptionInstance.createBoolean("Fast decay (this world's server)", OptionInstance.cachedConstantTooltip(Component.literal(
                "Singleplayer and LAN hosts only: leaves decay in a ripple after the tree is cut. On a dedicated server this is its own config file.")),
                c.server.enabled, v -> c.server.enabled = v)};
    }

    @Override
    public void removed() {
        Config.save();
        super.removed();
    }
}
