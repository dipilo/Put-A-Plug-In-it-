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

import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(Biome.class)
public abstract class BiomeThreadLocalMixin {
	private BiomeThreadLocalMixin() {
	}

	@Unique
	@SuppressWarnings("java:S5164")
	private static ThreadLocal<Long2FloatLinkedOpenHashMap> papiSharedTemperatureCache;

	@SuppressWarnings({"rawtypes", "unchecked", "java:S2696"})
	@Redirect(
			method = "<init>",
			at = @At(
					value = "INVOKE",
					target = "Ljava/lang/ThreadLocal;withInitial(Ljava/util/function/Supplier;)Ljava/lang/ThreadLocal;"
			),
			require = 0
	)
	private ThreadLocal<Long2FloatLinkedOpenHashMap> papiUseSharedThreadLocal(Supplier supplier) {
		if (papiSharedTemperatureCache == null) {
			papiSharedTemperatureCache = ThreadLocal.withInitial(supplier);
		}
		return papiSharedTemperatureCache;
	}
}
