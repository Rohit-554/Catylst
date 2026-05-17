package com.catylst.cli

import com.catylst.cli.model.AiProvider
import com.catylst.cli.model.FeatureManifest
import com.catylst.cli.model.GeneratorConfig
import com.catylst.cli.model.SkillEntry
import com.catylst.cli.model.loadManifest
import com.catylst.cli.skills.SkillsInstaller
import com.catylst.cli.template.ProjectGenerator
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

class CatylstCli : CoreCliktCommand(name = "catylst") {

    override fun help(context: com.github.ajalt.clikt.core.Context) = """
        Catylst KMP Project Generator

        Generate a customized Kotlin Multiplatform project with selected features.

        Examples:
          catylst --interactive                    # Wizard mode
          catylst -p com.example.app -n MyApp      # Quick generate with defaults
          catylst -p com.app -n App -f ai,ktor     # Select specific features
    """.trimIndent()

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
        help = "Comma-separated feature IDs: ai, notifications, permissions, room, preferences, ktor, server"
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
        help = "Run in interactive wizard mode"
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
            aiProvider = result.primaryAi,
            aiProviders = result.ais,
            themeSeedColor = result.themeColor,
            themeExpressive = result.themeExpressive,
            themeFonts = result.themeFonts,
            selectedPermissions = result.selectedPermissions,
            selectedSkills = result.selectedSkills,
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
        val ais: Set<AiProvider>,
        val themeColor: String? = null,
        val themeExpressive: Boolean = false,
        val themeFonts: Map<String, File?> = emptyMap(),
        val selectedPermissions: Set<String>? = null,
        val selectedSkills: List<SkillEntry> = emptyList()
    ) {
        val primaryAi: AiProvider get() = ais.firstOrNull() ?: AiProvider.NONE
    }

    private fun runInteractive(
        manifest: FeatureManifest,
        featureIds: List<String>
    ): InteractiveResult {
        while (true) {
            echo("\u001B[1m\n  ╔══════════════════════════════════════╗")
            echo("  ║   Catylst KMP Project Generator     ║")
            echo("  ╚══════════════════════════════════════╝\u001B[0m")

            // ── SECTION 1: PROJECT ────────────────────────────────────────────
            sectionHeader("PROJECT")

            val pkg = if (packageName != null) {
                echo("  Package  : ${packageName!!}")
                packageName!!
            } else {
                print("  Package  \u001B[2m(e.g., com.example.myapp)\u001B[0m\n  \u001B[36m›\u001B[0m ")
                System.out.flush()
                readlnOrNull()?.trim()?.takeIf { it.isNotBlank() }
                    ?: error("Package name is required")
            }

            val app = if (appName != null) {
                echo("  App name : ${appName!!}")
                appName!!
            } else {
                print("  App name \u001B[2m(e.g., MyApp)\u001B[0m\n  \u001B[36m›\u001B[0m ")
                System.out.flush()
                readlnOrNull()?.trim()?.takeIf { it.isNotBlank() }
                    ?: error("App name is required")
            }
            val proj = projectName ?: app

            // ── SECTION 2: FEATURES ───────────────────────────────────────────
            sectionHeader("FEATURES", "Enter = defaults")

            val defaultSelected = manifest.features.filter { it.default }.map { it.id }.toSet()
            val forcedByFlag = if (features != null)
                features!!.split(",").map { it.trim() }.filter { it in featureIds }.toSet()
            else null

            val featureOptions = manifest.features.map { f ->
                SelectOption(value = f.id, label = f.name, description = f.description)
            }

            val rawSelected = forcedByFlag ?: interactiveCheckboxSelect(featureOptions, defaultSelected)

            // Resolve dependencies
            val resolvedFeatures = resolveDependencies(rawSelected, manifest.features)
            if (resolvedFeatures != rawSelected) {
                val added = resolvedFeatures - rawSelected
                echo("  \u001B[33m⚠  Auto-enabled: ${added.joinToString(", ")} (required by selected features)\u001B[0m")
            }

            // Permissions sub-selection (inline, still in FEATURES section)
            val selectedPermissions: Set<String>? = if ("permissions" in resolvedFeatures) {
                val permDef = manifest.features.first { it.id == "permissions" }
                if (permDef.permissionTypes.isNotEmpty()) {
                    echo("")
                    echo("  \u001B[2mPermissions — which ones?\u001B[0m")
                    val opts = permDef.permissionTypes.map {
                        SelectOption(value = it.id, label = it.label)
                    }
                    interactiveCheckboxSelect(opts, opts.map { it.value }.toSet()).also { selected ->
                        if (selected.isEmpty()) {
                            echo("  \u001B[33m⚠  No permissions selected — permissions feature will be skipped\u001B[0m")
                        }
                    }
                } else null
            } else null

            // Agent-skills sub-selection (inline, still in FEATURES section)
            val selectedSkills: List<SkillEntry> = if ("agent-skills" in resolvedFeatures) {
                echo("")
                echo("  \u001B[33m⚡\u001B[0m \u001B[1mWhich skills to install?\u001B[0m  \u001B[2m(remote skills need internet at generation time)\u001B[0m")
                val opts = SkillsInstaller.catalogue.map {
                    val tag = when (it.category) {
                        "kmp"       -> "\u001B[32m[kmp]\u001B[0m"
                        "workflow"  -> "\u001B[36m[workflow]\u001B[0m"
                        "quality"   -> "\u001B[33m[quality]\u001B[0m"
                        "design"    -> "\u001B[35m[design]\u001B[0m"
                        else        -> "\u001B[2m[community]\u001B[0m"
                    }
                    SelectOption(it, "${it.label} $tag", it.description)
                }
                val defaultEntries = SkillsInstaller.catalogue
                    .filter { it.id in SkillsInstaller.defaultSelected }.toSet()
                interactiveCheckboxSelect(opts, defaultEntries).toList().also { selected ->
                    if (selected.isEmpty()) echo("  \u001B[2mNo skills selected — skipping\u001B[0m")
                }
            } else emptyList()

            // ── SECTION 3: OPTIONS ────────────────────────────────────────────
            sectionHeader("OPTIONS")

            val sample = if (noSampleCode) {
                echo("  Sample code : skipped (--no-sample)")
                false
            } else {
                confirm("Include sample/demo code?", default = true)
            }

            val ais: Set<AiProvider> = if ("ai" in resolvedFeatures) {
                if (aiProvider != null) {
                    setOf(AiProvider.valueOf(aiProvider!!.uppercase()))
                } else {
                    echo("")
                    interactiveCheckboxSelect(
                        options = listOf(
                            SelectOption(AiProvider.CLAUDE, "Claude",  "Anthropic — recommended"),
                            SelectOption(AiProvider.GROQ,   "Groq",    "LLama — fast inference"),
                            SelectOption(AiProvider.GEMINI, "Gemini",  "Google Gemini"),
                        ),
                        initial = setOf(AiProvider.CLAUDE)
                    ).ifEmpty { setOf(AiProvider.CLAUDE) } // default to Claude if none chosen
                }
            } else emptySet()

            // ── SECTION 4: THEME (optional) ───────────────────────────────────
            sectionHeader("THEME", "optional")

            val themeSeed = if (themeColor != null) {
                echo("  Seed color : ${themeColor!!}")
                requireValidHexColor(themeColor!!)
            } else {
                print("  Seed color \u001B[2m(#RRGGBB or Enter to skip)\u001B[0m\n  \u001B[36m›\u001B[0m ")
                System.out.flush()
                val input = readlnOrNull()?.trim()
                if (input.isNullOrBlank()) null else requireValidHexColor(input)
            }

            val expressive: Boolean
            val fonts: Map<String, File?>

            if (themeSeed != null) {
                echo("")
                expressive = themeExpressive || confirm("Enable Material 3 Expressive motion? (alpha)", default = false)
                echo("")
                echo("  \u001B[1mCustom fonts\u001B[0m \u001B[2m(optional — .ttf/.otf paths, Enter to skip)\u001B[0m")
                val body    = promptFontFile("  Body font (applies to all roles)")
                val display = promptFontFile("  Display/Headline override (Enter to reuse body)")
                val label   = promptFontFile("  Label/Title override (Enter to reuse body)")
                fonts = mapOf(
                    "body"            to body,
                    "displayHeadline" to (display ?: body),
                    "labelTitle"      to (label ?: body)
                ).filterValues { it != null }
            } else {
                expressive = themeExpressive
                fonts = emptyMap()
            }

            // ── READY ─────────────────────────────────────────────────────────
            sectionHeader("READY")
            printSummary(pkg, app, resolvedFeatures, sample, ais, themeSeed, expressive, selectedSkills)

            if (confirm("Generate project?", default = true)) {
                return InteractiveResult(pkg, app, proj, resolvedFeatures, sample, ais, themeSeed, expressive, fonts, selectedPermissions, selectedSkills)
            }
            // User said N → loop back to top
            echo("")
        }
    }

    private fun printSummary(
        pkg: String, app: String, features: Set<String>,
        sample: Boolean, ais: Set<AiProvider>, theme: String?, expressive: Boolean,
        skills: List<SkillEntry> = emptyList()
    ) {
        echo("\u001B[2m  ────────────────────────────────────────\u001B[0m")
        echo("\u001B[1m  Summary\u001B[0m")
        echo("  Package   : $pkg")
        echo("  App name  : $app")
        echo("  Features  : ${features.joinToString(", ")}")
        echo("  Sample    : $sample")
        if (ais.isNotEmpty()) echo("  AI        : ${ais.joinToString(", ") { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }}")
        if (theme != null) echo("  Theme     : $theme${if (expressive) " + Expressive" else ""}")
        if (skills.isNotEmpty()) echo("  Skills    : ${skills.joinToString(", ") { it.label }}")
        echo("\u001B[2m  ────────────────────────────────────────\u001B[0m")
        echo("")
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
        val primaryAi = aiProvider?.let { AiProvider.valueOf(it.uppercase()) }
            ?: if ("ai" in resolvedFeatures) AiProvider.CLAUDE else AiProvider.NONE
        val ais = if (primaryAi != AiProvider.NONE) setOf(primaryAi) else emptySet()
        val themeSeed = themeColor?.let { requireValidHexColor(it) }
        val expressive = themeExpressive

        return InteractiveResult(pkg, app, proj, resolvedFeatures, sample, ais, themeSeed, expressive)
    }

    private fun promptFontFile(label: String): File? {
        echo("  $label (path to .ttf/.otf, or Enter to skip):")
        val input = readlnOrNull()?.trim()
        if (input.isNullOrBlank()) return null
        val file = File(input)
        if (!file.exists()) {
            echo("  ⚠️  File not found, skipping: $input")
            return null
        }
        if (file.extension.lowercase() !in setOf("ttf", "otf")) {
            echo("  ⚠️  Not a .ttf/.otf file, skipping: $input")
            return null
        }
        return file
    }

    private fun requireValidHexColor(input: String): String {
        val hex = if (input.startsWith("#")) input else "#$input"
        require(Regex("^#[0-9A-Fa-f]{6}$").matches(hex)) {
            "Invalid hex color '$input'. Expected format: #RRGGBB (e.g., #6750A4)"
        }
        return hex
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

fun main(args: Array<String>) = CatylstCli().main(args)
