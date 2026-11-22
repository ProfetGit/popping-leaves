package io.github.profetgit.snappyleafdecay.mixin;

import io.github.profetgit.snappyleafdecay.client.Decays;
import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Level events as the client plays them: 2001 (block destroyed) tells a break from a decay. */
@Mixin(LevelEventHandler.class)
public abstract class LevelEventHandlerMixin {
    @Inject(method = "levelEvent", at = @At("HEAD"))
    private void snappy_leaf_decay$event(int type, BlockPos pos, int data, CallbackInfo ci) {
        Decays.levelEvent(type, pos);
    }
}
