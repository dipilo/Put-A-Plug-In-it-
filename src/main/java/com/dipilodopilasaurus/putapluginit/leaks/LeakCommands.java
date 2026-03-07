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
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = PutAPlugInIt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LeakCommands {
	private LeakCommands() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		if (!Config.isLeakCommandsEnabled()) {
			return;
		}

		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(PutAPlugInIt.MODID)
				.requires(source -> source.hasPermission(2));

		root.then(Commands.literal("leak")
				.then(Commands.literal("report")
						.executes(ctx -> {
							String report = LeakReporter.buildReport();
							LeakReporter.WriteResult written = LeakReporter.logAndWriteReport(report);
							if (written.success() && written.path() != null) {
								Path outFile = written.path().toAbsolutePath();
								Path outDir = outFile.getParent();

								Component fileLink = Component.literal(outFile.getFileName().toString())
										.withStyle(style -> style
												.withColor(ChatFormatting.AQUA)
												.withUnderlined(true)
												.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outFile.toString()))
												.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Open report file"))));

								Component folderLink = Component.literal("(folder)")
										.withStyle(style -> style
												.withColor(ChatFormatting.AQUA)
												.withUnderlined(true)
												.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, outDir.toString()))
												.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Open report folder"))));

								Component msg = Component.literal("[papi] Leak report written: ")
										.withStyle(ChatFormatting.GREEN)
										.append(fileLink)
										.append(Component.literal(" "))
										.append(folderLink);
								ctx.getSource().sendSuccess(() -> msg, false);
							} else {
								ctx.getSource().sendFailure(Component.literal("[papi] Leak report generated (logged), but failed to write to disk: " + written.error()));
							}
							return 1;
						}))
				.then(Commands.literal("dumpHeap")
						.requires(source -> Config.isHeapDumpCommandEnabled())
						.executes(ctx -> {
							HeapDumper.DumpResult result = HeapDumper.tryDumpHeap();
							if (result.success()) {
								ctx.getSource().sendSuccess(() -> Component.literal("[papi] Heap dump written: " + result.path())
										.withStyle(ChatFormatting.GREEN), false);
							} else {
								ctx.getSource().sendFailure(Component.literal("[papi] Heap dump failed: " + result.message()));
							}
							return result.success() ? 1 : 0;
						})));

		dispatcher.register(root);
	}
}
