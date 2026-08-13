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
package com.dipilodopilasaurus.putapluginit.modfix;

import com.dipilodopilasaurus.putapluginit.Config;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * EMI: clears {@code EmiHistory} on respawn.
 *
 * <p>Harvested from AllTheLeaks (MIT) {@code leaks/client/mods/emi/UntrackedIssue001}: the recipe
 * history was static state that survived a respawn/dimension change, retaining the referenced stacks.
 * Loader-agnostic via reflection; no-ops when EMI is absent.
 */
final class EmiHistoryFix {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MODID = "emi";
    private static final String EMI_HISTORY_CLASS = "dev.emi.emi.runtime.EmiHistory";

    private static volatile boolean triedInit;
    private static volatile boolean available;
    private static Method clearMethod;

    private EmiHistoryFix() {
    }

    static void onClientRespawn() {
        if (!Config.isEmiClearHistoryOnRespawnEnabled()) {
            return;
        }
        initIfNeeded();
        if (!available) {
            return;
        }
        try {
            clearMethod.invoke(null);
        } catch (ReflectiveOperationException e) {
            available = false;
            LOGGER.warn("[papi] Failed to clear EMI history; disabling fix", e);
        }
    }

    private static void initIfNeeded() {
        if (triedInit) {
            return;
        }
        synchronized (EmiHistoryFix.class) {
            if (triedInit) {
                return;
            }
            triedInit = true;

            if (!ModFixReflection.isModLoaded(MODID)) {
                return;
            }
            try {
                Class<?> historyClass = Class.forName(EMI_HISTORY_CLASS, false, EmiHistoryFix.class.getClassLoader());
                Method method = historyClass.getDeclaredMethod("clear");
                method.setAccessible(true);
                clearMethod = method;
                available = true;
            } catch (ReflectiveOperationException ignored) {
                available = false;
            }
        }
    }
}
