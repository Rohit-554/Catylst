package com.catylst.cli.template

import com.catylst.cli.model.FeatureDef
import com.catylst.cli.model.FeatureManifest
import com.catylst.cli.model.GeneratorConfig
import com.catylst.cli.model.loadManifest
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private fun manifestFeaturesToScreens(features: List<FeatureDef>): Set<String> {
    return features.flatMap { it.screens }.toSet()
}

object ProjectGenerator {

    fun generate(config: GeneratorConfig) {
        val manifest = loadManifest()

        println("🚀 Generating project: ${config.projectName}")
        println("📦 Package: ${config.packageName}")
        println("🎯 Features: ${config.features.joinToString(", ")}")
        println("🤖 AI Provider: ${config.aiProvider}")
        println("📂 Output: ${config.outputDir.absolutePath}")

        // Step 1: Use local template (parent project directory)
        val templateDir = File("").absoluteFile.parentFile
        println("📁 Using local template: ${templateDir.absolutePath}")

        // Step 2: Deep copy to output directory
        val projectDir = File(config.outputDir, config.projectName)
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        }
        deepCopy(templateDir, projectDir)

        // Step 3: Rename package and app name
        println("📝 Renaming package and app...")
        PackageRenamer.rename(projectDir, config)

        // Step 4: Remove unselected features
        println("🔧 Applying feature selections...")
        FeatureRemover.removeFeatures(projectDir, config, manifest.features, manifest.sampleCode)

        // Step 4b: Regenerate navigation, screens, and home screen holistically
        val allRemovedScreens = manifestFeaturesToScreens(manifest.features.filter { it.id !in config.features })
        NavigationEditor.removeScreens(projectDir, allRemovedScreens)

        // Step 4c: Regenerate HomeViewModel based on remaining features
        HomeViewModelEditor.regenerate(
            projectDir,
            config.packageName,
            hasRoom = "room" in config.features,
            hasKtor = "ktor" in config.features
        )

        // Step 5: Clean dependencies in TOML
        println("🧹 Cleaning dependencies...")
        DependencyCleaner.clean(projectDir, config, manifest.features)

        // Step 6: Apply AI provider selection
        if (config.aiProvider != com.catylst.cli.model.AiProvider.NONE) {
            println("🤖 Setting AI provider to ${config.aiProvider}...")
            AiProviderSelector.apply(projectDir, config)
        }
        MainActivityEditor.apply(projectDir, config)

        // Step 7: Apply sample code selection
        if (!config.sampleCode) {
            println("🗑️  Removing sample code...")
            SampleCodeManager.apply(projectDir, config, manifest.features, manifest.sampleCode)
        }

        // Step 8: Apply theme customization
        if (config.themeSeedColor != null) {
            println("🎨 Generating custom theme...")
            ThemeGenerator.generate(
                projectDir,
                config.packageName,
                config.themeSeedColor,
                config.themeExpressive
            )
        }

        // Step 9: Clean up project (remove .git, docs, scripts, etc.)
        ProjectCleaner.clean(projectDir, config)

        println("")
        println("✅ Project generated successfully at: ${projectDir.absolutePath}")
        println("")
        println("Next steps:")
        println("  cd ${projectDir.name}")
        println("  ./gradlew :androidApp:assembleDebug")
    }

    private fun deepCopy(source: File, destination: File) {
        if (source.isDirectory) {
            destination.mkdirs()
            source.listFiles()?.forEach { child ->
                // Skip the cli-generator directory to avoid copying ourselves
                if (child.name == "cli-generator") return@forEach
                deepCopy(child, File(destination, child.name))
            }
        } else {
            destination.parentFile?.mkdirs()
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
