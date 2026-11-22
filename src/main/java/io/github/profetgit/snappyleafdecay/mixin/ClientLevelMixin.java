package io.github.profetgit.snappyleafdecay.mixin;

import io.github.profetgit.snappyleafdecay.client.Decays;
import java.util.ArrayDeque;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every client block change with the state it replaced. Server updates come through setServerVerifiedBlockState, which
 * calls Level.setBlock directly (not this override); the player's own predictions come through setBlock.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    /** A stack: a change can set more blocks inside it. */
    @Unique
    private final ArrayDeque<BlockState> snappy_leaf_decay$old = new ArrayDeque<>();

    @Inject(method = "setBlock", at = @At("HEAD"))
    private void snappy_leaf_decay$before(BlockPos pos, BlockState state, int flags, int limit, CallbackInfoReturnable<Boolean> cir) {
        snappy_leaf_decay$old.push(((ClientLevel) (Object) this).getBlockState(pos));
    }

    @Inject(method = "setBlock", at = @At("RETURN"))
    private void snappy_leaf_decay$after(BlockPos pos, BlockState state, int flags, int limit, CallbackInfoReturnable<Boolean> cir) {
        BlockState old = snappy_leaf_decay$old.poll();
        if (old != null && cir.getReturnValueZ() && old != state) Decays.blockChanged(pos, old, state);
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("HEAD"))
    private void snappy_leaf_decay$serverBefore(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        snappy_leaf_decay$old.push(((ClientLevel) (Object) this).getBlockState(pos));
    }

    @Inject(method = "setServerVerifiedBlockState", at = @At("TAIL"))
    private void snappy_leaf_decay$serverAfter(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        BlockState old = snappy_leaf_decay$old.poll();
        BlockState now = ((ClientLevel) (Object) this).getBlockState(pos);
        if (old != null && old != now) Decays.blockChanged(pos, old, now);
    }
}
