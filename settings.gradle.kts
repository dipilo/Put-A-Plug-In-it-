pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.6"
    // Cross-compat loom variant selection for Fabric across old/new Minecraft.
    id("dev.kikugie.loom-back-compat") version "0.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        // Base proof nodes.
        match("1.20.1", "fabric", "forge")
        match("1.21.1", "fabric", "neoforge")
        // Calendar-versioned releases (26.x). Node id != MC id for hotfixes.
        match("26.3", "fabric", "neoforge")
        match("26.2", "fabric", "neoforge")
        match("26.1", "fabric", "neoforge", version = "26.1.2")
        // Latest versions (Phase 3 expansion).
        match("1.21.11", "fabric", "neoforge")
        match("1.21.10", "fabric", "neoforge")
        match("1.21.8", "fabric", "neoforge")
        match("1.21.5", "fabric", "neoforge")
        match("1.21.4", "fabric", "neoforge")
        // Older versions (mojmap-stable APIs; expect no conditionals).
        match("1.20.4", "fabric", "neoforge")
        match("1.19.4", "fabric")
        match("1.19.2", "fabric", "forge")
        match("1.18.2", "forge")
        vcsVersion = "1.21.1-fabric"
    }
}

rootProject.name = "put-a-plug-in-it"
