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

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * {@code remap = false} is forced by the {@code @Pseudo} string targets, so the annotation processor
 * never writes a refmap entry for this class and the method name has to be supplied per environment:
 * the mojmap name binds in dev, the SRG name in production. Upstream solves the same problem with
 * its own {@code @Remap(mcp = "func_225684_a_", excludeDev = true)} machinery, which PAPI does not
 * carry. {@code func_225684_a_} is the SRG name across all of 1.15.2-1.16.5, so one string covers the
 * island; whichever name is absent simply does not match, which {@code require = 0} permits.
 */
@Pseudo
@Mixin(targets = {
        "com/mojang/blaze3d/platform/TextureUtil",
        "net/minecraft/client/renderer/texture/TextureUtil"
}, remap = false)
public abstract class TextureUtilFreeBufferMixin {
    private static final String READ_RESOURCE_DEV = "readResource(Ljava/io/InputStream;)Ljava/nio/ByteBuffer;";
    private static final String READ_RESOURCE_SRG = "func_225684_a_(Ljava/io/InputStream;)Ljava/nio/ByteBuffer;";

    private TextureUtilFreeBufferMixin() {
    }

    @Redirect(
            method = {READ_RESOURCE_DEV, READ_RESOURCE_SRG},
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/nio/channels/ReadableByteChannel;read(Ljava/nio/ByteBuffer;)I"
            ),
            require = 0
    )
    private static int papiReadResourceWithoutLeak(ReadableByteChannel channel, ByteBuffer byteBuf) throws java.io.IOException {
        try {
            return channel.read(byteBuf);
        } catch (Exception e) {
            MemoryUtil.memFree(byteBuf);
            throw e;
        }
    }

    @Redirect(
            method = {READ_RESOURCE_DEV, READ_RESOURCE_SRG},
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/nio/channels/FileChannel;read(Ljava/nio/ByteBuffer;)I"
            ),
            require = 0
    )
    private static int papiReadFileChannelWithoutLeak(FileChannel channel, ByteBuffer byteBuf) throws java.io.IOException {
        try {
            return channel.read(byteBuf);
        } catch (Exception e) {
            MemoryUtil.memFree(byteBuf);
            throw e;
        }
    }
}