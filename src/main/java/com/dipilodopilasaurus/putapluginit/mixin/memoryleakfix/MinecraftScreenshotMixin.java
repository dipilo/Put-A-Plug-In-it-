/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 *
 * Adapted from MemoryLeakFix by Fx Morin (fxmorin / ca.fxco)
 * https://github.com/fxmorin/memoryLeakFix
 * Original work Copyright (C) Fx Morin. Original license: GNU LGPL-2.1-only.
 *
 * Changed by dipilo and PAPI contributors on 2026-03-07; modified through 2026.
 * Modifications Copyright (C) 2026 dipilo and PAPI contributors.
 *
 * Merged into Put A Plug In it! with the author's explicit written permission,
 * granted on the condition that MemoryLeakFix is credited. See NOTICE.md.
 *
 * This file remains licensed under the GNU Lesser General Public License,
 * version 2.1 ONLY. It is not relicensed to LGPL-3.0 and cannot be, because
 * MemoryLeakFix is LGPL-2.1-only. You may redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1 as
 * published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 2.1 along with this file; see licenses/LGPL-2.1.txt. If not, see
 * <https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html>.
 */
package com.dipilodopilasaurus.putapluginit.mixin.memoryleakfix;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
//? if <1.21.5 {
import net.minecraft.network.chat.Component;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
//?}

/**
 * Unlike the {@code @Pseudo} mixins here this one names {@link Minecraft} directly, so remapping is
 * left on: both {@code grabHugeScreenshot} and {@code GlUtil#allocateMemory} are obfuscated members
 * ({@code method_35699}/{@code m_167903_} and {@code method_35611}/{@code m_166247_}), and writing
 * them verbatim would bind only on mojmap-native NeoForge.
 *
 * <p>1.21.5 removed {@code grabHugeScreenshot} along with {@code GlUtil}, so the body is gated out at
 * build time, not left to the runtime {@code PapiMixinConfigPlugin} check alone - an injector that
 * can never bind still adds its {@code @Unique} field to {@link Minecraft} for nothing. The runtime
 * gate stays as defence for a node whose range ever crosses the boundary; none does today.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftScreenshotMixin {
    //? if <1.21.5 {
    @Unique
    private ByteBuffer papiLastHugeScreenshotBuffer;

    @Redirect(
            method = "grabHugeScreenshot",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/GlUtil;allocateMemory(I)Ljava/nio/ByteBuffer;"
            ),
            require = 0
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
            require = 0
    )
    private void papiFreeHugeScreenshotBufferOnFailure(CallbackInfoReturnable<Component> cir) {
        if (this.papiLastHugeScreenshotBuffer != null) {
            MemoryUtil.memFree(this.papiLastHugeScreenshotBuffer);
            this.papiLastHugeScreenshotBuffer = null;
        }
    }
    //?}
}