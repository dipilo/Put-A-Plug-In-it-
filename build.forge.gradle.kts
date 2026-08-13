plugins {
    // ModDevGradle's legacy-Forge variant (MinecraftForge for MC <= 1.20.1).
    id("net.neoforged.moddev.legacyforge") version "2.0.140"
    id("neoforge-mutex")
    id("mixin-remap-strict")
    id("smoke-server")
    id("smoke-client")
}

val smokeRequested: Boolean by extra
val smokeDirectory: File by extra
val smokeClientRequested: Boolean by extra
val smokeClientDirectory: File by extra

// Jar file name is <archivesName>-<version>: papi-forge-<minecraft>-<mod version>. The mod's own
// declared version stays mod.version (processResources reads that, not the project version).
version = "${sc.current.version}-${property("mod.version")}"
base.archivesName = "${property("mod.id") as String}-forge"

val requiredJava = when {
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

val packFormat = when {
    sc.current.parsed >= "1.20" -> 15
    sc.current.parsed >= "1.19.4" -> 13
    sc.current.parsed >= "1.19.3" -> 12
    else -> 9
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/repository/maven-public/") { name = "Sponge" }
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    // Mixin annotation processor: generates the SRG refmap Forge needs at runtime.
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

configurations.configureEach {
    resolutionStrategy {
        // Mixin 0.8.5 asks for Guava 21, and Minecraft's own Guava is not on these configurations
        // for Gradle to prefer over it. ForgeHooks#readTypedPackFormats calls ImmutableMap.Builder
        // #buildOrThrow (Guava 31+), so the dev server dies on NoSuchMethodError while reading pack
        // metadata - before any mixin applies. This is the version 1.18.2 and 1.19.2 both ship.
        force("com.google.guava:guava:31.0.1-jre")
    }
}

// legacyforge moddev mixin support: registers the config and emits the refmap.
mixin {
    add(sourceSets.main.get(), "papi.refmap.json")
    config("papi.mixins.json")
}

legacyForge {
    // e.g. "1.20.1-47.4.16"
    version = "${sc.current.version}-${property("deps.forge")}"

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        // securejarhandler reflects into java.lang.invoke from BootstrapLauncher, before any
        // Minecraft class loads. ModDevGradle supplies the opens on 1.19.2 but not 1.18.2, where
        // every run dies with InaccessibleObjectException and never gets far enough to write a log.
        all {
            jvmArguments.addAll(
                "--add-opens", "java.base/java.lang.invoke=cpw.mods.securejarhandler",
                "--add-opens", "java.base/java.util.jar=cpw.mods.securejarhandler",
                "--add-exports", "java.base/sun.security.util=cpw.mods.securejarhandler",
            )
        }

        register("client") {
            gameDirectory = if (smokeClientRequested) smokeClientDirectory else file("../../run/")
            client()
        }
        register("server") {
            gameDirectory = if (smokeRequested) smokeDirectory else file("../../run/")
            server()
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks {
    // Required for Forge to load the mixin config in production.
    jar {
        manifest.attributes("MixinConfigs" to "papi.mixins.json")
    }

    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }
        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("description", "mod.description")
            register("minecraft", "mod.mc_compat")
            register("loader", "mod.loader_compat")
        }
        filesMatching("META-INF/mods.toml") { expand(props) }
        filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }
        filesMatching("pack.mcmeta") { expand("pack_format" to packFormat) }
        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to build/libs/{mod version}/"
        inputs.property("version", project.property("mod.version"))
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
