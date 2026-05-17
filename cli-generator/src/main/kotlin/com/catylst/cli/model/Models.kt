package com.catylst.cli.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FeatureManifest(
    val features: List<FeatureDef>,
    val sampleCode: SampleCodeDef,
    val theme: ThemeConfig
)

@Serializable
data class FeatureDef(
    val id: String,
    val name: String,
    val description: String,
    val default: Boolean,
    val requires: List<String>,
    val files: List<String>,
    val gradleDeps: List<String>,
    val kspProcessors: List<String>,
    val tomlVersionKeys: List<String>,
    val tomlPluginKeys: List<String>,
    val koinBindings: List<String>,
    val platformBindings: Map<String, List<String>>,
    val screens: List<String>,
    val buildConfigFields: List<String>,
    val iosInfoPlistKeys: List<String>,
    val settingsInclude: List<String> = emptyList(),
    val permissionTypes: List<PermissionTypeDef> = emptyList()
)

@Serializable
data class PermissionTypeDef(
    val id: String,           // matches Permission enum name: CAMERA, LOCATION, etc.
    val label: String,
    val iosInfoPlistKey: String?,
    val locationDelegate: Boolean  // true = LocationPermissionDelegate.kt is needed
)

@Serializable
data class SampleCodeDef(
    val description: String,
    val default: Boolean,
    val perFeature: Map<String, SampleFeatureDef>
)

@Serializable
data class SampleFeatureDef(
    val files: List<String>,
    val screens: List<String>,
    val koinBindings: List<String>,
    val homeButtons: List<String>
)

@Serializable
data class ThemeConfig(
    val expressiveMotion: Boolean,
    val seedColor: String,
    val advancedSeeds: AdvancedSeeds,
    val fonts: FontConfig
)

@Serializable
data class AdvancedSeeds(
    val secondary: String?,
    val tertiary: String?
)

@Serializable
data class FontConfig(
    val displayHeadline: String?,
    val body: String?,
    val labelTitle: String?
)

fun loadManifest(): FeatureManifest {
    val json = Json { ignoreUnknownKeys = true }
    val resource = object {}.javaClass.getResourceAsStream("/templates/manifest.json")
        ?: error("manifest.json not found in resources")
    return json.decodeFromString(resource.bufferedReader().readText())
}

enum class AiProvider {
    CLAUDE, GROQ, GEMINI, NONE
}

enum class SkillSource {
    LOCAL_KMP,      // .claude/skills/kmp-*/  in template
    LOCAL_TOOL,     // .claude/skills/<id>/   in template (clean-code, figma-mcp)
    LOCAL_OPSX,     // .claude/commands/opsx/ in template
    REMOTE          // downloaded from GitHub at generation time
}

data class SkillEntry(
    val id: String,
    val label: String,
    val description: String,
    val category: String,   // "kmp" | "workflow" | "quality" | "design" | "community"
    val source: SkillSource,
    /** Path relative to templateDir used for LOCAL_* sources (e.g. ".claude/skills/kmp-add-feature") */
    val localRelativePath: String? = null,
    /** Raw download URL — only for REMOTE source */
    val skillMdUrl: String = "",
    /** GitHub API URL for discovering assets/references — only for REMOTE source */
    val contentsApiUrl: String? = null
)

data class GeneratorConfig(
    val packageName: String,
    val appName: String,
    val projectName: String,
    val features: Set<String>,
    val sampleCode: Boolean,
    /** Primary (first) AI provider — used for AppModule.kt binding. */
    val aiProvider: AiProvider,
    /** All selected AI providers — determines which provider files are kept. */
    val aiProviders: Set<AiProvider> = setOf(aiProvider).filter { it != AiProvider.NONE }.toSet(),
    val includeAndroid: Boolean = true,
    val includeIos: Boolean = true,
    val includeDesktop: Boolean = true,
    val themeSeedColor: String? = null,
    val themeExpressive: Boolean = false,
    /** Font files keyed by slot: "displayHeadline", "body", "labelTitle" */
    val themeFonts: Map<String, java.io.File?> = emptyMap(),
    /** Subset of permission IDs to keep (null = keep all). Only used when "permissions" feature is selected. */
    val selectedPermissions: Set<String>? = null,
    /** AI agent skills to install into .skills/ in the generated project. */
    val selectedSkills: List<SkillEntry> = emptyList(),
    val outputDir: java.io.File
) {
    val packagePath: String get() = packageName.replace(".", "/")
}
