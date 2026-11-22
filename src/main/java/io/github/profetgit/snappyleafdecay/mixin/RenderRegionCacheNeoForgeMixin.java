package io.github.profetgit.snappyleafdecay.mixin;

//? if fabric || >=1.21.2 {
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

// Placeholder: NeoForge's section tasks call createRegion(Level, SectionPos, boolean), which the hook below covers.
@Mixin(Minecraft.class)
public abstract class RenderRegionCacheNeoForgeMixin {
}
//?} else {
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
public abstract class RenderRegionCacheNeoForgeMixin {
    @Inject(method = "createRegion(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/SectionPos;Z)Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;", at = @At("RETURN"))
    private void snappy_leaf_decay$start(Level level, SectionPos section, boolean flag, CallbackInfoReturnable<RenderSectionRegion> cir) {
        Decays.compileStarted(section.asLong());
    }
}
*///?}
