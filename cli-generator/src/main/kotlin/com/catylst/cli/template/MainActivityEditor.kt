package com.catylst.cli.template

import com.catylst.cli.model.AiProvider
import com.catylst.cli.model.GeneratorConfig
import java.io.File

object MainActivityEditor {

    fun apply(projectDir: File, config: GeneratorConfig) {
        val mainActivity = findFile(projectDir, "MainActivity.kt") ?: return
        var content = mainActivity.readText()

        // Remove unused BuildConfig key references
        when (config.aiProvider) {
            AiProvider.CLAUDE -> {
                content = content.replaceLine("AppConfig.groqApiKey\\s*=\\s*BuildConfig\\.GROQ_API_KEY".toRegex())
                content = content.replaceLine("AppConfig.geminiApiKey\\s*=\\s*BuildConfig\\.GEMINI_API_KEY".toRegex())
            }
            AiProvider.GROQ -> {
                content = content.replaceLine("AppConfig.claudeApiKey\\s*=\\s*BuildConfig\\.CLAUDE_API_KEY".toRegex())
                content = content.replaceLine("AppConfig.geminiApiKey\\s*=\\s*BuildConfig\\.GEMINI_API_KEY".toRegex())
            }
            AiProvider.GEMINI -> {
                content = content.replaceLine("AppConfig.claudeApiKey\\s*=\\s*BuildConfig\\.CLAUDE_API_KEY".toRegex())
                content = content.replaceLine("AppConfig.groqApiKey\\s*=\\s*BuildConfig\\.GROQ_API_KEY".toRegex())
            }
            AiProvider.NONE -> {
                content = content.replaceLine("AppConfig.claudeApiKey\\s*=\\s*BuildConfig\\.CLAUDE_API_KEY".toRegex())
                content = content.replaceLine("AppConfig.groqApiKey\\s*=\\s*BuildConfig\\.GROQ_API_KEY".toRegex())
                content = content.replaceLine("AppConfig.geminiApiKey\\s*=\\s*BuildConfig\\.GEMINI_API_KEY".toRegex())
                // Also remove the comment block about AI keys
                content = content.replaceBlock("// Inject AI provider keys".toRegex())
                // Remove AppConfig import when no AI
                if ("ai" !in config.features) {
                    content = content.replaceLine("import .*\\.config\\.AppConfig".toRegex())
                }
            }
        }

        mainActivity.writeText(content)
    }

    private fun String.replaceLine(regex: Regex): String {
        return this.lines().filterNot { regex.containsMatchIn(it) }.joinToString("\n")
    }

    private fun String.replaceBlock(triggerRegex: Regex): String {
        val lines = this.lines().toMutableList()
        val toRemove = mutableSetOf<Int>()
        for (i in lines.indices) {
            if (triggerRegex.containsMatchIn(lines[i])) {
                // Remove this line and following empty lines
                toRemove.add(i)
                var j = i + 1
                while (j < lines.size && lines[j].trim().isEmpty()) {
                    toRemove.add(j)
                    j++
                }
            }
        }
        return lines.filterIndexed { i, _ -> i !in toRemove }.joinToString("\n")
    }

    private fun findFile(projectDir: File, name: String): File? {
        return projectDir.walkTopDown()
            .filter { it.isFile && it.name == name }
            .firstOrNull()
    }
}
