package io.github.profetgit.snappyleafdecay.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A chip of the leaf: one solid texel of the leaf's own texture (an opaque one, never a transparent hole), drawn as a small
 * square so it reads as a chunky pixel, with the biome tint of the real block. It falls with real gravity, lives 10-16 ticks
 * and shrinks away over its last ticks, so a chip near the camera never blinks out and nothing hangs in the air.
 */
public final class Chip extends TerrainParticle {
    private static final float FADE_TICKS = 3;
    private boolean counted = true;
    private final float tu, tv;

    public Chip(ClientLevel level, double x, double y, double z, BlockState state, BlockPos pos) {
        super(level, x, y, z, 0, 0, 0, state, pos);
        Decays.liveChips++;
        // TerrainParticle darkens every particle to 60%: the chip should match the leaf it came from
        this.rCol = Math.min(1f, this.rCol / 0.6f);
        this.gCol = Math.min(1f, this.gCol / 0.6f);
        this.bCol = Math.min(1f, this.bCol / 0.6f);
        int w = Math.max(1, this.sprite.contents().width()), h = Math.max(1, this.sprite.contents().height());
        int tx = w / 2, ty = h / 2;
        for (int i = 0; i < 16; i++) {
            int x0 = this.random.nextInt(w), y0 = this.random.nextInt(h);
            if (!this.sprite.contents().isTransparent(0, x0, y0)) {
                tx = x0;
                ty = y0;
                break;
            }
        }
        this.tu = (float) tx / w;
        this.tv = (float) ty / h;
        this.lifetime = 10 + this.random.nextInt(7);
    }

    @Override
    protected float getU0() {
        return this.sprite.getU(tu + 1.0f / Math.max(1, this.sprite.contents().width()) * 0.98f);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(tu + 0.02f / Math.max(1, this.sprite.contents().width()));
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(tv + 0.02f / Math.max(1, this.sprite.contents().height()));
    }

    @Override
    protected float getV1() {
        return this.sprite.getV(tv + 1.0f / Math.max(1, this.sprite.contents().height()) * 0.98f);
    }

    @Override
    public float getQuadSize(float a) {
        float left = this.lifetime - this.age - a;
        return super.getQuadSize(a) * Math.clamp(left / FADE_TICKS, 0f, 1f);
    }

    /** Sets the speed (blocks per tick); the chip then falls like any block chip. */
    public Chip fly(double xd, double yd, double zd) {
        this.setParticleSpeed(xd, yd, zd);
        this.gravity = 1.0f;
        this.friction = 0.92f;
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
