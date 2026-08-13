# NOTICE — Put A Plug In it! (PAPI)

Copyright (C) 2025–2026 Dipilodopilasaurus and PAPI contributors.

Put A Plug In it! ("PAPI") is licensed under the
**GNU Lesser General Public License, version 3.0 or later (LGPL-3.0-or-later)**.
See [LICENSE](LICENSE) for the full text.

PAPI incorporates, adapts, and interoperates with code and ideas from other open-source memory-optimization and leak-fix mods. This file records the attribution and licensing of that third-party material, as required by their
licenses. Human-readable feature attribution also lives in the [README](README.md); this file is the authoritative legal record.

> Maintainer rule: whenever you bring in code derived from another project, add it here **and** add a per-file header to the affected source files (see [Per-file headers](#per-file-headers) below) **before** committing.

---

## 1. Incorporated / derived code

Code in PAPI that is copied from or directly derived from another project. The original license governs those portions and is preserved.

### MemoryLeakFix — by FX_PR0CESS

- Author: fxmorin (`ca.fxco`)
  - <https://github.com/fxmorin/memoryLeakFix>
- Project: 
  - <https://modrinth.com/mod/memoryleakfix>
  - <https://www.curseforge.com/minecraft/mc-mods/memoryleakfix>
- License: **LGPL-2.1-only**
- **Additional permission required & obtained.** MemoryLeakFix's repository adds: *"I don't allow clients or other mods to merge this mod without permission; if you would like to use this mod in your client or another mod please contact me."* I obtained **explicit written permission from the author, Fx Morin (FX_PR0CESS), by email** to port and merge MemoryLeakFix's code into PAPI. The author's grant (2 Mar 2026): *"Last I checked its using the LGPL-2.1 license, so as long as you give credit. Sure go for it ;)"* — merging is permitted **on the condition that PAPI credits MemoryLeakFix**, which this NOTICE and the per-file headers satisfy.
- Used by PAPI for the following derived features:
  - Biome temperature cache sharing
  - Entity brain-memory cleanup trio
  - TagKey weak intern pool
  - Drowned navigation cleanup
  - Crosshair target / hit-result reset
  - Huge screenshot buffer free-on-failure
  - Texture read-resource buffer free-on-failure (`TextureUtilFreeBufferMixin`, from
    MemoryLeakFix's `readResourcesLeak`)

> **Licensing note (mixed-license combination).** PAPI as a whole is LGPL-3.0-or-later, but MemoryLeakFix is **LGPL-2.1-only**. The files derived from it therefore **cannot be relicensed to LGPLv3**; they remain **LGPL-2.1-only** within this combined work, which LGPL permits. Those files carry an LGPL-2.1-only header rather than the project-default LGPLv3 header. Distribution of the combined work is permitted, and the author additionally granted explicit merge permission (see above).

### AllTheLeaks — by Uncandango

- Author: Uncandango
  - <https://github.com/Uncandango/AllTheLeaks>
- Project: 
  - <https://modrinth.com/mod/all-the-leaks>
  - <https://www.curseforge.com/minecraft/mc-mods/alltheleaks>
- License: **MIT**
- MIT permits incorporation into PAPI's LGPL-3.0-or-later work; the MIT notice is preserved (per-file header + this entry).
- Used by PAPI for the following derived features:
  - `EntityTickList` passive-map clear (release the previous tick's entity backup each tick). 
  PAPI reimplements the technique with a version-stable `TAIL` injection rather than AllTheLeaks' ordinal-based field injection.
  - Per-mod client-side leak fixes under `modfix` (harvested from AllTheLeaks' `leaks/client/mods/*`). 
  PAPI reimplements them loader-agnostically via reflection, and derives the respawn / world-unload triggers from client-player/level identity changes on the client tick rather than subscribing to NeoForge-specific events:
    - EMI
      - clear `EmiHistory` on respawn (`emi/UntrackedIssue001`).
    - Jade
      - clear `ObjectDataCenter` + `JadeClient.hideModName` cache on world change / respawn (`jade/UntrackedIssue001`).
    - Iceberg
      - clear `EntityCollector`'s wrapped-level and entity caches and drop `CustomItemRenderer`'s cached display entities on client world unload (`iceberg/Issue76`, `iceberg/UntrackedIssue001`). 
    PAPI reads the renderer fields reflectively rather than through AllTheLeaks' Mixin accessor.
    - JEI
      - clear `RecipeTransferManager.unsupportedContainers` and drop `GrindstoneRecipeMaker.GRINDSTONE_MENU` on world unload and respawn (`jei/UntrackedIssue001`, `jei/UntrackedIssue004`). 
    Upstream drops the menu on `Clone` and `LoggingOut`; PAPI's level-unload transition stands in for the latter.
    - SophisticatedCore
      - invalidate `StorageWrapperRepository` on client level unload (`sophisticatedcore/UntrackedIssue001`, its `LevelEvent.Unload` half only). 
    The `ItemStackKey.CACHE` half of that upstream class is **not** derived: PAPI's `SophisticatedCoreLeakFix` predates it, clears on the tick rather than on `ScreenEvent.Closing`, and covers the server side.
    - Supplementaries
      - run its static cache-clearing methods on server stop (`supplementaries/UntrackedIssue001`–`003`, merged into one version-tolerant table).
    - Entity Texture Features
      - release the player-texture state held across a respawn and the entity/texture the renderer holds after each render (`entity_texture_features/UntrackedIssue001`, `UntrackedIssue002`). 
    PAPI picks between upstream's two version-gated respawn behaviours on the presence of `ETFPlayerTexture.player` instead of parsing ETF's version, and performs the re-point from the new player alone (a respawn preserves the UUID upstream reads off the old one).
    - Entity Model Features
      - clear the animation iteration context the renderer holds after each render (`entity_model_features/UntrackedIssue001`, `UntrackedIssue002`). Self-gating on the field's presence, so it is inert on EMF 3.0.6+.
  - The post-render hook both of the above need
    (`mixin/alltheleaks/LivingEntityRenderPostMixin`). AllTheLeaks takes this transition
    from NeoForge's `RenderLivingEvent.Post`; PAPI injects at `RETURN` in `LivingEntityRenderer#render` instead, which is loader-agnostic.

---

## 2. Interoperability / compatibility only (no code derived)

PAPI ships compatibility shims that detect or work around these mods. No source is copied; listed for attribution and clarity.

### ImmediatelyFast — by RaphiMC

- Project: <https://modrinth.com/mod/immediatelyfast>
  - <https://github.com/RaphiMC/ImmediatelyFast>
- License: **GNU LGPL-3.0**
- PAPI prevents a known crash between ImmediatelyFast and MemoryLeakFix, and
  mitigates an ImmediatelyFast screen-freeze on some Intel iGPUs/GPUs.

---

## 3. Planned / reference sources

Mods PAPI intends to adapt features from. Listed here in advance so attribution
is not forgotten; move an entry up to section 1 once code actually lands.

| Mod | Author | License | Compatibility with PAPI (LGPL-3.0-or-later) |
|-----|--------|---------|---------------------------------------------|
| ModernFix | embeddedt | LGPL-3.0 (LICENSE text grants "version 3 or, at your option, any later version"; metadata says `GNU LGPL 3.0`) | Clean — same license family |
| FerriteCore | malte0811 | MIT | Clean — MIT folds into LGPLv3 (keep MIT notice) |

(AllTheLeaks was previously listed here; its code is now incorporated — see section 1.)

---

## Per-file headers

Any source file containing third-party-derived code must carry a header naming
the origin and its license.

For **MemoryLeakFix-derived files**, LGPL-2.1 §1 and §2(b) make four elements
mandatory — the LGPL-2.1-**only** statement, attribution (also the stated condition
of the author's grant), a **notice of change including its date**, and the warranty
disclaimer. The canonical form is the header currently on
`src/main/java/com/dipilodopilasaurus/putapluginit/TagKeyLeakFix.java`; copy it and
change only the date line:

```java
 * Changed by dipilo and PAPI contributors on <YYYY-MM-DD>; modified through <year>.
```

Use the file's **actual first-derivation date** (`git log --diff-filter=A`), not a
blanket project date — a §2(b) notice has to be factually correct. The files derived
on 2026-03-07 / 2026-03-08 are listed in the audit below.

These headers are exempt from the project's normal "keep comments short" rule in
[CLAUDE.md](CLAUDE.md); they are legal notices, not commentary.

For **AllTheLeaks-derived files** (MIT), the project-default LGPL-3.0 header plus a
`Derived from AllTheLeaks (MIT) by Uncandango … See NOTICE.md` provenance line is
sufficient — MIT requires only that the notice be preserved. See
`mixin/alltheleaks/EntityTickListPassiveMixin.java`.

### Audit — MemoryLeakFix-derived files (LGPL-2.1-only)

Carrying the LGPL-2.1 header as of 2026-07-19. Both the Stonecutter tree and the
`ports/` copies are covered; `src/` and `ports/` twins share a derivation date.

| File (both `src/…` and `ports/forge-1.15.2-1.16.5/src/…` unless noted) | Derived |
| --- | --- |
| `TagKeyLeakFix.java` | 2026-03-07 |
| `EntityMemoriesLeakFix.java` (`src/` only) | 2026-03-07 |
| `mixin/memoryleakfix/BiomeThreadLocalMixin.java` | 2026-03-07 |
| `mixin/memoryleakfix/MinecraftScreenshotMixin.java` (`src/` only) | 2026-03-07 |
| `mixin/memoryleakfix/TextureUtilFreeBufferMixin.java` | 2026-03-07 |
| `TargetEntityLeakFix.java` | 2026-03-08 |
| `extensions/ExtendDrowned.java` (`ports/` only) | 2026-07-19 |
| `mixin/memoryleakfix/DrownedNavigationMixin.java` (`ports/` only) | 2026-07-19 |
| `mixin/memoryleakfix/ServerWorldNavigationMixin.java` (`ports/` only) | 2026-07-19 |

The former `DrownedNavigationLeakFix.java` (both trees) and `MinecraftTargetClearMixin.java`
(`ports/` only) were deleted on 2026-07-19 — the first is superseded by the two Drowned mixins
above, the second was an empty stub superseded by `TargetEntityLeakFix`.

The `ports/` copy of `MinecraftScreenshotMixin.java` was deleted on 2026-07-20: upstream gates the
fix to `minVersion = "1.17.0"` and `grabHugeScreenshot` does not exist anywhere in 1.15.2-1.16.5, so
it could never bind on that island. The `src/` copy remains and keeps its header.

Files containing only original PAPI code do not need a per-file header; they are
covered by the project-wide LGPL-3.0-or-later in [LICENSE](LICENSE).

---

## Full license texts

Bundling a copy of each third-party license (under `licenses/`) is recommended
once their code is incorporated:

- `licenses/LGPL-2.1.txt` — MemoryLeakFix (LGPL-2.1-only)
- `licenses/MIT-FerriteCore.txt` — FerriteCore (preserve malte0811 copyright line)
- `licenses/MIT-AllTheLeaks.txt` — AllTheLeaks (preserve Uncandango copyright line)
- ModernFix and ImmediatelyFast (LGPL-3.0 family) are covered by PAPI's own
  [LICENSE](LICENSE); preserve their copyright notices where code is adapted.
