package io.github.profetgit.snappyleafdecay.mixin;

import io.github.profetgit.snappyleafdecay.server.DecayQueue;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The end of every level tick: the decay queue's turn (Fabric API's END_LEVEL_TICK, without Fabric API). */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void snappy_leaf_decay$tick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        DecayQueue.tick((ServerLevel) (Object) this);
    }
}
