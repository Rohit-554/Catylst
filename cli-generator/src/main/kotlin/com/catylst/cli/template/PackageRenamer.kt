package com.catylst.cli.template

import com.catylst.cli.model.GeneratorConfig
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object PackageRenamer {

    private val TEXT_EXTENSIONS = setOf(
        "kt", "kts", "xml", "plist", "pbxproj", "xcconfig", "swift", "properties", "md", "sh", "json", "gradle"
    )

    fun rename(projectDir: File, config: GeneratorConfig) {
        val oldPackage = "io.jadu.catylst"
        val oldPackagePath = oldPackage.replace(".", "/")
        val newPackage = config.packageName
        val newPackagePath = config.packagePath
        val oldAppName = "Catylst"
        val newAppName = config.appName
        val oldProjectName = "Catylst"
        val newProjectName = config.projectName

        // 1. Replace text content in all source files
        projectDir.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val ext = file.extension.lowercase()
                if (ext in TEXT_EXTENSIONS || file.name == "build.gradle" || file.name.endsWith(".gradle.kts")) {
                    var content = file.readText()
                    var changed = false

                    if (content.contains(oldPackage)) {
                        content = content.replace(oldPackage, newPackage)
                        changed = true
                    }
                    if (content.contains(oldAppName)) {
                        content = content.replace(oldAppName, newAppName)
                        changed = true
                    }
                    if (content.contains(oldProjectName)) {
                        content = content.replace(oldProjectName, newProjectName)
                        changed = true
                    }

                    if (changed) {
                        file.writeText(content)
                    }
                }
            }

        // 2. Rename directory trees: io/jadu/catylst -> com/company/app
        // Collect all directories that contain the old package path
        val dirsToRename = mutableListOf<Pair<File, File>>()
        projectDir.walkTopDown()
            .filter { it.isDirectory }
            .forEach { dir ->
                val relative = dir.relativeTo(projectDir).invariantSeparatorsPath
                if (relative.contains(oldPackagePath)) {
                    val newRelative = relative.replace(oldPackagePath, newPackagePath)
                    val newDir = File(projectDir, newRelative)
                    dirsToRename.add(dir to newDir)
                }
            }

        // Sort by depth (longest path first) to avoid conflicts during rename
        dirsToRename.sortByDescending { it.first.absolutePath.length }

        for ((oldDir, newDir) in dirsToRename) {
            if (oldDir.exists() && oldDir != newDir) {
                newDir.parentFile?.mkdirs()
                // If target exists, merge contents
                if (newDir.exists()) {
                    mergeDirectories(oldDir, newDir)
                    oldDir.deleteRecursively()
                } else {
                    oldDir.renameTo(newDir)
                }
            }
        }

        // 3. Update settings.gradle.kts rootProject.name
        val settingsFile = File(projectDir, "settings.gradle.kts")
        if (settingsFile.exists()) {
            var content = settingsFile.readText()
            val regex = """rootProject\.name\s*=\s*"[^"]+"""".toRegex()
            content = regex.replace(content, """rootProject.name = "$newProjectName"""")
            settingsFile.writeText(content)
        }
    }

    private fun mergeDirectories(source: File, target: File) {
        source.listFiles()?.forEach { file ->
            val dest = File(target, file.name)
            if (file.isDirectory) {
                if (!dest.exists()) dest.mkdirs()
                mergeDirectories(file, dest)
            } else {
                if (!dest.exists()) {
                    Files.move(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}
