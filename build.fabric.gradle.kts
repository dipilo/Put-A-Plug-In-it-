plugins {
    // Applies the correct loom variant based on the Minecraft version.
    id("dev.kikugie.loom-back-compat")
    id("mixin-remap-strict")
    id("smoke-server")
    id("smoke-client")
}

val smokeRequested: Boolean by extra
val smokeDirectory: File by extra
val smokeClientRequested: Boolean by extra
val smokeClientDirectory: File by extra
val smokeWithMods = providers.gradleProperty("papi.smokeWithMods").isPresent

// DO NOT set group = ...! (loom-back-compat manages it)
// Jar file name is <archivesName>-<version>: papi-fabric-<minecraft>-<mod version>. The mod's own
// declared version stays mod.version (processResources reads that, not the project version).
version = "${sc.current.version}-${property("mod.version")}"
base.archivesName = "${property("mod.id") as String}-fabric"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

val packFormat = when {
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

dependencies {
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    // Mods-present smoke on 1.21.2+ needs a newer Fabric Loader than PAPI pins: current mods (Smooth
    // Boot, recent Clumps) require >=0.18, and the dev run must satisfy them or Fabric Loader aborts
    // before PAPI is even exercised. Gated to 1.21.2+: older nodes pass on their pinned loader, and
    // 0.18.x there only surfaces old third-party server bugs (Supplementaries 1.19.2 client-config).
    // Normal builds always keep deps.fabric_loader.
    val fabricLoaderVersion = if (smokeWithMods && sc.current.parsed >= "1.21.2") "0.18.4" else property("deps.fabric_loader")
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    if (smokeWithMods) {
        // Mods-present smoke: give the dropped-in mods the whole Fabric API rather than PAPI's two
        // modules, so their API deps resolve. fetch-smoke-mods skips the FAPI jar for this reason -
        // a second copy in mods/ would duplicate its nested modules and crash Fabric Loader.
        modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
    } else {
        // Only the modules PAPI uses: lifecycle (tick) events + command registration.
        fapi("fabric-lifecycle-events-v1", "fabric-command-api-v2")
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        // runConfigs.all covers client and server alike, but only one sweep is ever in flight, so
        // whichever one was asked for takes the redirect.
        runDirectory = when {
            smokeClientRequested -> smokeClientDirectory
            smokeRequested -> smokeDirectory
            else -> rootProject.file("run")
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
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
        }
        filesMatching("fabric.mod.json") { expand(props) }
        filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }
        filesMatching("pack.mcmeta") { expand("pack_format" to packFormat) }
        exclude("META-INF/neoforge.mods.toml", "META-INF/mods.toml")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to build/libs/{mod version}/"
        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
