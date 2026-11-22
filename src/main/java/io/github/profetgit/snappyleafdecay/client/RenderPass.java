package io.github.profetgit.snappyleafdecay.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.2 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
//?} else {
/*import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
*///?}

/**
 * What the world draw hook hands to the effects: the pose stack, the camera position and where blocks are submitted.
 * The one adapter between effect code and the version's render path (26.x and 1.21.11: submit nodes; before 1.21.2:
 * immediate mode through a buffer source), so a port touches this file, PopRenderState and LevelRendererMixin.
 */
public final class RenderPass {
    public final PoseStack ps;
    public final Vec3 cam;
//? if >=1.21.2 {
    public final SubmitNodeCollector collector;

    public RenderPass(PoseStack ps, SubmitNodeCollector collector, LevelRenderState state) {
        this.ps = ps;
        this.collector = collector;
        this.cam = state.cameraRenderState.pos;
    }

    public void flush() {
    }
//?} else {
/*    public final MultiBufferSource.BufferSource buffers;
    private final Set<RenderType> used = new LinkedHashSet<>();

    public RenderPass(PoseStack ps, MultiBufferSource.BufferSource buffers, Vec3 cam) {
        this.ps = ps;
        this.buffers = buffers;
        this.cam = cam;
    }

    public void used(RenderType type) {
        used.add(type);
    }

    // the copies are drawn in vanilla's own batches: end the ones this pass wrote to
    public void flush() {
        for (RenderType t : used) buffers.endBatch(t);
    }
*///?}
}
