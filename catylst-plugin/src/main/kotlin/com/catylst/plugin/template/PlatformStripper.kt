package com.catylst.plugin.template

import com.catylst.plugin.model.GeneratorConfig
import java.io.File

/**
 * Removes the source sets, modules and Gradle wiring for any platform the user did not select.
 *
 * Runs late in generation (after the per-platform editors) so those editors still see a full
 * tree; whatever is left for a deselected platform is then deleted here.
 */
object PlatformStripper {

    fun strip(projectDir: File, config: GeneratorConfig) {
        if (!config.includeAndroid) stripAndroid(projectDir)
        if (!config.includeIos) stripIos(projectDir)
        if (!config.includeDesktop) stripDesktop(projectDir)
    }

    private fun stripAndroid(projectDir: File) {
        File(projectDir, "composeApp/src/androidMain").deleteRecursively()
        File(projectDir, "androidApp").deleteRecursively()

        settingsFile(projectDir).editLines { lines ->
            lines.filterNot { it.trim() == "include(\":androidApp\")" }
        }

        composeBuildFile(projectDir).editLines { lines ->
            lines
                .removeBraceBlock { it.trim() == "android {" }
                .removeBraceBlock { it.trim().startsWith("androidMain.dependencies") }
                .filterNot { line ->
                    val t = line.trim()
                    t.startsWith("alias(libs.plugins.androidLibrary)") ||
                        t.startsWith("androidRuntimeClasspath(") ||
                        t.contains("\"kspAndroid\"")
                }
        }
    }

    private fun stripIos(projectDir: File) {
        File(projectDir, "composeApp/src/iosMain").deleteRecursively()
        File(projectDir, "iosApp").deleteRecursively()

        composeBuildFile(projectDir).editLines { lines ->
            lines
                .removeBraceBlock { it.trim().startsWith("listOf(") }
                .removeBraceBlock { it.trim().startsWith("iosMain.dependencies") }
                .filterNot { line ->
                    val t = line.trim()
                    t.contains("\"kspIosArm64\"") || t.contains("\"kspIosSimulatorArm64\"")
                }
        }
    }

    private fun stripDesktop(projectDir: File) {
        File(projectDir, "composeApp/src/desktopMain").deleteRecursively()

        composeBuildFile(projectDir).editLines { lines ->
            lines
                .removeBraceBlock { it.trim() == "compose.desktop {" }
                .removeBraceBlock { it.trim().startsWith("desktopMain.dependencies") }
                .filterNot { line ->
                    val t = line.trim()
                    t.startsWith("jvm(\"desktop\")") ||
                        t.startsWith("val desktopMain by getting")
                }
        }
    }

    private fun composeBuildFile(projectDir: File) = File(projectDir, "composeApp/build.gradle.kts")

    private fun settingsFile(projectDir: File) = File(projectDir, "settings.gradle.kts")

    private fun File.editLines(transform: (List<String>) -> List<String>) {
        if (!exists()) return
        writeText(transform(readText().lines()).joinToString("\n"))
    }

    /**
     * Drops a brace-balanced block whose opening line matches [start]. The block may begin a few
     * lines before the first `{` (e.g. a `listOf(...).forEach { }` target declaration): counting
     * starts at the matched line and the block ends when braces return to zero after the first `{`.
     * No-op if no line matches.
     */
    private fun List<String>.removeBraceBlock(start: (String) -> Boolean): List<String> {
        val startIdx = indexOfFirst(start)
        if (startIdx < 0) return this

        var depth = 0
        var seenOpen = false
        var endIdx = startIdx
        loop@ for (i in startIdx until size) {
            for (c in this[i]) {
                when (c) {
                    '{' -> { depth++; seenOpen = true }
                    '}' -> depth--
                }
            }
            endIdx = i
            if (seenOpen && depth <= 0) break@loop
        }
        return filterIndexed { i, _ -> i < startIdx || i > endIdx }
    }
}
