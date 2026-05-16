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
            val line = lines[i]

            // Remove version entries
            for (key in versionKeys) {
                if (line.trim().startsWith("$key ") || line.trim().startsWith("$key=")) {
                    toRemove.add(i)
                }
            }

            // Remove plugin entries
            for (key in pluginKeys) {
                if (line.trim().startsWith(key) && line.contains("= { id =")) {
                    toRemove.add(i)
                }
            }

            // Remove library entries that match removed deps OR reference removed versions
            for (dep in gradleDeps) {
                if (line.trim().startsWith(dep) && line.contains("= { module =")) {
                    toRemove.add(i)
                }
            }

            // Remove library entries that reference removed version keys
            for (key in versionKeys) {
                if (line.contains("version.ref = \"$key\"")) {
                    toRemove.add(i)
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        tomlFile.writeText(result.joinToString("\n"))
    }
}
