package io.github.profetgit.snappyleafdecay.mixin;

import io.github.profetgit.snappyleafdecay.Config;
import io.github.profetgit.snappyleafdecay.client.Clock;
import io.github.profetgit.snappyleafdecay.client.Decays;
import io.github.profetgit.snappyleafdecay.selftest.SelfTest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "runTick", at = @At("TAIL"))
    private void snappy_leaf_decay$frame(boolean advance, CallbackInfo ci) {
        Decays.frame();
        if (SelfTest.active()) SelfTest.onFrame((Minecraft) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void snappy_leaf_decay$tick(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        Clock.tick(mc);
        Config.pollReload();
        if (mc.level != null) Decays.tick();
        if (SelfTest.active()) SelfTest.onClientTick(mc);
    }

    /** A clean slate on every level change (join, dimension change, disconnect). */
//? if >=1.21.2 {
    @Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
    private void snappy_leaf_decay$levelChange(ClientLevel level, boolean stopSound, CallbackInfo ci) {
        Clock.reset();
        Decays.clear();
    }
//?} else {
/*    @Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;)V", at = @At("HEAD"))
    private void snappy_leaf_decay$levelChange(ClientLevel level, CallbackInfo ci) {
        Clock.reset();
        Decays.clear();
    }
*///?}
}
