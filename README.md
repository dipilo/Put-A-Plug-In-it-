# Put A Plug In it! (PAPI)
PAPI is a Mod that was origionally intended to solve a Memory Leak I found with [Sophisitacted Core](https://www.curseforge.com/minecraft/mc-mods/sophisticated-core) while playing the "[NightfallCraft - The Casket of Reveries](https://www.curseforge.com/minecraft/modpacks/nightfallcraft-the-casket-of-reveries)" Modpack by [P1nero](https://www.curseforge.com/members/p1nero/projects)
Sophisticated Core natively fixed the Memory Leak in their 1.20.1+ Forge & NeoForge versions, but the pre-1.20.1 and Fabric versions of the mod still have the glitch, which PAPI can be used to fix.
PAPI now also aims to find and solve as many Memory Leaks as possible, and integrate features and compatability for and from other mods, to make an all-in-one Memory Leak Fix and Memory Optimization mod, and to increase version and loader support for features of other Memory Leak Fix and Memory Optimization Mods.

<img width="400" height="400" alt="image" src="https://github.com/user-attachments/assets/e544085f-9efb-40b7-b0d4-fc025f7313fa" />

-# Icon and Name by lyftos, inspired by [Plug & Play](https://store.steampowered.com/app/353560/Plug__Play/) by Morio von Rickenbach, and Michael Frei ([Playables](https://store.steampowered.com/developer/Playables))

## Core Features
- Sophisticated Core ItemStackKey leak mitigation
- Prevent known crashes between [RaphiMC](https://modrinth.com/user/RaphiMC)'s [ImmediatelyFast](https://modrinth.com/mod/immediatelyfast) and [FX](https://modrinth.com/user/FX) ([FX_PR0CESS](https://www.curseforge.com/members/fx_pr0cess/projects))'s [Memory Leak Fix](https://modrinth.com/mod/memoryleakfix) ([MemoryLeakFix](https://www.curseforge.com/minecraft/mc-mods/memoryleakfix))
- Prevent Screen-Freeze Glitch caused by [RaphiMC](https://modrinth.com/user/RaphiMC)'s [ImmediatelyFast](https://modrinth.com/mod/immediatelyfast) on Intel IGP's and GPU's

## Features Derrived or Adapted From Other Mods
### [FX](https://modrinth.com/user/FX) ([FX_PR0CESS](https://www.curseforge.com/members/fx_pr0cess/projects))'s [Memory Leak Fix](https://modrinth.com/mod/memoryleakfix) ([MemoryLeakFix](https://www.curseforge.com/minecraft/mc-mods/memoryleakfix))
- Biome Temperature cache sharing
- Entity brain-memory cleanup trio
- TagKey weak intern pool
- Drowned navigation cleanup
- Crosshair target / hit result reset
- Huge screenshot buffer free-on-failure

-# See the [Wiki](https://github.com/dipilo/Put-A-Plug-In-it-/wiki/Features-&-Versions) for more specifics on each feature and the versions/ports they're a part of
