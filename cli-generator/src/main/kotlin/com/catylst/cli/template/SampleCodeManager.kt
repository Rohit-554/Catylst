package com.catylst.cli.template

import com.catylst.cli.model.FeatureDef
import com.catylst.cli.model.GeneratorConfig
import com.catylst.cli.model.SampleCodeDef
import java.io.File

object SampleCodeManager {

    fun apply(
        projectDir: File,
        config: GeneratorConfig,
        manifestFeatures: List<FeatureDef>,
        sampleCodeDef: SampleCodeDef
    ) {
        if (config.sampleCode) return // Keep all sample code

        val selectedFeatures = manifestFeatures.filter { it.id in config.features }
        val screensToRemove = mutableSetOf<String>()

        for (feature in selectedFeatures) {
            val sampleDef = sampleCodeDef.perFeature[feature.id] ?: continue

            println("🗑️  Removing sample code for: ${feature.name}")

            // 1. Delete sample files
            for (pattern in sampleDef.files) {
                val resolved = pattern.replace("{pkg}", config.packagePath)
                val target = File(projectDir, resolved)
                if (target.exists()) {
                    target.deleteRecursively()
                    println("   Deleted: ${target.relativeTo(projectDir)}")
                }
            }

            // 2. Remove sample koin bindings from AppModule.kt
            if (sampleDef.koinBindings.isNotEmpty()) {
                removeKoinBindings(projectDir, sampleDef.koinBindings)
            }

            // Collect screens to remove for holistic regeneration
            screensToRemove.addAll(sampleDef.screens)
        }

        // Regenerate navigation files holistically after all sample code is removed
        if (screensToRemove.isNotEmpty()) {
            NavigationEditor.removeScreens(projectDir, screensToRemove)
        }
    }

    private fun removeKoinBindings(projectDir: File, bindings: List<String>) {
        val appModule = findFile(projectDir, "AppModule.kt") ?: return
        val lines = appModule.readText().lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            for (binding in bindings) {
                // Remove import lines
                if (lines[i].contains(Regex("import .*$binding"))) {
                    toRemove.add(i)
                }
                // Remove binding lines
                if (lines[i].contains(Regex("viewModel\\s*\\{\\s*$binding")) ||
                    lines[i].contains(Regex("single\\s*\\{\\s*$binding"))
                ) {
                    toRemove.add(i)
                }
            }
        }

        appModule.writeText(lines.filterIndexed { i, _ -> i !in toRemove }.joinToString("\n"))
    }

    private fun findFile(projectDir: File, name: String): File? {
        return projectDir.walkTopDown()
            .filter { it.isFile && it.name == name }
            .firstOrNull()
    }
}
