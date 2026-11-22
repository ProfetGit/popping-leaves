package io.github.profetgit.snappyleafdecay.mixin;

import io.github.profetgit.snappyleafdecay.server.DecayQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's {@code tick} is the distance update (scheduled with delay 1 whenever a neighbour changes). After it, a leaf at
 * distance 7 that is not persistent is decaying: the queue takes it. Vanilla's random tick stays as the fallback.
 */
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void snappy_leaf_decay$tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        DecayQueue.leafTicked(level, pos);
    }

    /** A leaf at distance 1 whose neighbour stopped being a log or a leaf: the spot a log was cut, where the ripple starts. */
    @Inject(method = "updateShape", at = @At("HEAD"))
    private void snappy_leaf_decay$cut(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos neighbourPos,
        BlockState neighbour, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
        if (level instanceof ServerLevel server && state.getValue(LeavesBlock.DISTANCE) == 1 && !state.getValue(LeavesBlock.PERSISTENT)
            && !(neighbour.getBlock() instanceof LeavesBlock) && !neighbour.is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY)) {
            DecayQueue.leafLostNeighbour(server, neighbourPos);
        }
    }
}
