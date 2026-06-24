package com.catylst.plugin.template

import com.catylst.plugin.model.FeatureDef
import com.catylst.plugin.model.GeneratorConfig
import java.io.File

object FeatureRemover {

    fun removeFeatures(
        projectDir: File,
        config: GeneratorConfig,
        manifestFeatures: List<FeatureDef>,
        sampleCodeDef: com.catylst.plugin.model.SampleCodeDef? = null
    ) {
        val unselected = manifestFeatures.filter { it.id !in config.features }

        for (feature in unselected) {
            println("🗑️  Removing feature: ${feature.name}")

            for (pattern in feature.files) {
                val resolved = pattern.replace("{pkg}", config.packagePath)
                val target = File(projectDir, resolved)
                if (target.exists()) {
                    target.deleteRecursively()
                    println("   Deleted: ${target.relativeTo(projectDir)}")
                }
            }

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

            removeKoinBindings(projectDir, config, feature.koinBindings)

            if (feature.id == "ai") {
                removeAiSpecificCode(projectDir)
            }

            for ((platform, bindings) in feature.platformBindings) {
                removePlatformBindings(projectDir, config, platform, bindings)
            }

            if (feature.id == "preferences") {
                removePreferencesPlatformImports(projectDir)
            }

            if (feature.id == "notifications") {
                removeNotificationsPlatformImports(projectDir)
                removeNotificationsManifestEntries(projectDir)
            }

            if (feature.id == "permissions") {
                // FeatureRemover deletes the permission code, but the <uses-permission> tags live
                // inline in AndroidManifest.xml and must be stripped here. NOTIFICATIONS tags are
                // owned by the notifications feature, so leave them to removeNotificationsManifestEntries.
                val ids = feature.permissionTypes.map { it.id }.filter { it != "NOTIFICATIONS" }
                PermissionStripper.stripAndroidManifestPermissions(projectDir, ids)
            }

            removeGradleDeps(projectDir, feature.gradleDeps)
            removeKspProcessors(projectDir, feature.kspProcessors)
            removePluginAliases(projectDir, feature.tomlPluginKeys)

            if (feature.id == "room") {
                removeKspRoomBlock(projectDir)
            }

            removeBuildConfigFields(projectDir, feature.buildConfigFields)
            removeInfoPlistKeys(projectDir, feature.iosInfoPlistKeys)
            removeSettingsIncludes(projectDir, feature.settingsInclude)
        }

        pruneEmptyDirs(projectDir)
    }

    private fun pruneEmptyDirs(root: File) {
        root.walkBottomUp()
            .filter { it.isDirectory && it != root }
            .forEach { dir ->
                if (dir.exists() && dir.walkTopDown().none { it.isFile }) {
                    dir.deleteRecursively()
                }
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
                if (line.contains(Regex("import .*$binding"))) {
                    toRemove.add(i)
                }
                if (line.contains(Regex("single<\\s*$binding\\s*>")) ||
                    line.contains(Regex("single\\s*\\{\\s*$binding")) ||
                    line.contains(Regex("viewModel\\s*\\{\\s*$binding"))
                ) {
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
        newContent = newContent.replace(Regex("/\\*.*?AI PROVIDER.*?\\*/\\s*\\n?", RegexOption.DOT_MATCHES_ALL), "")
        appModule.writeText(newContent)
    }

    private fun removeGradleDeps(projectDir: File, deps: List<String>) {
        if (deps.isEmpty()) return
        val buildFile = File(projectDir, "composeApp/build.gradle.kts")
        if (!buildFile.exists()) return
        val lines = buildFile.readText().lines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            for (dep in deps) {
                // kebab-case dep IDs are referenced as dot-notation in Gradle: room-runtime -> room.runtime
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
        val buildFiles = listOf(
            File(projectDir, "composeApp/build.gradle.kts"),
            File(projectDir, "build.gradle.kts")
        )
        for (buildFile in buildFiles) {
            if (!buildFile.exists()) continue
            val lines = buildFile.readText().lines().toMutableList()
            val toRemove = mutableSetOf<Int>()
            for (i in lines.indices) {
                for (key in pluginKeys) {
                    if (lines[i].contains("alias(libs.plugins.$key)")) {
                        toRemove.add(i)
                    }
                }
            }
            if (toRemove.isNotEmpty()) {
                buildFile.writeText(lines.filterIndexed { i, _ -> i !in toRemove }.joinToString("\n"))
            }
        }
    }

    private fun removeKspRoomBlock(projectDir: File) {
        val buildFile = File(projectDir, "composeApp/build.gradle.kts")
        if (!buildFile.exists()) return
        val lines = buildFile.readText().lines().toMutableList()
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
        val lines = buildFile.readText().lines().toMutableList()
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
        val lines = plistFile.readText().lines().toMutableList()
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

    /**
     * Removes WorkManager-related entries from AndroidManifest.xml:
     *  - POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED permissions
     *  - The InitializationProvider `<provider>` block used by WorkManager
     */
    private fun removeNotificationsManifestEntries(projectDir: File) {
        val manifestFile = findFile(projectDir, "AndroidManifest.xml") ?: return
        val lines = manifestFile.readLines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        val notificationPermissions = listOf(
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.RECEIVE_BOOT_COMPLETED"
        )

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            if (line.contains("<uses-permission") && notificationPermissions.any { line.contains(it) }) {
                toRemove.add(i)
                var j = i
                while (j < lines.size && !lines[j].trimEnd().endsWith("/>")) {
                    toRemove.add(j)
                    j++
                }
                if (j < lines.size) toRemove.add(j)
                i = j + 1
                continue
            }

            if (line.contains("InitializationProvider")) {
                // Find the opening <provider tag (may be on a preceding line)
                var start = i
                while (start > 0 && !lines[start].trimStart().startsWith("<provider")) start--
                var end = i
                while (end < lines.size && !lines[end].contains("</provider>")) end++
                for (k in start..end) toRemove.add(k)
                i = end + 1
                continue
            }

            i++
        }

        if (toRemove.isNotEmpty()) {
            manifestFile.writeText(lines.filterIndexed { idx, _ -> idx !in toRemove }.joinToString("\n"))
            println("   Stripped AndroidManifest.xml WorkManager/notification entries")
        }
    }

    private fun removeSettingsIncludes(projectDir: File, includes: List<String>) {
        if (includes.isEmpty()) return
        val settingsFile = File(projectDir, "settings.gradle.kts")
        if (!settingsFile.exists()) return
        val lines = settingsFile.readText().lines().toMutableList()
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
}
