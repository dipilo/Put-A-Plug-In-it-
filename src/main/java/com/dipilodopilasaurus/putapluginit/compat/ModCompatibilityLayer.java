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
package com.dipilodopilasaurus.putapluginit.compat;

import com.dipilodopilasaurus.putapluginit.Config;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

public final class ModCompatibilityLayer {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final String IMMEDIATELY_FAST_MODID = "immediatelyfast";
	public static final String MEMORY_LEAK_FIX_MODID = "memoryleakfix";

	private static boolean initialized = false;

	private ModCompatibilityLayer() {
	}

	public static void bootstrap() {
		if (initialized) {
			return;
		}
		initialized = true;

		boolean hasImmediatelyFast = ModList.get().isLoaded(IMMEDIATELY_FAST_MODID);
		boolean hasMemoryLeakFix = ModList.get().isLoaded(MEMORY_LEAK_FIX_MODID);

		if (Config.isCompatibilityLogDetectedModsEnabled()) {
			LOGGER.info("[papi] Compatibility targets detected: immediatelyfast={}, memoryleakfix={}", hasImmediatelyFast, hasMemoryLeakFix);
		}

		if (!Config.isCompatibilityLayerEnabled()) {
			LOGGER.info("[papi] Compatibility layer disabled by config.");
			return;
		}

		if (hasImmediatelyFast) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ImmediatelyFastCompatClient::register);
		}
	}
}
