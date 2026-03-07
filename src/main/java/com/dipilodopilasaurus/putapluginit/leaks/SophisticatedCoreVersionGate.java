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

import java.util.Locale;

final class SophisticatedCoreVersionGate {
	private static final String GLOBAL_FIXED_VERSION = "1.4.6.1504";

    private SophisticatedCoreVersionGate() {
    }

    static boolean shouldApplyItemStackKeyFix(String version) {
        ParsedVersion parsed = parseVersion(version);
        if (parsed == null) {
            return true;
        }
        if (compareDotVersion(parsed.scVersion(), GLOBAL_FIXED_VERSION) >= 0) {
            return false;
        }

        String mcThreshold = getMcSpecificFixedVersion(parsed.mcVersion());
        return mcThreshold == null || compareDotVersion(parsed.scVersion(), mcThreshold) < 0;
    }

	private static ParsedVersion parseVersion(String version) {
		if (version == null || version.isBlank()) {
			return null;
		}
		String normalized = version.trim().toLowerCase(Locale.ROOT);
		String[] parts = normalized.split("-", 2);
		if (parts.length != 2) {
			return null;
		}
		return new ParsedVersion(parts[0], parts[1]);
	}

	private static String getMcSpecificFixedVersion(String mcVersion) {
		return switch (mcVersion) {
			case "1.20.1" -> "1.3.5.1505";
			case "1.21.1" -> "1.4.5.1499";
			case "1.21.4" -> "1.4.5.1500";
			case "1.21.5" -> "1.4.5.1501";
			case "1.21.8" -> "1.4.5.1502";
			case "1.21.10" -> "1.4.6.1503";
            case "1.21.11" -> GLOBAL_FIXED_VERSION;
			default -> null;
		};
	}

    private static int compareDotVersion(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        int len = Math.max(l.length, r.length);
        for (int i = 0; i < len; i++) {
            int li = i < l.length ? parsePart(l[i]) : 0;
            int ri = i < r.length ? parsePart(r[i]) : 0;
            if (li != ri) {
                return Integer.compare(li, ri);
            }
        }
        return 0;
    }

    private static int parsePart(String value) {
        try {
            return Integer.parseInt(value.replaceAll("\\D.*$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

	private record ParsedVersion(String mcVersion, String scVersion) {
	}
}
