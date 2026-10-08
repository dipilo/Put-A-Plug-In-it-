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
package com.dipilodopilasaurus.putapluginit.modfix;

/**
 * Loader-agnostic dispatcher for the per-mod client-side leak fixes.
 *
 * <p>Derives AllTheLeaks' two loader-specific events from the client tick instead of wiring them per
 * loader: a player identity change from one non-null value to another is its {@code Clone}, a level
 * identity change or disappearance is its client-side {@code LevelEvent.Unload}. Only
 * {@code identityHashCode} ints are kept between ticks — holding the objects would itself leak.
 */
public final class ModFixes {
    private static final String LOCAL_PLAYER = "net.minecraft.client.player.LocalPlayer";
    private static final String CLIENT_LEVEL = "net.minecraft.client.multiplayer.ClientLevel";

    private static int lastPlayerId;
    private static boolean lastPlayerPresent;
    private static int lastLevelId;
    private static boolean lastLevelPresent;

    private ModFixes() {
    }

    public static void onClientTickEnd() {
        Object mc = ModFixReflection.minecraft();
        if (mc == null) {
            return; // dedicated server
        }

        Object player = ModFixReflection.firstFieldOfType(mc, LOCAL_PLAYER);
        Object level = ModFixReflection.firstFieldOfType(mc, CLIENT_LEVEL);

        int playerId = player == null ? 0 : System.identityHashCode(player);
        boolean playerPresent = player != null;
        if (playerPresent && lastPlayerPresent && playerId != lastPlayerId) {
            dispatchClientRespawn(player);
        }

        int levelId = level == null ? 0 : System.identityHashCode(level);
        boolean levelPresent = level != null;
        if (lastLevelPresent && (!levelPresent || levelId != lastLevelId)) {
            dispatchClientLevelUnload();
        }

        lastPlayerId = playerId;
        lastPlayerPresent = playerPresent;
        lastLevelId = levelId;
        lastLevelPresent = levelPresent;
    }

    /**
     * Server-stop cleanup. Unlike the client transitions above this cannot be derived from a tick,
     * so each entrypoint wires it to its own loader event (Fabric {@code ServerLifecycleEvents.SERVER_STOPPED},
     * Forge/NeoForge {@code ServerStoppedEvent}).
     */
    public static void onServerStopped() {
        SupplementariesCacheFix.onServerStopped();
    }

    /**
     * Post-render cleanup for the fixes whose target state lives on the renderer itself. Called from
     * {@code LivingEntityRenderPostMixin} once per living-entity render, so everything downstream
     * resolves its reflection once and does nothing but a field write per call.
     */
    public static void onLivingEntityRenderPost(Object renderer) {
        EmfRenderContextFix.onLivingEntityRenderPost(renderer);
        EtfTextureFix.onLivingEntityRenderPost(renderer);
    }

    private static void dispatchClientRespawn(Object player) {
        EmiHistoryFix.onClientRespawn();
        JadeCacheFix.onClientRespawn();
        JeiCacheFix.onClientWorldChange();
        EtfTextureFix.onClientRespawn(player);
        SophisticatedCoreCraftingUiFix.onClientWorldChange();
    }

    private static void dispatchClientLevelUnload() {
        JadeCacheFix.onClientLevelUnload();
        IcebergCacheFix.onClientLevelUnload();
        JeiCacheFix.onClientWorldChange();
        SophisticatedCoreWrapperFix.onClientLevelUnload();
        SophisticatedCoreCraftingUiFix.onClientWorldChange();
    }
}
