package com.example.pbrskin.mixin;

import com.example.pbrskin.PbrSkinSettings;
import com.example.pbrskin.PbrSkinTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void pbrskin$useLocalSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        if (!PbrSkinTextures.isActive()) {
            return;
        }
        if ((Object) this != Minecraft.getInstance().player) {
            return;
        }
        PlayerSkin original = cir.getReturnValue();
        ClientAsset.ResourceTexture body =
                new ClientAsset.ResourceTexture(PbrSkinTextures.SKIN_ID, PbrSkinTextures.SKIN_ID);
        cir.setReturnValue(new PlayerSkin(
                body,
                original.cape(),
                original.elytra(),
                PbrSkinSettings.apply(original.model()),
                original.secure()));
    }
}
