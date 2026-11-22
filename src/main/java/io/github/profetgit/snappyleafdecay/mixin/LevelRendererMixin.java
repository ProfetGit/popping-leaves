package io.github.profetgit.snappyleafdecay.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.profetgit.snappyleafdecay.client.Decays;
import io.github.profetgit.snappyleafdecay.client.RenderPass;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.2 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
//?} else if >=1.21 {
/*import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.joml.Matrix4f;
*///?} else {
/*import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.joml.Matrix4f;
*///?}

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
//? if >=26.3 {
    // The hand-off signal: a section mesh whose compile started at this time is now live (Sodium calls it too).
    @Inject(method = "removeTransientBlocksInSection", at = @At("HEAD"))
    private void snappy_leaf_decay$meshLive(long sectionNode, long compileTaskStartTimeNs, CallbackInfo ci) {
        Decays.meshLive(sectionNode, compileTaskStartTimeNs);
    }

    // The copies are drawn with vanilla's transient blocks, after the terrain.
    @Inject(method = "submitTransientBlocks", at = @At("TAIL"))
    private void snappy_leaf_decay$submit(PoseStack ps, SubmitNodeCollector collector, LevelRenderState state, CallbackInfo ci) {
        Decays.submit(new RenderPass(ps, collector, state));
    }
//?}
//? if >=26.2 && <26.3 {
    /*// 26.2 has no transient blocks: the copies go in with the block destroy animation (RETURN: it returns early when nothing is mined)
    @Inject(method = "submitBlockDestroyAnimation", at = @At("RETURN"))
    private void snappy_leaf_decay$submit(PoseStack ps, SubmitNodeCollector collector, LevelRenderState state, CallbackInfo ci) {
        Decays.submit(new RenderPass(ps, collector, state));
    }
*///?}
//? if >=1.21.2 && <26.2 {
    /*// 1.21.11: render states and submit nodes like 26.x, but no transient blocks: the copies go into the submit storage after the block entities
    @Inject(method = "submitBlockEntities", at = @At("RETURN"))
    private void snappy_leaf_decay$submit(PoseStack ps, LevelRenderState state, net.minecraft.client.renderer.SubmitNodeStorage storage, CallbackInfo ci) {
        Decays.submit(new RenderPass(ps, storage, state));
    }
*///?}
//? if >=1.21 && <1.21.2 {
    /*// no transient blocks before 1.21.2: a section mesh is live when its upload is done (setCompiled -> here)
    @Inject(method = "addRecentlyCompiledSection", at = @At("HEAD"))
    private void snappy_leaf_decay$meshLive(SectionRenderDispatcher.RenderSection section, CallbackInfo ci) {
        Decays.meshLive(SectionPos.asLong(section.getOrigin()));
    }

    @Inject(method = "levelEvent", at = @At("HEAD"))
    private void snappy_leaf_decay$event(int type, BlockPos pos, int data, CallbackInfo ci) {
        Decays.levelEvent(type, pos);
    }

    // after the entities, before their buffers are flushed: the copies share vanilla's batches (the pose is the identity here)
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V"))
    private void snappy_leaf_decay$submit(DeltaTracker delta, boolean outline, Camera camera, GameRenderer renderer, LightTexture light,
        Matrix4f view, Matrix4f projection, CallbackInfo ci) {
        RenderPass pass = new RenderPass(new PoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), camera.getPosition());
        Decays.submit(pass);
        pass.flush();
    }
*///?}
//? if <1.21 {
    /*// no transient blocks before 1.21.2: a chunk mesh is live when its upload is done (setCompiled -> here)
    @Inject(method = "addRecentlyCompiledChunk", at = @At("HEAD"))
    private void snappy_leaf_decay$meshLive(ChunkRenderDispatcher.RenderChunk section, CallbackInfo ci) {
        Decays.meshLive(SectionPos.asLong(section.getOrigin()));
    }

    @Inject(method = "levelEvent", at = @At("HEAD"))
    private void snappy_leaf_decay$event(int type, BlockPos pos, int data, CallbackInfo ci) {
        Decays.levelEvent(type, pos);
    }

    // after the entities, before their buffers are flushed: the copies share vanilla's batches (the pose carries the view rotation)
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V"))
    private void snappy_leaf_decay$submit(PoseStack ps, float partial, long finishNanos, boolean outline, Camera camera, GameRenderer renderer, LightTexture light,
        Matrix4f projection, CallbackInfo ci) {
        RenderPass pass = new RenderPass(ps, Minecraft.getInstance().renderBuffers().bufferSource(), camera.getPosition());
        Decays.submit(pass);
        pass.flush();
    }
*///?}
}
