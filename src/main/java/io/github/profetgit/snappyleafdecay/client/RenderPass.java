package io.github.profetgit.snappyleafdecay.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * What the world draw hook hands to the effects: the pose stack, the camera position and where blocks are submitted.
 * The one adapter between effect code and the version's render path (26.3: submit nodes), so a port touches this file
 * and the mixin that builds it.
 */
public final class RenderPass {
    public final PoseStack ps;
    public final Vec3 cam;
    public final SubmitNodeCollector collector;

    public RenderPass(PoseStack ps, SubmitNodeCollector collector, LevelRenderState state) {
        this.ps = ps;
        this.collector = collector;
        this.cam = state.cameraRenderState.pos;
    }
}
