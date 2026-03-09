package com.dipilodopilasaurus.putapluginit.mixin.memoryleakfix;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftTargetClearMixin {
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void papiResetTargetState(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        minecraft.crosshairPickEntity = null;
        minecraft.hitResult = null;
    }
}
