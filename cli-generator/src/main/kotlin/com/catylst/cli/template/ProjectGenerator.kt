package com.catylst.cli.template

import com.catylst.cli.model.FeatureDef
import com.catylst.cli.model.FeatureManifest
import com.catylst.cli.model.GeneratorConfig
import com.catylst.cli.model.loadManifest
import com.catylst.cli.skills.SkillsInstaller
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private fun screensRemovedBy(features: List<FeatureDef>): Set<String> =
    features.flatMap { it.screens }.toSet()

object ProjectGenerator {

    fun generate(config: GeneratorConfig) {
        val manifest = loadManifest()

        println("🚀 Generating project: ${config.projectName}")
        println("📦 Package: ${config.packageName}")
        println("🎯 Features: ${config.features.joinToString(", ")}")
        println("🤖 AI Provider: ${config.aiProvider}")
        println("📂 Output: ${config.outputDir.absolutePath}")

        val templateDir = File("").absoluteFile.parentFile
        println("📁 Using local template: ${templateDir.absolutePath}")

        val projectDir = File(config.outputDir, config.projectName)
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        }
        deepCopy(templateDir, projectDir)

        println("📝 Renaming package and app...")
        PackageRenamer.rename(projectDir, config)

        println("🔧 Applying feature selections...")
        FeatureRemover.removeFeatures(projectDir, config, manifest.features, manifest.sampleCode)

        val removedScreens = screensRemovedBy(manifest.features.filter { it.id !in config.features })
        NavigationEditor.removeScreens(projectDir, removedScreens)

        HomeViewModelEditor.regenerate(
            projectDir,
            config.packageName,
            hasRoom = "room" in config.features,
            hasKtor = "ktor" in config.features,
            selectedFeatures = config.features
        )

        println("🧹 Cleaning dependencies...")
        DependencyCleaner.clean(projectDir, config, manifest.features)

        if ("permissions" in config.features && config.selectedPermissions != null) {
            val permissionFeature = manifest.features.first { it.id == "permissions" }
            if (permissionFeature.permissionTypes.isNotEmpty()) {
                PermissionStripper.apply(projectDir, permissionFeature.permissionTypes, config.selectedPermissions)
            }
        }

        println("🤖 Applying AI provider: ${config.aiProvider}...")
        AiProviderSelector.apply(projectDir, config)
        MainActivityEditor.apply(projectDir, config)

        if (!config.sampleCode) {
            println("🗑️  Removing sample code...")
            SampleCodeManager.apply(projectDir, config, manifest.features, manifest.sampleCode)
        }

        if (config.themeSeedColor != null) {
            println("🎨 Generating custom theme...")
            ThemeGenerator.generate(
                projectDir,
                config.packageName,
                config.themeSeedColor,
                config.themeExpressive,
                config.themeFonts
            )
        }

        ProjectCleaner.clean(projectDir, config)

        if (config.selectedSkills.isNotEmpty()) {
            println("📥 Installing ${config.selectedSkills.size} skill(s)...")
            SkillsInstaller.install(projectDir, templateDir, config.selectedSkills)
        }

        println("")
        println("✅ Project generated successfully at: ${projectDir.absolutePath}")
        println("")
        println("Next steps:")
        println("  cd ${projectDir.name}")
        println("  Before building, add your Android SDK path to local.properties:")
        println("    sdk.dir=/Users/<you>/Library/Android/sdk")
        println("  ./gradlew :androidApp:assembleDebug")
    }

    private val SKIP_COPY = setOf("cli-generator", "build", ".gradle", ".kotlin", ".git", ".idea")

    private fun deepCopy(source: File, destination: File) {
        if (source.isDirectory) {
            destination.mkdirs()
            source.listFiles()?.forEach { child ->
                if (child.name in SKIP_COPY) return@forEach
                deepCopy(child, File(destination, child.name))
            }
        } else {
            destination.parentFile?.mkdirs()
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
