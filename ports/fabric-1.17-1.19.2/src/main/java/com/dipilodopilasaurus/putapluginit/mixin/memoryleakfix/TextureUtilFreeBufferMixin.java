/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

@Pseudo
@Mixin(targets = {
		"com/mojang/blaze3d/platform/TextureUtil",
		"net/minecraft/client/renderer/texture/TextureUtil"
}, remap = false)
public abstract class TextureUtilFreeBufferMixin {
	private TextureUtilFreeBufferMixin() {
	}

	@Redirect(
			method = "readResource(Ljava/io/InputStream;)Ljava/nio/ByteBuffer;",
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
			method = "readResource(Ljava/io/InputStream;)Ljava/nio/ByteBuffer;",
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
