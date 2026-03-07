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

import com.mojang.blaze3d.platform.GlUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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
			require = 0
	)
	private ByteBuffer papiCaptureHugeScreenshotBuffer(int bytes) {
		this.papiLastHugeScreenshotBuffer = GlUtil.allocateMemory(bytes);
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
			GlUtil.freeMemory(this.papiLastHugeScreenshotBuffer);
			this.papiLastHugeScreenshotBuffer = null;
		}
	}
}
