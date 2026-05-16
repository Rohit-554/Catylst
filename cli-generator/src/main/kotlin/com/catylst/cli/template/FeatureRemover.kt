package com.catylst.cli.template

import com.catylst.cli.model.FeatureDef
import com.catylst.cli.model.GeneratorConfig
import java.io.File

object FeatureRemover {

    fun removeFeatures(
        projectDir: File,
        config: GeneratorConfig,
        manifestFeatures: List<FeatureDef>,
        sampleCodeDef: com.catylst.cli.model.SampleCodeDef? = null
    ) {
        val unselected = manifestFeatures.filter { it.id !in config.features }

        for (feature in unselected) {
            println("🗑️  Removing feature: ${feature.name}")

            // 1a. Delete core files
            for (pattern in feature.files) {
                val resolved = pattern.replace("{pkg}", config.packagePath)
                val target = File(projectDir, resolved)
                if (target.exists()) {
                    target.deleteRecursively()
                    println("   Deleted: ${target.relativeTo(projectDir)}")
                }
            }

            // 1b. Delete sample files for this feature (if feature unselected, sample code goes too)
            if (sampleCodeDef != null) {
                val sampleDef = sampleCodeDef.perFeature[feature.id]
                if (sampleDef != null) {
                    for (pattern in sampleDef.files) {
                        val resolved = pattern.replace("{pkg}", config.packagePath)
                        val target = File(projectDir, resolved)
                        if (target.exists()) {
                            target.deleteRecursively()
                            println("   Deleted sample: ${target.relativeTo(projectDir)}")
                        }
                    }
                }
            }

            // 2. Remove Koin bindings from AppModule.kt
            removeKoinBindings(projectDir, config, feature.koinBindings)

            // 2b. Special cleanup for AI feature removal
            if (feature.id == "ai") {
                removeAiSpecificCode(projectDir)
            }

            // 3. Remove platform bindings
            for ((platform, bindings) in feature.platformBindings) {
                removePlatformBindings(projectDir, config, platform, bindings)
            }

            // 3b. Special cleanup for preferences removal
            if (feature.id == "preferences") {
                removePreferencesPlatformImports(projectDir)
            }

            // 3c. Special cleanup for notifications removal
            if (feature.id == "notifications") {
                removeNotificationsPlatformImports(projectDir)
            }

            // Navigation, Screen, and HomeScreen edits are handled holistically
            // after all features are processed

            // 7. Remove Gradle deps from composeApp/build.gradle.kts
            removeGradleDeps(projectDir, feature.gradleDeps)

            // 8. Remove KSP processors
            removeKspProcessors(projectDir, feature.kspProcessors)

            // 9. Remove plugin aliases
            removePluginAliases(projectDir, feature.tomlPluginKeys)

            // 10. Remove KSP room block if room removed
            if (feature.id == "room") {
                removeKspRoomBlock(projectDir)
            }

            // 11. Remove buildConfigFields from androidApp/build.gradle.kts
            removeBuildConfigFields(projectDir, feature.buildConfigFields)

            // 12. Remove Info.plist keys
            removeInfoPlistKeys(projectDir, feature.iosInfoPlistKeys)

            // 13. Remove settings includes
            removeSettingsIncludes(projectDir, feature.settingsInclude)
        }
    }

    private fun removeKoinBindings(projectDir: File, config: GeneratorConfig, bindings: List<String>) {
        if (bindings.isEmpty()) return
        val appModule = findFile(projectDir, "AppModule.kt") ?: return
        removeKoinBindingsFromFile(appModule, bindings)
    }

    private fun removePlatformBindings(
        projectDir: File,
        config: GeneratorConfig,
        platform: String,
        bindings: List<String>
    ) {
        if (bindings.isEmpty()) return
        val suffix = when (platform) {
            "android" -> "PlatformModule.android.kt"
            "ios" -> "PlatformModule.ios.kt"
            "desktop" -> "PlatformModule.jvm.kt"
            else -> return
        }
        val platformModule = findFile(projectDir, suffix) ?: return
        removeKoinBindingsFromFile(platformModule, bindings)
    }

