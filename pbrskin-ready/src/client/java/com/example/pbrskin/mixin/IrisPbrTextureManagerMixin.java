package com.example.pbrskin.mixin;

import com.example.pbrskin.IrisBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pbr.texture.PBRTextureManager", remap = false)
public abstract class IrisPbrTextureManagerMixin {
    @Inject(method = "getHolder", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void pbrskin$supplyHolder(int id, CallbackInfoReturnable<Object> cir) {
        Object holder = IrisBridge.holderFor(id);
        if (holder != null) {
            cir.setReturnValue(holder);
        }
    }

    @Inject(method = "getOrLoadHolder", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void pbrskin$supplyLoadedHolder(int id, CallbackInfoReturnable<Object> cir) {
        Object holder = IrisBridge.holderFor(id);
        if (holder != null) {
            cir.setReturnValue(holder);
        }
    }
}
