package com.catylst.plugin.model

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
    val id: String,
    val label: String,
    val iosInfoPlistKey: String?,
    val locationDelegate: Boolean
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
    LOCAL_KMP,
    LOCAL_TOOL,
    LOCAL_OPSX,
    REMOTE
}

data class SkillEntry(
    val id: String,
    val label: String,
    val description: String,
    val category: String,
    val source: SkillSource,
    val localRelativePath: String? = null,
    val skillMdUrl: String = "",
    val contentsApiUrl: String? = null
)

data class GeneratorConfig(
    val packageName: String,
    val appName: String,
    val projectName: String,
    val features: Set<String>,
    val sampleCode: Boolean,
    val aiProvider: AiProvider,
    val aiProviders: Set<AiProvider> = setOf(aiProvider).filter { it != AiProvider.NONE }.toSet(),
    val includeAndroid: Boolean = true,
    val includeIos: Boolean = true,
    val includeDesktop: Boolean = true,
    val themeSeedColor: String? = null,
    val themeExpressive: Boolean = false,
    val themeFonts: Map<String, java.io.File?> = emptyMap(),
    val selectedPermissions: Set<String>? = null,
    val selectedSkills: List<SkillEntry> = emptyList(),
    val outputDir: java.io.File
) {
    val packagePath: String get() = packageName.replace(".", "/")
}
