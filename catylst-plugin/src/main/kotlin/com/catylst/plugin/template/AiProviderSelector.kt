package com.catylst.plugin.template

import com.catylst.plugin.model.AiProvider
import com.catylst.plugin.model.GeneratorConfig
import java.io.File

object AiProviderSelector {

    fun apply(projectDir: File, config: GeneratorConfig) {
        val allProviders    = listOf("ClaudeProvider", "GroqProvider", "GeminiProvider")
        val allApiKeyFields = listOf("CLAUDE_API_KEY", "GROQ_API_KEY", "GEMINI_API_KEY")

        if (config.aiProvider == AiProvider.NONE) {
            removeApiKeyFields(projectDir, allApiKeyFields)
            return
        }

        val primaryClass = when (config.aiProvider) {
            AiProvider.CLAUDE -> "ClaudeProvider"
            AiProvider.GROQ   -> "GroqProvider"
            AiProvider.GEMINI -> "GeminiProvider"
            AiProvider.NONE   -> return
        }
        val primaryConfigField = when (config.aiProvider) {
            AiProvider.CLAUDE -> "AppConfig.claudeApiKey"
            AiProvider.GROQ   -> "AppConfig.groqApiKey"
            AiProvider.GEMINI -> "AppConfig.geminiApiKey"
            AiProvider.NONE   -> return
        }

        val keepProviders = config.aiProviders.map { provider ->
            when (provider) {
                AiProvider.CLAUDE -> "ClaudeProvider"
                AiProvider.GROQ   -> "GroqProvider"
                AiProvider.GEMINI -> "GeminiProvider"
                AiProvider.NONE   -> null
            }
        }.filterNotNull().toSet()

        val unusedProviders = allProviders.filter { it !in keepProviders }

        val appModule = findFile(projectDir, "AppModule.kt")
        if (appModule != null) {
            var content = appModule.readText()
            val providerRegex =
                """single<AiProvider>\s*\{\s*(ClaudeProvider|GroqProvider|GeminiProvider)\(get\(\),\s*[^)]+\)\s*\}"""
                    .toRegex()
            content = providerRegex.replace(content, "single<AiProvider> { $primaryClass(get(), $primaryConfigField) }")
            for (unused in unusedProviders) {
                content = content.replaceLineContaining("import .*$unused".toRegex())
            }
            appModule.writeText(content)
        }

        val providersDir = findDir(projectDir, "providers")
        if (providersDir != null) {
            for (unused in unusedProviders) {
                val file = File(providersDir, "$unused.kt")
                if (file.exists()) {
                    file.delete()
                }
            }
        }

        val unusedFields = allApiKeyFields.filter { field ->
            unusedProviders.any { provider -> field.contains(provider.removeSuffix("Provider").uppercase()) }
        }
        removeApiKeyFields(projectDir, unusedFields)
    }

    private fun removeApiKeyFields(projectDir: File, fields: List<String>) {
        val androidBuildFile = File(projectDir, "androidApp/build.gradle.kts")
        if (!androidBuildFile.exists()) return
        var content = androidBuildFile.readText()
        for (field in fields) {
            content = content.replaceLineContaining("buildConfigField.*$field".toRegex())
        }
        androidBuildFile.writeText(content)
    }

    private fun String.replaceLineContaining(regex: Regex): String {
        return this.lines().filterNot { it.matches(regex) || regex.containsMatchIn(it) }.joinToString("\n")
    }
}
