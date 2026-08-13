plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1-fabric"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Apply version- and loader-specific properties from stonecutter.properties.toml
    properties {
        tags(version, loader)
    }

    // Constants for Stonecutter comments (e.g. //? if forge {...})
    constants {
        match(loader, "fabric", "neoforge", "forge")
    }

    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["mod_id"] = "\"${properties.get<String>("mod.id")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = properties.get<String>("mod.id") != "template"
    dependencies["fapi"] = properties.getOrNull<String>("deps.fabric_api") ?: "0"

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}

// Sweep tasks. Without these every node has to be named individually on the command line, and a
// range change that only breaks one node in the middle is easy to miss.
//
// Stonecutter 0.9 removed the `chiseled` task type: `tasks.named(x)` now only hands back a
// node -> TaskProvider map, so the aggregate task is ours to register.
//
// Nodes still run concurrently under org.gradle.parallel - tasks.order() did not serialize them in
// practice, so anything a sweep touches that is machine-global (ports, the shared run/ directory)
// has to be made per-node rather than assumed exclusive. See buildSrc/.../smoke-server.gradle.kts.
fun sweep(name: String, nodeTask: String, summary: String) {
    tasks.register(name) {
        group = "project"
        description = summary
        dependsOn(stonecutter.tasks.named(nodeTask).map { it.values })
    }
}

sweep("chiseledCompile", "compileJava", "Compiles every node. Cheapest full-tree check for Stonecutter/API breakage.")
sweep("chiseledBuild", "build", "Builds every node's jar.")
sweep(
    "chiseledSmokeServer",
    "smokeServer",
    "Boots a dedicated server on every node and stops it. Catches require=1 mixin failures that compiling cannot.",
)
