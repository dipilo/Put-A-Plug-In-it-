/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * Derived from AllTheLeaks (MIT) by Uncandango - invalidating SophisticatedCore's
 * StorageWrapperRepository on client level unload. See NOTICE.md.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version. See <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit.modfix;

import com.dipilodopilasaurus.putapluginit.Config;

/**
 * SophisticatedCore: invalidates {@code StorageWrapperRepository} when the client level unloads.
 *
 * <p>Harvested from AllTheLeaks (MIT) {@code leaks/client/mods/sophisticatedcore/UntrackedIssue001},
 * its {@code LevelEvent.Unload} half only — {@code ItemStackKey.CACHE} is
 * {@link com.dipilodopilasaurus.putapluginit.SophisticatedCoreLeakFix}'s.
 *
 * <p>The repository's Guava caches are {@code expireAfterAccess(10, MINUTES)}, but Guava evicts
 * lazily, so with nothing touching the repository the storage wrappers are pinned indefinitely.
 * Upstream clears it on server start/stop, which a client connected to a dedicated server never
 * runs; this closes that hole. Safe because {@code getStorageWrapper} re-instantiates on a miss.
 */
final class SophisticatedCoreWrapperFix {
    private static final String MODID = "sophisticatedcore";
    private static final String REPOSITORY_CLASS =
            "net.p3pp3rf1y.sophisticatedcore.inventory.StorageWrapperRepository";

    private SophisticatedCoreWrapperFix() {
    }

    static void onClientLevelUnload() {
        if (!Config.isSophCoreClearStorageWrappersOnWorldUnloadEnabled() || !ModFixReflection.isModLoaded(MODID)) {
            return;
        }
        // Absent before the repository was introduced, so older SophisticatedCore just no-ops.
        ModFixReflection.invokeStaticNoArg(REPOSITORY_CLASS, "clearCache");
    }
}
