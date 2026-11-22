package io.github.profetgit.snappyleafdecay.mixin;

import io.github.profetgit.snappyleafdecay.Config;
import io.github.profetgit.snappyleafdecay.server.DecayQueue;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The server's lifecycle without any loader API: the config poll once a tick, the queues dropped when the server stops. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void snappy_leaf_decay$tick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        Config.pollReload();
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void snappy_leaf_decay$stopped(CallbackInfo ci) {
        DecayQueue.clear();
    }
}
