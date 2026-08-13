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
package com.dipilodopilasaurus.putapluginit.command;

import com.dipilodopilasaurus.putapluginit.Config;
import com.dipilodopilasaurus.putapluginit.leaks.HeapDumper;
import com.dipilodopilasaurus.putapluginit.leaks.LeakReporter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;*/
//?}

/**
 * Registers {@code /papi leak report} and, when enabled, {@code /papi leak dumpHeap}.
 *
 * <p>Two API breaks are bridged in {@link #text} and {@link #sendSuccess} rather than gating the
 * command body: {@code sendSuccess} took a bare {@code Component} before 1.20 and a
 * {@code Supplier} from 1.20, and {@code Component.literal} only exists from 1.19.
 */
public final class PapiLeakCommands {
    private PapiLeakCommands() {
    }

    // S125: the //? blocks are Stonecutter version gates, not dead code.
    @SuppressWarnings("java:S125")
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!Config.isLeakCommandsEnabled()) {
            return;
        }
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("papi")
                //? if >=1.21.11 {
                /*.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));*/
                //?} else {
                .requires(source -> source.hasPermission(2));
                //?}

        LiteralArgumentBuilder<CommandSourceStack> leak = Commands.literal("leak")
                .then(Commands.literal("report")
                        .executes(ctx -> {
                            String report = LeakReporter.buildReport();
                            LeakReporter.WriteResult written = LeakReporter.logAndWriteReport(report);
                            if (written.success() && written.path() != null) {
                                sendSuccess(ctx.getSource(),
                                        "[papi] Leak report written: " + written.path().toAbsolutePath());
                            } else {
                                ctx.getSource().sendFailure(text(
                                        "[papi] Leak report generated (logged), but failed to write to disk: " + written.error()));
                            }
                            return 1;
                        }));

        if (Config.isHeapDumpCommandEnabled()) {
            leak.then(Commands.literal("dumpHeap")
                    .executes(ctx -> {
                        HeapDumper.DumpResult result = HeapDumper.tryDumpHeap();
                        if (result.success()) {
                            sendSuccess(ctx.getSource(), "[papi] Heap dump written: " + result.path());
                        } else {
                            ctx.getSource().sendFailure(text(
                                    "[papi] Heap dump failed: " + result.message()));
                        }
                        return result.success() ? 1 : 0;
                    }));
        }

        root.then(leak);
        dispatcher.register(root);
    }

    @SuppressWarnings("java:S125")
    private static Component text(String message) {
        //? if >=1.19 {
        return Component.literal(message);
        //?} else {
        /*return new TextComponent(message);*/
        //?}
    }

    @SuppressWarnings("java:S125")
    private static void sendSuccess(CommandSourceStack source, String message) {
        //? if >=1.20 {
        source.sendSuccess(() -> text(message), false);
        //?} else {
        /*source.sendSuccess(text(message), false);*/
        //?}
    }
}
