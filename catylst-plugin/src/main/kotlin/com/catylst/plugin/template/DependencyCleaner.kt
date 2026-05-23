package com.catylst.plugin.template

import com.catylst.plugin.model.FeatureDef
import com.catylst.plugin.model.GeneratorConfig
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
        val lines = tomlFile.readText().lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            val trimmed = lines[i].trim()

            // Matches: key = "x.y.z" or key = { ... }
            for (key in versionKeys) {
                val keyPattern = Regex("""^${Regex.escape(key)}\s*[=]""")
                if (keyPattern.containsMatchIn(trimmed)) {
                    toRemove.add(i)
                }
            }

            // Matches plugin entries only: key = { id = "...", version.ref = "..." }
            // The `id =` check prevents accidentally removing a same-named version entry.
            for (key in pluginKeys) {
                val keyPattern = Regex("""^${Regex.escape(key)}\s*[=]""")
                if (keyPattern.containsMatchIn(trimmed) && trimmed.contains("id =")) {
                    toRemove.add(i)
                }
            }

            // Matches: dep-name = { module = "..." } and dep-name = { group = "...", name = "..." }
            for (dep in gradleDeps) {
                val libKey = dep.replace("-", ".").replace("_", ".")
                val altKey = dep
                val keyPattern = Regex("""^(${Regex.escape(libKey)}|${Regex.escape(altKey)})\s*[=]""")
                if (keyPattern.containsMatchIn(trimmed) &&
                    (trimmed.contains("module =") || trimmed.contains("group =") || trimmed.contains("= \""))) {
                    toRemove.add(i)
                }
            }

            // Matches: version.ref = "key" and version = { ref = "key" }
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
