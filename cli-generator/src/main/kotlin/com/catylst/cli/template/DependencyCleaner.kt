package com.catylst.cli.template

import com.catylst.cli.model.FeatureDef
import com.catylst.cli.model.GeneratorConfig
import java.io.File

object DependencyCleaner {

    fun clean(projectDir: File, config: GeneratorConfig, manifestFeatures: List<FeatureDef>) {
        val unselected = manifestFeatures.filter { it.id !in config.features }

        val allVersionKeys = unselected.flatMap { it.tomlVersionKeys }.toSet()
        val allPluginKeys = unselected.flatMap { it.tomlPluginKeys }.toSet()
        val allGradleDeps = unselected.flatMap { it.gradleDeps }.toSet()

        val tomlFile = File(projectDir, "gradle/libs.versions.toml")
        if (tomlFile.exists()) {
            cleanToml(tomlFile, allVersionKeys, allPluginKeys, allGradleDeps)
        }
    }

    private fun cleanToml(
        tomlFile: File,
        versionKeys: Set<String>,
        pluginKeys: Set<String>,
        gradleDeps: Set<String>
    ) {
        var content = tomlFile.readText()
        val lines = content.lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            val trimmed = lines[i].trim()

            // Remove version entries: key = "x.y.z" or key = { ... }
            for (key in versionKeys) {
                val keyPattern = Regex("""^${Regex.escape(key)}\s*[=]""")
                if (keyPattern.containsMatchIn(trimmed)) {
                    toRemove.add(i)
                }
            }

            // Remove plugin entries: key = { id = "...", ... }
            for (key in pluginKeys) {
                val keyPattern = Regex("""^${Regex.escape(key)}\s*[=]""")
                if (keyPattern.containsMatchIn(trimmed)) {
                    toRemove.add(i)
                }
            }

            // Remove library entries that match removed dep names
            // Handles both: dep-name = { module = "..." } and dep-name = { group = "...", name = "..." }
            for (dep in gradleDeps) {
                val libKey = dep.replace("-", ".").replace("_", ".")
                val altKey = dep // original kebab form
                val keyPattern = Regex("""^(${Regex.escape(libKey)}|${Regex.escape(altKey)})\s*[=]""")
                if (keyPattern.containsMatchIn(trimmed) &&
                    (trimmed.contains("module =") || trimmed.contains("group =") || trimmed.contains("= \""))) {
                    toRemove.add(i)
                }
            }

            // Remove any library entry that references a removed version key
            // Handles: version.ref = "key" and version = { ref = "key" }
            for (key in versionKeys) {
                if (lines[i].contains("""version.ref = "$key"""") ||
                    lines[i].contains("""version = { ref = "$key"""")) {
                    toRemove.add(i)
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        tomlFile.writeText(result.joinToString("\n"))
    }
}
