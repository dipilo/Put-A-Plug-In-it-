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

//? if fabric {
import com.dipilodopilasaurus.putapluginit.command.PapiLeakCommands;
import com.dipilodopilasaurus.putapluginit.config.TomlFileConfig;
import com.dipilodopilasaurus.putapluginit.leaks.ClientWorldLeaveCleanup;
import com.dipilodopilasaurus.putapluginit.modfix.ModFixes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Fabric entrypoint. Wires PAPI's leak-fix hooks to Fabric lifecycle events.
 */
public class PutAPlugInItFabric implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        TomlFileConfig.init();
        TagKeyLeakFix.applyIfNeeded();
        ServerTickEvents.END_SERVER_TICK.register(server -> SophisticatedCoreLeakFix.onServerTickEnd());
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> EntityMemoriesLeakFix.onEntityRemoved(entity));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ModFixes.onServerStopped());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                PapiLeakCommands.register(dispatcher));
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SophisticatedCoreLeakFix.onClientTickEnd();
            TargetEntityLeakFix.onClientTickEnd();
            ImmediatelyFastCompat.onClientTickEnd();
            ClientWorldLeaveCleanup.onClientTickEnd();
            ModFixes.onClientTickEnd();
        });
    }
}
//?}
