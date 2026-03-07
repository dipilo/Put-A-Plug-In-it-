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
package com.dipilodopilasaurus.putapluginit.leaks;

import com.dipilodopilasaurus.putapluginit.Config;
import com.dipilodopilasaurus.putapluginit.PutAPlugInIt;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = PutAPlugInIt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientWorldLeaveCleanup {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static boolean lastHadWorld = false;

	private ClientWorldLeaveCleanup() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !isAnyWorldLeaveCleanupEnabled()) {
			return;
		}

		boolean hasWorld = MinecraftClientMitigations.tryHasClientWorld();
		if (lastHadWorld && !hasWorld) {
			runWorldLeaveCleanup();
		}
		lastHadWorld = hasWorld;
	}

	private static boolean isAnyWorldLeaveCleanupEnabled() {
		return Config.isClearModelManagerBlockStateToIdMapOnClientWorldLeaveEnabled()
				|| Config.isClearModelManagerBakedModelMapsOnClientWorldLeaveEnabled();
	}

	private static void runWorldLeaveCleanup() {
		cleanupBlockStateToIdMap();
		cleanupBakedModelMaps();
	}

	private static void cleanupBlockStateToIdMap() {
		if (!Config.isClearModelManagerBlockStateToIdMapOnClientWorldLeaveEnabled()) {
			return;
		}
		MinecraftClientMitigations.ClearResult result = MinecraftClientMitigations.tryClearModelManagerBlockStateToIntMap();
		if (result.isClient() && !result.cleared()) {
			LOGGER.info("[papi] World-leave cleanup (BlockState->id): no-op ({})", result.message());
		}
	}

	private static void cleanupBakedModelMaps() {
		if (!Config.isClearModelManagerBakedModelMapsOnClientWorldLeaveEnabled()) {
			return;
		}
		int minSize = Math.max(0, Config.getClearModelManagerBakedModelMapsMinSize());
		MinecraftClientMitigations.BakedModelMapClearResult result = MinecraftClientMitigations.tryClearLargeBakedModelMaps(minSize);
		if (!result.isClient()) {
			return;
		}
		if (result.clearedMaps() > 0) {
			LOGGER.info("[papi] World-leave cleanup (baked-model maps): cleared {} maps / {} entries (minSize={}).", result.clearedMaps(), result.clearedEntries(), minSize);
			return;
		}
		if (result.message() != null) {
			LOGGER.info("[papi] World-leave cleanup (baked-model maps): no-op ({}).", result.message());
		}
	}
}
