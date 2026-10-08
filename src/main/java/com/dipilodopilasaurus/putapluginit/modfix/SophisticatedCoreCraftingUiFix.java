/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * Derived from AllTheLeaks (MIT) by Uncandango - re-registering SophisticatedCore's
 * CraftingTweaks UI part on respawn and client level unload. See NOTICE.md.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version. See <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit.modfix;

import com.dipilodopilasaurus.putapluginit.Config;

/**
 * SophisticatedCore + CraftingTweaks: re-registers {@code CraftingUpgradeTweakUIPart} on respawn
 * and client level unload, replacing the singleton that still holds the old storage screen.
 *
 * <p>Harvested from AllTheLeaks (MIT) {@code leaks/client/mods/sophisticatedcore/UntrackedIssue002}.
 * The UI part is a process-lifetime singleton whose {@code storageScreen} and button list pin a
 * screen, its menu, the player and the level; {@code register()} installs a fresh one.
 */
final class SophisticatedCoreCraftingUiFix {
    private static final String MODID = "sophisticatedcore";
    private static final String CRAFTING_TWEAKS_MODID = "craftingtweaks";
    private static final String UI_PART_CLASS =
            "net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks.CraftingUpgradeTweakUIPart";

    private SophisticatedCoreCraftingUiFix() {
    }

    static void onClientWorldChange() {
        if (!Config.isSophCoreResetCraftingUiOnWorldChangeEnabled()
                || !ModFixReflection.isModLoaded(MODID)
                || !ModFixReflection.isModLoaded(CRAFTING_TWEAKS_MODID)) {
            return;
        }
        // The compat class only ships when SophisticatedCore was built against CraftingTweaks.
        ModFixReflection.invokeStaticNoArg(UI_PART_CLASS, "register");
    }
}
