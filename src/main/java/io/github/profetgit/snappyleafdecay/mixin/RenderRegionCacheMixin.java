package io.github.profetgit.snappyleafdecay.mixin;

//? if >=26.3 {
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

// Placeholder: 26.3 keeps the compile-start clock itself (transient blocks).
@Mixin(Minecraft.class)
public abstract class RenderRegionCacheMixin {
}
//?} else if >=1.21.2 {
/*import io.github.profetgit.snappyleafdecay.client.Decays;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// A new mesh region (render thread) marks the moment a section's compile starts: a mesh compiled after a leaf was removed no longer has it.
@Mixin(RenderRegionCache.class)
public abstract class RenderRegionCacheMixin {
    @Inject(method = "createRegion", at = @At("RETURN"))
    private void snappy_leaf_decay$start(ClientLevel level, long sectionNode, CallbackInfoReturnable<RenderSectionRegion> cir) {
        Decays.compileStarted(sectionNode);
    }
}
*///?} else if >=1.21 {
/*import io.github.profetgit.snappyleafdecay.client.Decays;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderRegionCache.class)
public abstract class RenderRegionCacheMixin {
    @Inject(method = "createRegion", at = @At("RETURN"))
    private void snappy_leaf_decay$start(Level level, SectionPos section, CallbackInfoReturnable<RenderSectionRegion> cir) {
        Decays.compileStarted(section.asLong());
    }
}
*///?} else {
/*import io.github.profetgit.snappyleafdecay.client.Decays;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderRegionCache.class)
public abstract class RenderRegionCacheMixin {
    @Inject(method = "createRegion", at = @At("RETURN"))
    private void snappy_leaf_decay$start(Level level, BlockPos start, BlockPos end, int padding, CallbackInfoReturnable<RenderChunkRegion> cir) {
        Decays.compileStarted(SectionPos.of(start.offset(padding, padding, padding)).asLong());
    }
}
*///?}
