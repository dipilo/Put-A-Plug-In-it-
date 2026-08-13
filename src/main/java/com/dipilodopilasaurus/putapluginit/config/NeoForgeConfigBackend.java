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
package com.dipilodopilasaurus.putapluginit.config;

//? if neoforge {
/*import com.dipilodopilasaurus.putapluginit.Config;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.util.HashMap;
import java.util.Map;

// Not a Javadoc: Stonecutter block-comments this file out on non-neoforge nodes, and a nested
// terminator would close it early. The spec is what Configured/Catalogue build their menu from.
public final class NeoForgeConfigBackend {
    private static final Map<String, Object> HANDLES = new HashMap<>();
    public static final ModConfigSpec SPEC = build();

    private NeoForgeConfigBackend() {
    }

    private static ModConfigSpec build() {
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        Config.defineAll(new ConfigSpecSink() {
            @Override
            public void bool(String path, boolean def, String... comment) {
                HANDLES.put(path, b.comment(comment).define(path, def));
            }

            @Override
            public void integer(String path, int def, int min, int max, String... comment) {
                HANDLES.put(path, b.comment(comment).defineInRange(path, def, min, max));
            }
        });
        return b.build();
    }

    public static void onModConfig(ModConfigEvent event) {
        Config.loadAll(new ConfigView() {
            @Override
            public boolean getBool(String path, boolean def) {
                return ((ModConfigSpec.BooleanValue) HANDLES.get(path)).get();
            }

            @Override
            public int getInt(String path, int def) {
                return ((ModConfigSpec.IntValue) HANDLES.get(path)).get();
            }
        });
    }
}
*///?}
