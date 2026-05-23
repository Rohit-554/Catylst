package com.catylst.plugin.template

import com.catylst.plugin.model.FeatureDef
import com.catylst.plugin.model.GeneratorConfig
import com.catylst.plugin.model.SampleCodeDef
import java.io.File

object SampleCodeManager {

    fun apply(
        projectDir: File,
        config: GeneratorConfig,
        manifestFeatures: List<FeatureDef>,
        sampleCodeDef: SampleCodeDef
    ) {
        if (config.sampleCode) return

        val selectedFeatures = manifestFeatures.filter { it.id in config.features }
        val screensToRemove = mutableSetOf<String>()

        for (feature in selectedFeatures) {
            val sampleDef = sampleCodeDef.perFeature[feature.id] ?: continue

            for (pattern in sampleDef.files) {
                val resolved = pattern.replace("{pkg}", config.packagePath)
                val target = File(projectDir, resolved)
                if (target.exists()) {
                    target.deleteRecursively()
                }
            }

            if (sampleDef.koinBindings.isNotEmpty()) {
                removeKoinBindings(projectDir, sampleDef.koinBindings)
            }

            screensToRemove.addAll(sampleDef.screens)
        }

        if (screensToRemove.isNotEmpty()) {
            NavigationEditor.removeScreens(projectDir, screensToRemove)
        }

        pruneEmptyDirs(projectDir)
    }

    private fun pruneEmptyDirs(root: File) {
        root.walkBottomUp()
            .filter { it.isDirectory && it != root }
            .forEach { dir ->
                if (dir.exists() && dir.walkTopDown().none { it.isFile }) {
                    dir.deleteRecursively()
                }
            }
    }

    private fun removeKoinBindings(projectDir: File, bindings: List<String>) {
        val appModule = findFile(projectDir, "AppModule.kt") ?: return
        val lines = appModule.readText().lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            for (binding in bindings) {
                if (lines[i].contains(Regex("import .*$binding"))) {
                    toRemove.add(i)
                }
                if (lines[i].contains(Regex("viewModel\\s*\\{\\s*$binding")) ||
                    lines[i].contains(Regex("single\\s*\\{\\s*$binding"))
                ) {
                    toRemove.add(i)
                }
            }
        }

        appModule.writeText(lines.filterIndexed { i, _ -> i !in toRemove }.joinToString("\n"))
    }
}
