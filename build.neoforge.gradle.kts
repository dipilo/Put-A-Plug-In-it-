plugins {
    id("net.neoforged.moddev") version "2.0.140"
    id("neoforge-mutex")
    id("mixin-remap-strict")
    id("smoke-server")
    id("smoke-client")
}

val smokeRequested: Boolean by extra
val smokeDirectory: File by extra
val smokeClientRequested: Boolean by extra
val smokeClientDirectory: File by extra

// The Breadday flavor: a private, undistributed build carrying pack-specific fixes and the
// WaterMedia 2.x API shim. Opt in with -Ppapi.breadday, and only on the node the pack runs, so no
// sweep and no other node ever sees the extra source set.
val breaddayNode = "1.21.1-neoforge"
val breadday = providers.gradleProperty("papi.breadday").isPresent && sc.current.project == breaddayNode
val BREADDAY_MIXINS = "papi-breadday.mixins.json"

// Jar file name is <archivesName>-<version>: papi-neoforge-<minecraft>-<mod version>. The mod's own
// declared version stays mod.version (processResources reads that, not the project version).
version = "${sc.current.version}-${property("mod.version")}"
base.archivesName = "${property("mod.id") as String}-${if (breadday) "breadday" else "neoforge"}"

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

val packFormat = when {
    sc.current.parsed >= "26.3" -> 97
    sc.current.parsed >= "26.2" -> 85
    sc.current.parsed >= "26.1" -> 84
    sc.current.parsed >= "1.21.11" -> 70
    sc.current.parsed >= "1.21.10" -> 69
    sc.current.parsed >= "1.21.8" -> 64
    sc.current.parsed >= "1.21.5" -> 55
    sc.current.parsed >= "1.21.4" -> 46
    sc.current.parsed >= "1.21.2" -> 42
    sc.current.parsed >= "1.21" -> 34
    sc.current.parsed >= "1.20.5" -> 32
    sc.current.parsed >= "1.20.3" -> 22
    sc.current.parsed >= "1.20" -> 15
    sc.current.parsed >= "1.19.4" -> 13
    sc.current.parsed >= "1.19.3" -> 12
    else -> 9
}

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

val breaddaySources: SourceSet? = if (!breadday) null else sourceSets.create("breadday") {
    java.setSrcDirs(listOf(rootProject.file("src/breadday/java")))
    resources.setSrcDirs(listOf(rootProject.file("src/breadday/resources")))
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

dependencies {
    if (breaddaySources != null) {
        // Everything in libs/ is compile-only: the shim and its mixins link against the copies the
        // pack ships and resolve them from the game class loader, so PAPI bundles none of them and
        // declares no dependency on them. See libs/README.md for what belongs there.
        add(breaddaySources.compileOnlyConfigurationName, rootProject.fileTree("libs") { include("*.jar") })
    }
}

neoForge {
    version = property("deps.neo_loader") as String

    // MDG 2.0.140 pins NFRT 2.0.18, whose Vineflower emits a `HolderSet$1.contents()` with weaker
    // access than the interface method, so recompiling Minecraft's own sources fails on 26.3.
    if (sc.current.parsed >= "26.3") neoFormRuntime { version = "2.0.31" }

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
            if (breaddaySources != null) sourceSet(breaddaySources)
        }
    }

    runs {
        register("client") {
            gameDirectory = if (smokeClientRequested) smokeClientDirectory else file("../../run/")
            client()
        }
        register("server") {
            gameDirectory = if (smokeRequested) smokeDirectory else file("../../run/")
            server()
        }
    }

    // `mods { sourceSet(...) }` only declares jar membership; the Minecraft/NeoForge classpath is a
    // separate opt-in, and without it the Breadday mixins cannot see their targets.
    if (breaddaySources != null) addModdingDependenciesTo(breaddaySources)
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks {
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
        // FancyModLoader only learned META-INF/neoforge.mods.toml in NeoForge 20.5; 20.4 and older
        // read META-INF/mods.toml. Nodes below the rename ship both names so one jar spans it.
        if (sc.current.parsed < "1.20.5") {
            from(rootProject.file("src/main/resources/META-INF/neoforge.mods.toml")) {
                into("META-INF")
                rename { "mods.toml" }
                expand(props)
            }
        }
        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
        filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }
        filesMatching("pack.mcmeta") { expand("pack_format" to packFormat) }
        exclude("fabric.mod.json", "META-INF/mods.toml")

        // Appended rather than templated into the shared toml: every other node reads the same file
        // and must not declare a mixin config its jar does not carry. Declared as an input so
        // flipping the flag re-runs the task instead of leaving the previous flavor's toml in place.
        inputs.property("breadday", breadday)
        if (breadday) doLast {
            val toml = destinationDir.resolve("META-INF/neoforge.mods.toml")
            if (!toml.readText().contains(BREADDAY_MIXINS)) {
                toml.appendText("\n[[mixins]]\nconfig = \"$BREADDAY_MIXINS\"\n")
            }
        }
    }

    if (breaddaySources != null) {
        jar { from(breaddaySources.output) }
        named<Jar>("sourcesJar") { from(breaddaySources.allSource) }
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
