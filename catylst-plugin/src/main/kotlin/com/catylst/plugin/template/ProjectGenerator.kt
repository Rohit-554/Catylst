package com.catylst.plugin.template

import com.catylst.plugin.model.FeatureDef
import com.catylst.plugin.model.GeneratorConfig
import com.catylst.plugin.model.loadManifest
import com.catylst.plugin.util.TemplateExtractor
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private fun screensRemovedBy(features: List<FeatureDef>): Set<String> =
    features.flatMap { it.screens }.toSet()

object ProjectGenerator {

    fun generate(config: GeneratorConfig, onProgress: (String) -> Unit = {}) {
        val manifest = loadManifest()

        onProgress("Extracting template…")
        val templateDir = TemplateExtractor.extract()

        onProgress("Copying template files…")
        val projectDir = File(config.outputDir, config.projectName)
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        }
        deepCopy(templateDir, projectDir)

        onProgress("Renaming package and app…")
        PackageRenamer.rename(projectDir, config)

        onProgress("Applying feature selections…")
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

        onProgress("Cleaning dependencies…")
        DependencyCleaner.clean(projectDir, config, manifest.features)

        if ("permissions" in config.features && config.selectedPermissions != null) {
            val permissionFeature = manifest.features.first { it.id == "permissions" }
            if (permissionFeature.permissionTypes.isNotEmpty()) {
                PermissionStripper.apply(projectDir, permissionFeature.permissionTypes, config.selectedPermissions)
            }
        }

        onProgress("Applying AI provider…")
        AiProviderSelector.apply(projectDir, config)
        MainActivityEditor.apply(projectDir, config)

        if (!config.sampleCode) {
            onProgress("Removing sample code…")
            SampleCodeManager.apply(projectDir, config, manifest.features, manifest.sampleCode)
        }

        if (config.themeSeedColor != null) {
            onProgress("Generating custom theme…")
            ThemeGenerator.generate(
                projectDir,
                config.packageName,
                config.themeSeedColor,
                config.themeExpressive,
                config.themeFonts
            )
        }

        onProgress("Cleaning up…")
        ProjectCleaner.clean(projectDir, config)

        onProgress("Done!")
    }

    private val SKIP_COPY = setOf("cli-generator", "catylst-plugin", "build", ".gradle", ".kotlin", ".git", ".idea", ".github")

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
