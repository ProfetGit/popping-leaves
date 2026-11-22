package io.github.profetgit.snappyleafdecay.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A chunky chip cut from the leaf's own texture: a vanilla terrain particle (so the biome tint and the solid texels come
 * with it) that drifts down like a leaf and shrinks away over its last ticks, so a 2x chip near the camera never blinks out.
 */
public final class Chip extends TerrainParticle {
    private static final float FADE_TICKS = 4;
    private boolean counted = true;

    public Chip(ClientLevel level, double x, double y, double z, BlockState state, BlockPos pos) {
        super(level, x, y, z, 0, 0, 0, state, pos);
        Decays.liveChips++;
    }

    @Override
    public float getQuadSize(float a) {
        float left = this.lifetime - this.age - a;
        return super.getQuadSize(a) * Math.clamp(left / FADE_TICKS, 0f, 1f);
    }

    /** Leaves drift: slow fall, strong air drag. */
    public Chip fly(double xd, double yd, double zd) {
        this.setParticleSpeed(xd * 0.7, yd * 0.7 * 0.8, zd * 0.7);
        this.gravity = 0.18f;
        this.friction = 0.9f;
        return this;
    }

    @Override
    public void remove() {
        if (counted) {
            counted = false;
            Decays.liveChips--;
        }
        super.remove();
    }
}
