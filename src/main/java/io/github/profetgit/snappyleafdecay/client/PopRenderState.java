package io.github.profetgit.snappyleafdecay.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * The copy's view of the world: the block itself, and the real level around it. Vanilla's moving block sees only air
 * around it (no ambient occlusion from neighbours); with the real neighbours the copy is lit like the chunk mesh it
 * replaced. (Same idea as Snappy Blocks' PopRenderState.)
 */
public final class PopRenderState extends MovingBlockRenderState {
    public ClientLevel level;

    public void init(ClientLevel level, BlockPos pos, BlockState state) {
        this.level = level;
        this.blockPos = pos;
        this.randomSeedPos = pos;
        this.blockState = state;
        this.biome = level.getBiome(pos);
        this.cardinalLighting = level.cardinalLighting();
        this.lightEngine = level.getLightEngine();
    }

    /** With the pose at the block's corner. */
    public void draw(RenderPass pass) {
        pass.collector.submitMovingBlock(pass.ps, this, 0);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return pos.equals(this.blockPos) ? this.blockState : level.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinY() {
        return level.getMinY();
    }
}
