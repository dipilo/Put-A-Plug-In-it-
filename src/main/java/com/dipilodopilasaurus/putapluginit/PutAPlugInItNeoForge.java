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
package com.dipilodopilasaurus.putapluginit;

//? if neoforge {
/*import com.dipilodopilasaurus.putapluginit.command.PapiLeakCommands;
import com.dipilodopilasaurus.putapluginit.config.NeoForgeConfigBackend;
import com.dipilodopilasaurus.putapluginit.leaks.ClientWorldLeaveCleanup;
import com.dipilodopilasaurus.putapluginit.modfix.ModFixes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
//? if >=1.21 {
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
//?} else {
/^import net.neoforged.neoforge.event.TickEvent;^/
//?}

@Mod("papi")
public class PutAPlugInItNeoForge {
    @SuppressWarnings("unused")
    public PutAPlugInItNeoForge(IEventBus modBus, ModContainer modContainer) {
        // FML 12 (NeoForge 26.3) renamed Type.COMMON to LOCAL and SERVER to SYNCED.
        //? if >=26.3 {
        modContainer.registerConfig(ModConfig.Type.LOCAL, NeoForgeConfigBackend.SPEC);
        //?} elif >=1.21 {
        /^modContainer.registerConfig(ModConfig.Type.COMMON, NeoForgeConfigBackend.SPEC);^/
        //?} else {
        /^net.neoforged.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NeoForgeConfigBackend.SPEC);^/
        //?}
        modBus.addListener(NeoForgeConfigBackend::onModConfig);
        TagKeyLeakFix.applyIfNeeded();
        NeoForge.EVENT_BUS.addListener(PutAPlugInItNeoForge::onClientTickEnd);
        NeoForge.EVENT_BUS.addListener(PutAPlugInItNeoForge::onServerTickEnd);
        NeoForge.EVENT_BUS.addListener(PutAPlugInItNeoForge::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(PutAPlugInItNeoForge::onServerStopped);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        PapiLeakCommands.register(event.getDispatcher());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        ModFixes.onServerStopped();
    }

    //? if >=1.21 {
    private static void onClientTickEnd(ClientTickEvent.Post event) {
        SophisticatedCoreLeakFix.onClientTickEnd();
        TargetEntityLeakFix.onClientTickEnd();
        ImmediatelyFastCompat.onClientTickEnd();
        ClientWorldLeaveCleanup.onClientTickEnd();
        ModFixes.onClientTickEnd();
    }

    private static void onServerTickEnd(ServerTickEvent.Post event) {
        SophisticatedCoreLeakFix.onServerTickEnd();
    }
    //?} else {
    /^private static void onClientTickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SophisticatedCoreLeakFix.onClientTickEnd();
            TargetEntityLeakFix.onClientTickEnd();
            ImmediatelyFastCompat.onClientTickEnd();
            ClientWorldLeaveCleanup.onClientTickEnd();
            ModFixes.onClientTickEnd();
        }
    }

    private static void onServerTickEnd(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SophisticatedCoreLeakFix.onServerTickEnd();
        }
    }^/
    //?}
}
*///?}
