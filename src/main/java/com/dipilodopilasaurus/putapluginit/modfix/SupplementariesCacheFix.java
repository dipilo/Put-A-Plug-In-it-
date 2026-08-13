/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * Derived from AllTheLeaks (MIT) by Uncandango - the set of Supplementaries cache
 * clearing methods to run when the server stops. See NOTICE.md.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version. See <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit.modfix;

import com.dipilodopilasaurus.putapluginit.Config;

/**
 * Supplementaries: runs its own static cache-clearing methods when the server stops.
 *
 * <p>Harvested from AllTheLeaks (MIT) {@code leaks/common/mods/supplementaries/UntrackedIssue00{1,2,3}};
 * the caches retain the previous server's objects across a single-player world reopening. Upstream's
 * three version-gated classes collapse into one table here, since a method the installed version does
 * not have is simply skipped - which is also why both {@code WaySignStructure} and its 1.21-3.5.0
 * rename {@code RoadSignStructure} are listed.
 */
final class SupplementariesCacheFix {
    private static final String MODID = "supplementaries";
    private static final String PKG = "net.mehvahdjukaar.supplementaries.common.";

    /** {class, static no-arg method} pairs; absent entries are skipped. */
    private static final String[][] CACHE_CLEARERS = {
            {PKG + "items.crafting.WeatheredMapRecipe", "onWorldUnload"},
            {PKG + "block.tiles.EndermanSkullBlockTile", "clearCache"},
            {PKG + "misc.map_data.ColoredMapHandler", "clearIdCache"},
            {PKG + "worldgen.RoadSignStructure", "clearCache"},
            {PKG + "worldgen.WaySignStructure", "clearCache"},
    };

    private SupplementariesCacheFix() {
    }

    static void onServerStopped() {
        if (!Config.isSupplementariesClearCachesOnServerStopEnabled() || !ModFixReflection.isModLoaded(MODID)) {
            return;
        }
        for (String[] target : CACHE_CLEARERS) {
            ModFixReflection.invokeStaticNoArg(target[0], target[1]);
        }
    }
}