    private fun removeKoinBindingsFromFile(file: File, bindings: List<String>) {
        val lines = file.readText().lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            val line = lines[i]
            for (binding in bindings) {
                // Remove import lines
                if (line.contains(Regex("import .*$binding"))) {
                    toRemove.add(i)
                }
                // Remove binding lines (and their multi-line blocks)
                if (line.contains(Regex("single<\\s*$binding\\s*>")) ||
                    line.contains(Regex("single\\s*\\{\\s*$binding")) ||
                    line.contains(Regex("viewModel\\s*\\{\\s*$binding"))
                ) {
                    // Remove from this line to the end of the balanced brace block
                    var braceCount = 0
                    var foundOpen = false
                    for (k in i until lines.size) {
                        toRemove.add(k)
                        if (lines[k].contains("{")) {
                            braceCount += lines[k].count { it == '{' }
                            foundOpen = true
                        }
                        if (lines[k].contains("}")) {
                            braceCount -= lines[k].count { it == '}' }
                        }
                        if (foundOpen && braceCount <= 0) break
                    }
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        file.writeText(result.joinToString("\n"))
    }

    private fun removePreferencesPlatformImports(projectDir: File) {
        val suffixes = listOf("PlatformModule.android.kt", "PlatformModule.ios.kt", "PlatformModule.jvm.kt")
        for (suffix in suffixes) {
            val file = findFile(projectDir, suffix) ?: continue
            val lines = file.readText().lines().toMutableList()
            val toRemove = mutableSetOf<Int>()
            for (i in lines.indices) {
                if (lines[i].contains("com.russhwolf.settings")) {
                    toRemove.add(i)
                }
            }
            val result = lines.filterIndexed { i, _ -> i !in toRemove }
            file.writeText(result.joinToString("\n"))
        }
    }

    private fun removeNotificationsPlatformImports(projectDir: File) {
        val suffixes = listOf("PlatformModule.android.kt", "PlatformModule.ios.kt", "PlatformModule.jvm.kt")
        for (suffix in suffixes) {
            val file = findFile(projectDir, suffix) ?: continue
            val lines = file.readText().lines().toMutableList()
            val toRemove = mutableSetOf<Int>()
            for (i in lines.indices) {
                if (lines[i].contains("NotificationScheduler")) {
                    toRemove.add(i)
                }
            }
            val result = lines.filterIndexed { i, _ -> i !in toRemove }
            file.writeText(result.joinToString("\n"))
        }
    }

    private fun removeAiSpecificCode(projectDir: File) {
        val appModule = findFile(projectDir, "AppModule.kt") ?: return
        val lines = appModule.readText().lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        val aiImports = listOf(
            "ClaudeProvider", "GroqProvider", "GeminiProvider",
            "AppConfig", "AiProvider", "AiRepository", "AiViewModel"
        )

        for (i in lines.indices) {
            for (import in aiImports) {
                if (lines[i].contains(Regex("import .*$import"))) {
                    toRemove.add(i)
                }
            }
            // Remove AiViewModel binding block
            if (lines[i].contains(Regex("viewModel\\s*\\{\\s*AiViewModel"))) {
                var braceCount = 0
                var foundOpen = false
                for (k in i until lines.size) {
                    toRemove.add(k)
                    if (lines[k].contains("{")) {
                        braceCount += lines[k].count { it == '{' }
                        foundOpen = true
                    }
                    if (lines[k].contains("}")) {
                        braceCount -= lines[k].count { it == '}' }
                    }
                    if (foundOpen && braceCount <= 0) break
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        var newContent = result.joinToString("\n")
        // Remove orphaned AI comment block
        newContent = newContent.replace(Regex("/\\*.*?AI PROVIDER.*?\\*/\\s*\\n?", RegexOption.DOT_MATCHES_ALL), "")
        appModule.writeText(newContent)
    }

    private fun removeScreens(projectDir: File, config: GeneratorConfig, screens: List<String>) {
        if (screens.isEmpty()) return
        val screenFile = findFile(projectDir, "Screen.kt") ?: return
        var content = screenFile.readText()
        val lines = content.lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            val line = lines[i]
            for (screen in screens) {
                if (line.contains("data object $screen") || line.contains("data class $screen")) {
                    toRemove.add(i)
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        screenFile.writeText(result.joinToString("\n"))
    }

    private fun removeNavigationWiring(projectDir: File, config: GeneratorConfig, screens: List<String>) {
        if (screens.isEmpty()) return
        val navFile = findFile(projectDir, "AppNavigation.kt") ?: return
        var content = navFile.readText()
        val lines = content.lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            val line = lines[i]
            for (screen in screens) {
                if (line.contains("subclass(Screen\\.$screen::class)".toRegex()) ||
                    line.contains("is Screen\\.$screen ->".toRegex()) ||
                    line.contains(Regex("onNavigateTo${Regex.escape(screen)}")) ||
                    line.contains(Regex("${Regex.escape(screen)}Screen\\(")) ||
                    line.contains(Regex("import .*${Regex.escape(screen)}Screen"))
                ) {
                    toRemove.add(i)
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        navFile.writeText(result.joinToString("\n"))
    }

    private fun removeHomeButtons(projectDir: File, config: GeneratorConfig, feature: FeatureDef) {
        val homeScreen = findFile(projectDir, "HomeScreen.kt") ?: return
        var content = homeScreen.readText()
        val lines = content.lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        // Remove lambda params for navigation
        for (screen in feature.screens) {
            val paramName = "onNavigateTo$screen"
            for (i in lines.indices) {
                if (lines[i].contains(paramName)) {
                    toRemove.add(i)
                }
            }
        }

        // Remove OutlinedButton blocks for demo screens
        for (i in lines.indices) {
            val line = lines[i]
            for (screen in feature.screens) {
                val buttonText = when (screen) {
                    "Detail" -> "Go to Detail"
                    "Permissions" -> "Permissions Demo"
                    "Notifications" -> "Notifications Demo"
                    "Preferences" -> "Preferences Demo"
                    "AiDemo" -> "AI Demo"
                    else -> screen
                }
                if (line.contains(buttonText)) {
                    // Find the start of the OutlinedButton block (up to 5 lines back)
                    for (j in maxOf(0, i - 5)..i) {
                        if (lines[j].contains("OutlinedButton")) {
                            // Mark from j to the closing brace
                            var braceCount = 0
                            var foundOpen = false
                            for (k in j until lines.size) {
                                toRemove.add(k)
                                if (lines[k].contains("{")) {
                                    braceCount += lines[k].count { it == '{' }
                                    foundOpen = true
                                }
                                if (lines[k].contains("}")) {
                                    braceCount -= lines[k].count { it == '}' }
                                }
                                if (foundOpen && braceCount <= 0) break
                            }
                            break
                        }
                    }
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        homeScreen.writeText(result.joinToString("\n"))
    }

    private fun removeGradleDeps(projectDir: File, deps: List<String>) {
        if (deps.isEmpty()) return
        val buildFile = File(projectDir, "composeApp/build.gradle.kts")
        if (!buildFile.exists()) return
        val lines = buildFile.readText().lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            for (dep in deps) {
                // Convert kebab-case to camelCase/dot notation: room-runtime -> room.runtime
                val libRef = dep.replace("-", ".")
                if (lines[i].contains("implementation(libs.$libRef)")) {
                    toRemove.add(i)
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        buildFile.writeText(result.joinToString("\n"))
    }

    private fun removeKspProcessors(projectDir: File, processors: List<String>) {
        if (processors.isEmpty()) return
        val buildFile = File(projectDir, "composeApp/build.gradle.kts")
        if (!buildFile.exists()) return
        val lines = buildFile.readText().lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            for (proc in processors) {
                val libRef = proc.replace("-", ".")
                if (lines[i].contains("add(\"ksp")) {
                    if (lines[i].contains("libs.$libRef")) {
                        toRemove.add(i)
                    }
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        buildFile.writeText(result.joinToString("\n"))
    }

    private fun removePluginAliases(projectDir: File, pluginKeys: List<String>) {
        if (pluginKeys.isEmpty()) return
        val buildFile = File(projectDir, "composeApp/build.gradle.kts")
        if (!buildFile.exists()) return
        var content = buildFile.readText()
        val lines = content.lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            for (key in pluginKeys) {
                if (lines[i].contains("alias(libs.plugins.$key)")) {
                    toRemove.add(i)
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        buildFile.writeText(result.joinToString("\n"))
    }

    private fun removeKspRoomBlock(projectDir: File) {
        val buildFile = File(projectDir, "composeApp/build.gradle.kts")
        if (!buildFile.exists()) return
        var content = buildFile.readText()
        val lines = content.lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            if (lines[i].contains("ksp {")) {
                var braceCount = 0
                var foundOpen = false
                for (k in i until lines.size) {
                    toRemove.add(k)
                    if (lines[k].contains("{")) {
                        braceCount += lines[k].count { it == '{' }
                        foundOpen = true
                    }
                    if (lines[k].contains("}")) {
                        braceCount -= lines[k].count { it == '}' }
                    }
                    if (foundOpen && braceCount <= 0) break
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        buildFile.writeText(result.joinToString("\n"))
    }

    private fun removeBuildConfigFields(projectDir: File, fields: List<String>) {
        if (fields.isEmpty()) return
        val buildFile = File(projectDir, "androidApp/build.gradle.kts")
        if (!buildFile.exists()) return
        var content = buildFile.readText()
        val lines = content.lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            for (field in fields) {
                if (lines[i].contains("buildConfigField.*$field".toRegex())) {
                    toRemove.add(i)
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        buildFile.writeText(result.joinToString("\n"))
    }

    private fun removeInfoPlistKeys(projectDir: File, keys: List<String>) {
        if (keys.isEmpty()) return
        val plistFile = findFile(projectDir, "Info.plist") ?: return
        var content = plistFile.readText()
        val lines = content.lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            for (key in keys) {
                if (lines[i].contains("<key>$key</key>")) {
                    toRemove.add(i)
                    toRemove.add(i + 1) // remove the <string> value line too
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        plistFile.writeText(result.joinToString("\n"))
    }

    private fun removeSettingsIncludes(projectDir: File, includes: List<String>) {
        if (includes.isEmpty()) return
        val settingsFile = File(projectDir, "settings.gradle.kts")
        if (!settingsFile.exists()) return
        var content = settingsFile.readText()
        val lines = content.lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            for (inc in includes) {
                if (lines[i].contains("include(\"$inc\")")) {
                    toRemove.add(i)
                }
            }
        }

        val result = lines.filterIndexed { i, _ -> i !in toRemove }
        settingsFile.writeText(result.joinToString("\n"))
    }

    private fun findFile(projectDir: File, name: String): File? {
        return projectDir.walkTopDown()
            .filter { it.isFile && it.name == name }
            .firstOrNull()
    }
}
