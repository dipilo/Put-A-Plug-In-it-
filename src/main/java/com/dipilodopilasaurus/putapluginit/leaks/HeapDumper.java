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

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class HeapDumper {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	record DumpResult(boolean success, String path, String message) {
	}

	private HeapDumper() {
	}

	static DumpResult tryDumpHeap() {
		try {
			Path outDir = FMLPaths.GAMEDIR.get().resolve("papi-heap-dumps");
			Files.createDirectories(outDir);
			Path outFile = outDir.resolve("heap-" + TS.format(LocalDateTime.now()) + ".hprof");

			// Reflection avoids hard dependency on com.sun.management at compile time.
			Class<?> hsClazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
			Object bean = ManagementFactory.newPlatformMXBeanProxy(
					ManagementFactory.getPlatformMBeanServer(),
					"com.sun.management:type=HotSpotDiagnostic",
					hsClazz);

			hsClazz.getMethod("dumpHeap", String.class, boolean.class)
					.invoke(bean, outFile.toAbsolutePath().toString(), Boolean.TRUE);

			LOGGER.info("[papi] Heap dump written to {}", outFile.toAbsolutePath());
			return new DumpResult(true, outFile.toAbsolutePath().toString(), "ok");
		} catch (Exception t) {
			return new DumpResult(false, "", t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "" : t.getMessage()));
		}
	}
}
