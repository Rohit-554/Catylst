package com.catylst.cli

import com.catylst.cli.model.AiProvider
import com.catylst.cli.model.FeatureManifest
import com.catylst.cli.model.GeneratorConfig
import com.catylst.cli.model.loadManifest
import com.catylst.cli.template.ProjectGenerator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

class CatylstCli : CliktCommand(
    name = "catylst",
    help = """
        Catylst KMP Project Generator
        
        Generate a customized Kotlin Multiplatform project with selected features.
        
        Examples:
          catylst --package com.example.myapp --name MyApp --features ai,ktor,room
          catylst --interactive
    """.trimIndent()
) {

    private val packageName by option(
        "--package", "-p",
        help = "Application package name (e.g., com.example.myapp)"
    )

    private val appName by option(
        "--name", "-n",
        help = "Application display name"
    )

    private val projectName by option(
        "--project",
        help = "Project directory name (defaults to app name)"
    )

    private val features by option(
        "--features", "-f",
        help = "Comma-separated feature IDs. Available: ai, notifications, permissions, room, preferences, ktor, server"
    )

    private val noSampleCode by option(
        "--no-sample",
        help = "Exclude sample/demo code"
    ).flag(default = false)

    private val aiProvider by option(
        "--ai-provider", "-a",
        help = "AI provider: claude, groq, gemini, none"
    ).choice("claude", "groq", "gemini", "none")

    private val themeColor by option(
        "--theme-color",
        help = "Theme seed color (hex, e.g., #6750A4)"
    )

    private val themeExpressive by option(
        "--theme-expressive",
        help = "Enable Material 3 Expressive motion"
    ).flag(default = false)

    private val outputDir by option(
        "--output", "-o",
        help = "Output directory for generated project"
    ).file(canBeFile = false).default(File("."))

    private val interactive by option(
        "--interactive", "-i",
        help = "Run in interactive mode with prompts"
    ).flag(default = false)

    override fun run() {
        val manifest = loadManifest()
        val featureIds = manifest.features.map { it.id }

        val result = if (interactive) {
            runInteractive(manifest, featureIds)
        } else {
            runNonInteractive(manifest, featureIds)
        }

        val config = GeneratorConfig(
            packageName = result.pkg,
            appName = result.app,
            projectName = result.proj,
            features = result.features,
            sampleCode = result.sample,
            aiProvider = result.ai,
            themeSeedColor = result.themeColor,
            themeExpressive = result.themeExpressive,
            outputDir = outputDir
        )

        ProjectGenerator.generate(config)
    }

    private data class InteractiveResult(
        val pkg: String,
        val app: String,
        val proj: String,
        val features: Set<String>,
        val sample: Boolean,
        val ai: AiProvider,
        val themeColor: String? = null,
        val themeExpressive: Boolean = false
    )

    private fun runInteractive(
        manifest: FeatureManifest,
        featureIds: List<String>
    ): InteractiveResult {
        echo("🚀 Welcome to Catylst KMP Project Generator!\n")

        val pkg = packageName ?: prompt("Package name (e.g., com.example.myapp)")!!
        val app = appName ?: prompt("App name (e.g., MyApp)")!!
        val proj = projectName ?: app

        echo("\n📋 Available features:")
        manifest.features.forEach { f ->
            val defaultMark = if (f.default) " (default)" else ""
            echo("  [${f.id}] ${f.name} — ${f.description}$defaultMark")
        }

        val defaultFeatures = manifest.features.filter { it.default }.joinToString(",") { it.id }
        val featuresInput = features ?: prompt("\nSelect features (comma-separated, or 'all' for defaults)", default = defaultFeatures)!!

        val selectedFeatures = if (featuresInput.lowercase() == "all") {
            manifest.features.filter { it.default }.map { it.id }.toSet()
        } else {
            featuresInput.split(",").map { it.trim() }.filter { it in featureIds }.toSet()
        }

        // Resolve dependencies
        val resolvedFeatures = resolveDependencies(selectedFeatures, manifest.features)

        val sample = if (noSampleCode) {
            false
        } else {
            val input = prompt("Include sample/demo code? [Y/n]", default = "Y")!!
            input.lowercase() in listOf("y", "yes", "")
        }

        val ai = if ("ai" in resolvedFeatures) {
            val input = prompt("Select AI provider [claude/groq/gemini/none]", default = "claude")!!
            AiProvider.valueOf(input.uppercase())
        } else {
            AiProvider.NONE
        }

        val themeInput = themeColor ?: prompt("Theme seed color (hex, or 'skip' for default)", default = "skip")!!
        val themeSeed = if (themeInput.lowercase() == "skip") null else themeInput
        val expressive = themeExpressive || run {
            val input = prompt("Enable M3 Expressive motion? [y/N]", default = "N")!!
            input.lowercase() == "y"
        }

        return InteractiveResult(pkg, app, proj, resolvedFeatures, sample, ai, themeSeed, expressive)
    }

    private fun runNonInteractive(
        manifest: FeatureManifest,
        featureIds: List<String>
    ): InteractiveResult {
        val pkg = packageName ?: error("--package is required (or use --interactive)")
        val app = appName ?: error("--name is required (or use --interactive)")
        val proj = projectName ?: app

        val selectedFeatures = if (features != null) {
            features!!.split(",").map { it.trim() }.filter { it in featureIds }.toSet()
        } else {
            manifest.features.filter { it.default }.map { it.id }.toSet()
        }

        val resolvedFeatures = resolveDependencies(selectedFeatures, manifest.features)

        val sample = !noSampleCode
        val ai = aiProvider?.let { AiProvider.valueOf(it.uppercase()) }
            ?: if ("ai" in resolvedFeatures) AiProvider.CLAUDE else AiProvider.NONE
        val themeSeed = themeColor
        val expressive = themeExpressive

        return InteractiveResult(pkg, app, proj, resolvedFeatures, sample, ai, themeSeed, expressive)
    }

    private fun resolveDependencies(
        selected: Set<String>,
        features: List<com.catylst.cli.model.FeatureDef>
    ): Set<String> {
        val result = selected.toMutableSet()
        var changed = true
        while (changed) {
            changed = false
            for (feature in features) {
                if (feature.id in result) {
                    for (req in feature.requires) {
                        if (req !in result) {
                            result.add(req)
                            changed = true
                            echo("⚠️  Auto-enabling '$req' (required by '${feature.id}')")
                        }
                    }
                }
            }
        }
        return result
    }
}

fun main(args: Array<String>) = CatylstCli().parse(args)
