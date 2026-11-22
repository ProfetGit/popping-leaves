package io.github.profetgit.snappyleafdecay.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.snappyleafdecay.client.Decays;
import io.github.profetgit.snappyleafdecay.client.RenderPass;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    /** The hand-off signal: a section mesh whose compile started at this time is now live (Sodium calls it too). */
    @Inject(method = "removeTransientBlocksInSection", at = @At("HEAD"))
    private void snappy_leaf_decay$meshLive(long sectionNode, long compileTaskStartTimeNs, CallbackInfo ci) {
        Decays.meshLive(sectionNode, compileTaskStartTimeNs);
    }

    /** The copies are drawn with vanilla's transient blocks, after the terrain. */
    @Inject(method = "submitTransientBlocks", at = @At("TAIL"))
    private void snappy_leaf_decay$submit(PoseStack ps, SubmitNodeCollector collector, LevelRenderState state, CallbackInfo ci) {
        Decays.submit(new RenderPass(ps, collector, state));
    }
}
