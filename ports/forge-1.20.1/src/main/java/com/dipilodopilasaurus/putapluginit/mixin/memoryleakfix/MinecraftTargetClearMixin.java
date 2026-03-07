package com.dipilodopilasaurus.putapluginit.mixin.memoryleakfix;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftTargetClearMixin {
    @Shadow(remap = false)
    @Nullable
    public Entity crosshairPickEntity;

    @Shadow(remap = false)
    @Nullable
    public HitResult hitResult;

    @Inject(
            method = "updateScreenAndTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;runTick(Z)V",
                    shift = At.Shift.BEFORE
            ),
            require = 0,
            remap = false
    )
    private void papiResetTargetState(CallbackInfo ci) {
        this.crosshairPickEntity = null;
        this.hitResult = null;
    }
}