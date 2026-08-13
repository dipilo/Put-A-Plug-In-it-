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

import com.dipilodopilasaurus.putapluginit.Config;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TomlFileConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path FILE = Paths.get("config", "papi.toml");

    private TomlFileConfig() {
    }

    public static void init() {
        try {
            if (!Files.exists(FILE)) {
                writeDefaults();
            }
        } catch (IOException e) {
            LOGGER.warn("[papi] Could not write default config {}; using built-in defaults", FILE, e);
        }

        Map<String, String> values = read();
        Config.loadAll(new ConfigView() {
            @Override
            public boolean getBool(String path, boolean def) {
                String raw = values.get(path);
                return raw == null ? def : Boolean.parseBoolean(raw.trim());
            }

            @Override
            public int getInt(String path, int def) {
                String raw = values.get(path);
                if (raw == null) {
                    return def;
                }
                try {
                    return Integer.parseInt(raw.trim());
                } catch (NumberFormatException e) {
                    return def;
                }
            }
        });
    }

    private static void writeDefaults() throws IOException {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("# Put A Plug In it! configuration.\n");
        sb.append("# Booleans are true/false; integers are plain numbers. Delete this file to regenerate.\n\n");
        Config.defineAll(new ConfigSpecSink() {
            @Override
            public void bool(String path, boolean def, String... comment) {
                appendEntry(sb, path, Boolean.toString(def), comment);
            }

            @Override
            public void integer(String path, int def, int min, int max, String... comment) {
                String[] withRange = new String[comment.length + 1];
                System.arraycopy(comment, 0, withRange, 0, comment.length);
                withRange[comment.length] = "Range: " + min + " .. " + max;
                appendEntry(sb, path, Integer.toString(def), withRange);
            }
        });

        Path parent = FILE.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(FILE, sb.toString(), StandardCharsets.UTF_8);
        LOGGER.info("[papi] Wrote default config to {}", FILE.toAbsolutePath());
    }

    private static void appendEntry(StringBuilder sb, String path, String value, String[] comment) {
        for (String line : comment) {
            sb.append("# ").append(line).append('\n');
        }
        sb.append(path).append(" = ").append(value).append("\n\n");
    }

    private static Map<String, String> read() {
        Map<String, String> values = new LinkedHashMap<>();
        if (!Files.exists(FILE)) {
            return values;
        }
        try {
            for (String rawLine : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    if (!key.isEmpty()) {
                        values.put(key, line.substring(eq + 1).trim());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("[papi] Could not read config {}; using built-in defaults", FILE, e);
        }
        return values;
    }
}
