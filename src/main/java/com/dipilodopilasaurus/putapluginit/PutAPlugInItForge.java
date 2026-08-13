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

//? if forge {
/*import com.dipilodopilasaurus.putapluginit.command.PapiLeakCommands;
import com.dipilodopilasaurus.putapluginit.config.ForgeConfigBackend;
import com.dipilodopilasaurus.putapluginit.leaks.ClientWorldLeaveCleanup;
import com.dipilodopilasaurus.putapluginit.modfix.ModFixes;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
//? if >=1.19 {
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
//?}
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("papi")
public class PutAPlugInItForge {
    public static final String MODID = "papi";

    public PutAPlugInItForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeConfigBackend.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgeConfigBackend::onModConfig);
        TagKeyLeakFix.applyIfNeeded();
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class TickHooks {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            PapiLeakCommands.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                SophisticatedCoreLeakFix.onClientTickEnd();
                TargetEntityLeakFix.onClientTickEnd();
                ImmediatelyFastCompat.onClientTickEnd();
                ClientWorldLeaveCleanup.onClientTickEnd();
                ModFixes.onClientTickEnd();
            }
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                SophisticatedCoreLeakFix.onServerTickEnd();
            }
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            ModFixes.onServerStopped();
        }

        // Forge 40 (1.18.2) still calls this EntityLeaveWorldEvent; renamed in 41 and kept through
        // 45, so the gate covers this node's whole [41,) loader range.
        //? if >=1.19 {
        @SubscribeEvent
        public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
            if (!event.getLevel().isClientSide()) {
                EntityMemoriesLeakFix.onEntityRemoved(event.getEntity());
            }
        }
        //?}
    }
}
*///?}
