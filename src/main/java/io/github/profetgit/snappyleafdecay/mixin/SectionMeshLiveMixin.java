package io.github.profetgit.snappyleafdecay.mixin;

//? if >=26.3 || <1.21.2 {
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

// Placeholder: 26.3 signals mesh-live through LevelRenderer.removeTransientBlocksInSection, before 1.21.2 through addRecentlyCompiled* (LevelRendererMixin).
@Mixin(Minecraft.class)
public abstract class SectionMeshLiveMixin {
}
//?} else {
/*import io.github.profetgit.snappyleafdecay.client.Decays;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 1.21.11 and 26.2 have no transient blocks: a section mesh is live when it is swapped in (RenderSection.setSectionMesh), its compile started when its region was made
@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class SectionMeshLiveMixin {
    @Inject(method = "setSectionMesh", at = @At("TAIL"))
    private void snappy_leaf_decay$meshLive(SectionMesh mesh, CallbackInfoReturnable<SectionMesh> cir) {
        SectionRenderDispatcher.RenderSection self = (SectionRenderDispatcher.RenderSection) (Object) this;
        Decays.meshLive(SectionPos.asLong(self.getRenderOrigin()));
    }
}
*///?}
