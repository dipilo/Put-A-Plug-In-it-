package com.dipilodopilasaurus.putapluginit.mixin.memoryleakfix;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

@Mixin(Minecraft.class)
public abstract class MinecraftScreenshotMixin {
    @Unique
    private ByteBuffer papiLastHugeScreenshotBuffer;

    @Redirect(
            method = "grabHugeScreenshot",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlUtil;allocateMemory(I)Ljava/nio/ByteBuffer;"
            ),
            require = 0,
            remap = false
    )
    private ByteBuffer papiCaptureHugeScreenshotBuffer(int bytes) {
        this.papiLastHugeScreenshotBuffer = MemoryUtil.memAlloc(bytes);
        return this.papiLastHugeScreenshotBuffer;
    }

    @Inject(
            method = "grabHugeScreenshot",
            at = @At(
                    value = "CONSTANT",
                    args = "stringValue=screenshot.failure"
            ),
            require = 0,
            remap = false
    )
    private void papiFreeHugeScreenshotBufferOnFailure(CallbackInfoReturnable<ITextComponent> cir) {
        if (this.papiLastHugeScreenshotBuffer != null) {
            MemoryUtil.memFree(this.papiLastHugeScreenshotBuffer);
            this.papiLastHugeScreenshotBuffer = null;
        }
    }
}