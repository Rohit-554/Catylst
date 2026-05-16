package com.catylst.cli.template

import com.catylst.cli.model.AiProvider
import com.catylst.cli.model.GeneratorConfig
import java.io.File

object AiProviderSelector {

    fun apply(projectDir: File, config: GeneratorConfig) {
        if (config.aiProvider == AiProvider.NONE) return

        val appModule = findFile(projectDir, "AppModule.kt") ?: return
        var content = appModule.readText()

        val providerClass = when (config.aiProvider) {
            AiProvider.CLAUDE -> "ClaudeProvider"
            AiProvider.GROQ -> "GroqProvider"
            AiProvider.GEMINI -> "GeminiProvider"
            AiProvider.NONE -> return
        }

        val configField = when (config.aiProvider) {
            AiProvider.CLAUDE -> "AppConfig.claudeApiKey"
            AiProvider.GROQ -> "AppConfig.groqApiKey"
            AiProvider.GEMINI -> "AppConfig.geminiApiKey"
            AiProvider.NONE -> return
        }

        // Replace the active provider line
        val providerRegex =
            """single<AiProvider>\s*\{\s*(ClaudeProvider|GroqProvider|GeminiProvider)\(get\(\),\s*[^)]+\)\s*\}"""
                .toRegex()
        content = providerRegex.replace(content, "single<AiProvider> { $providerClass(get(), $configField) }")

        // Remove unused provider imports
        val allProviders = listOf("ClaudeProvider", "GroqProvider", "GeminiProvider")
        val unusedProviders = allProviders.filter { it != providerClass }
        for (unused in unusedProviders) {
            content = content.replaceLineContaining("import .*$unused".toRegex())
        }

        appModule.writeText(content)

        // Delete unused provider files
        val providersDir = findFile(projectDir, "providers")?.parentFile
        if (providersDir != null && providersDir.name == "ai") {
            for (unused in unusedProviders) {
                val file = File(providersDir, "$unused.kt")
                if (file.exists()) {
                    file.delete()
                    println("   Deleted unused provider: ${file.name}")
                }
            }
        }

        // Remove unused BuildConfig fields from androidApp/build.gradle.kts
        val androidBuildFile = File(projectDir, "androidApp/build.gradle.kts")
        if (androidBuildFile.exists()) {
            var androidContent = androidBuildFile.readText()
            val unusedFields = when (config.aiProvider) {
                AiProvider.CLAUDE -> listOf("GROQ_API_KEY", "GEMINI_API_KEY")
                AiProvider.GROQ -> listOf("CLAUDE_API_KEY", "GEMINI_API_KEY")
                AiProvider.GEMINI -> listOf("CLAUDE_API_KEY", "GROQ_API_KEY")
                AiProvider.NONE -> listOf("CLAUDE_API_KEY", "GROQ_API_KEY", "GEMINI_API_KEY")
            }
            for (field in unusedFields) {
                androidContent = androidContent.replaceLineContaining("buildConfigField.*$field".toRegex())
            }
            androidBuildFile.writeText(androidContent)
        }
    }

    private fun String.replaceLineContaining(regex: Regex): String {
        return this.lines().filterNot { it.matches(regex) || regex.containsMatchIn(it) }.joinToString("\n")
    }

    private fun findFile(projectDir: File, name: String): File? {
        return projectDir.walkTopDown()
            .filter { it.isFile && it.name == name }
            .firstOrNull()
    }
}
