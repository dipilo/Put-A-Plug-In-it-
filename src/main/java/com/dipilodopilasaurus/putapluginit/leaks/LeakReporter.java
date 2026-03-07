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

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class LeakReporter {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private static final AtomicReference<Snapshot> LAST_SNAPSHOT = new AtomicReference<>();

	private LeakReporter() {
	}

	public static String buildReport() {
		StringBuilder sb = new StringBuilder(2048);

		Runtime rt = Runtime.getRuntime();
		long max = rt.maxMemory();
		long total = rt.totalMemory();
		long free = rt.freeMemory();
		long used = total - free;

		appendHeapHeader(sb, used, total, max);
		appendGcSection(sb);
		appendWatchedSizes(sb);

		MinecraftClientWatch.ClientSnapshot client = MinecraftClientWatch.trySnapshot();
		appendClientModelSection(sb, client);

		Snapshot now = Snapshot.from(used, total, max, client.blockStateToIntMapSize(),
				client.bakedModelMaps().isEmpty() ? null : client.bakedModelMaps().get(0).size());
		Snapshot prev = LAST_SNAPSHOT.getAndSet(now);
		if (prev != null) {
			appendDeltaSection(sb, now, prev);
		}

		return sb.toString();
	}

	private static void appendHeapHeader(StringBuilder sb, long used, long total, long max) {
		sb.append("==== papi leak report ====\n");
		sb.append("time: ").append(LocalDateTime.now()).append('\n');
		sb.append("heap used: ").append(bytesToMiB(used)).append(" MiB").append('\n');
		sb.append("heap total: ").append(bytesToMiB(total)).append(" MiB").append('\n');
		sb.append("heap max: ").append(bytesToMiB(max)).append(" MiB").append('\n');
	}

	private static void appendGcSection(StringBuilder sb) {
		sb.append("\n-- GC --\n");
		List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean gc : gcs) {
			sb.append(gc.getName())
					.append(" count=").append(gc.getCollectionCount())
					.append(" timeMs=").append(gc.getCollectionTime())
					.append('\n');
		}
	}

	private static void appendWatchedSizes(StringBuilder sb) {
		sb.append("\n-- Watched sizes --\n");
		sb.append("SophisticatedCore ItemStackKey.CACHE size: ")
				.append(SophisticatedCoreWatch.tryGetItemStackKeyCacheSize())
				.append('\n');
	}

	private static void appendClientModelSection(StringBuilder sb, MinecraftClientWatch.ClientSnapshot client) {
		if (!client.isClient()) {
			return;
		}

		sb.append("\n-- Client model system --\n");
		if (client.error() != null) {
			sb.append("client snapshot error: ").append(client.error()).append('\n');
			return;
		}

		sb.append("ModelManager BlockState->int map size: ")
				.append(client.blockStateToIntMapSize() == null ? "n/a" : client.blockStateToIntMapSize())
				.append('\n');

		if (client.bakedModelMaps().isEmpty()) {
			sb.append("baked model maps: none detected\n");
			return;
		}

		for (int i = 0; i < client.bakedModelMaps().size(); i++) {
			appendBakedModelMapRow(sb, i + 1, client.bakedModelMaps().get(i));
		}
	}

	private static void appendBakedModelMapRow(StringBuilder sb, int index, MinecraftClientWatch.MapSummary summary) {
		sb.append("baked model map #").append(index)
				.append(": size=").append(summary.size())
				.append(" exampleValue=").append(summary.exampleValueType())
				.append(" multipartCount=");
		if (summary.multipartCount() < 0) {
			sb.append("n/a");
		} else {
			sb.append(summary.multipartCount());
			if (summary.multipartPartial()) {
				sb.append("(partial)");
			}
		}
		sb.append('\n');
	}

	private static void appendDeltaSection(StringBuilder sb, Snapshot now, Snapshot prev) {
		sb.append("\n-- Delta since last report --\n");
		sb.append("heap used delta: ").append(bytesToMiB(now.heapUsed - prev.heapUsed)).append(" MiB\n");
		if (now.blockStateIdMapSize != null && prev.blockStateIdMapSize != null) {
			sb.append("BlockState->int map delta: ").append(now.blockStateIdMapSize - prev.blockStateIdMapSize).append('\n');
		}
		if (now.biggestBakedMapSize != null && prev.biggestBakedMapSize != null) {
			sb.append("largest baked-model map delta: ").append(now.biggestBakedMapSize - prev.biggestBakedMapSize).append('\n');
		}
	}

	public static WriteResult logAndWriteReport(String report) {
		LOGGER.info("\n{}", report);

		try {
			Path outDir = FMLPaths.GAMEDIR.get().resolve("papi-leak-reports");
			Files.createDirectories(outDir);
			Path outFile = outDir.resolve("leak-report-" + TS.format(LocalDateTime.now()) + ".txt");
			Files.writeString(outFile, report, StandardCharsets.UTF_8);
			LOGGER.info("[papi] Leak report written to {}", outFile.toAbsolutePath());
			return WriteResult.success(outFile);
		} catch (IOException e) {
			LOGGER.warn("[papi] Failed to write leak report to disk", e);
			return WriteResult.failure(e.toString());
		}
	}

	public record WriteResult(boolean success, Path path, String error) {
		public static WriteResult success(Path path) {
			return new WriteResult(true, path, null);
		}

		public static WriteResult failure(String error) {
			return new WriteResult(false, null, error);
		}
	}

	private static long bytesToMiB(long bytes) {
		return bytes / (1024L * 1024L);
	}

	private record Snapshot(long heapUsed, long heapTotal, long heapMax,
						Integer blockStateIdMapSize, Integer biggestBakedMapSize) {
		static Snapshot from(long heapUsed, long heapTotal, long heapMax,
							Integer blockStateIdMapSize, Integer biggestBakedMapSize) {
			return new Snapshot(heapUsed, heapTotal, heapMax, blockStateIdMapSize, biggestBakedMapSize);
		}
	}
}
