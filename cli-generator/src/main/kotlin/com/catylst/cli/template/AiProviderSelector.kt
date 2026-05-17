package com.catylst.cli.template

import com.catylst.cli.model.AiProvider
import com.catylst.cli.model.GeneratorConfig
import java.io.File

object AiProviderSelector {

    fun apply(projectDir: File, config: GeneratorConfig) {
        val allProviders    = listOf("ClaudeProvider", "GroqProvider", "GeminiProvider")
        val allApiKeyFields = listOf("CLAUDE_API_KEY", "GROQ_API_KEY", "GEMINI_API_KEY")

        if (config.aiProvider == AiProvider.NONE) {
            // AI feature was deselected — strip all three BuildConfig fields.
            removeApiKeyFields(projectDir, allApiKeyFields)
            return
        }

        // Primary provider drives the AppModule.kt binding
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

        // All provider class names that should be kept
        val keepProviders = config.aiProviders.map { provider ->
            when (provider) {
                AiProvider.CLAUDE -> "ClaudeProvider"
                AiProvider.GROQ   -> "GroqProvider"
                AiProvider.GEMINI -> "GeminiProvider"
                AiProvider.NONE   -> null
            }
        }.filterNotNull().toSet()

        val unusedProviders = allProviders.filter { it !in keepProviders }

        // 1. Swap the active provider binding in AppModule.kt to the primary provider
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
            println("   Set AI provider: $primaryClass (active)")
            if (keepProviders.size > 1) {
                println("   Kept additional providers: ${(keepProviders - primaryClass).joinToString(", ")}")
            }
        } else {
            println("   ⚠️  AppModule.kt not found — skipping provider binding swap")
        }

        // 2. Delete provider .kt files that were NOT selected
        val providersDir = findDir(projectDir, "providers")
        if (providersDir != null) {
            for (unused in unusedProviders) {
                val file = File(providersDir, "$unused.kt")
                if (file.exists()) {
                    file.delete()
                    println("   Deleted unused provider: ${file.name}")
                }
            }
        } else {
            println("   ⚠️  providers/ directory not found under ${projectDir.absolutePath}")
        }

        // 3. Strip BuildConfig fields for providers that were NOT selected
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
