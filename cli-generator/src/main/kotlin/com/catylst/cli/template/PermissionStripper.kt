package com.catylst.cli.template

import com.catylst.cli.model.PermissionTypeDef
import java.io.File

/**
 * Strips unused permission types from the generated project when the user selects
 * only a subset of the available permissions.
 *
 * Touches:
 *  - Permission.kt                  — removes unused enum entries
 *  - Info.plist                     — removes unused NSXxxUsageDescription keys
 *  - AndroidManifest.xml            — removes unused <uses-permission> tags
 *  - LocationPermissionDelegate.kt  — deleted if LOCATION is not selected
 */
object PermissionStripper {

    fun apply(
        projectDir: File,
        allPermissions: List<PermissionTypeDef>,
        selectedIds: Set<String>
    ) {
        val removed = allPermissions.filter { it.id !in selectedIds }
        if (removed.isEmpty()) return

        println("✂️  Stripping unused permissions: ${removed.joinToString(", ") { it.id }}")

        stripPermissionEnum(projectDir, removed.map { it.id })

        val plistKeys = removed.mapNotNull { it.iosInfoPlistKey }
        if (plistKeys.isNotEmpty()) {
            stripInfoPlistKeys(projectDir, plistKeys)
        }

        if ("LOCATION" !in selectedIds) {
            val delegate = findFile(projectDir, "LocationPermissionDelegate.kt")
            if (delegate != null) {
                delegate.delete()
                println("   Deleted: LocationPermissionDelegate.kt")
            }
        }

        stripAndroidManifestCases(projectDir, removed.map { it.id })
        stripAndroidManifestPermissions(projectDir, removed.map { it.id })
    }

    /**
     * Removes `<uses-permission>` tags from AndroidManifest.xml for the given permission IDs.
     *
     * Permission ID → AndroidManifest permission name mapping:
     *   CAMERA        → android.permission.CAMERA
     *   LOCATION      → android.permission.ACCESS_FINE_LOCATION
     *   RECORD_AUDIO  → android.permission.RECORD_AUDIO
     *   STORAGE       → android.permission.READ_EXTERNAL_STORAGE
     *   NOTIFICATIONS → android.permission.POST_NOTIFICATIONS,
     *                   android.permission.SCHEDULE_EXACT_ALARM,
     *                   android.permission.RECEIVE_BOOT_COMPLETED
     */
    fun stripAndroidManifestPermissions(projectDir: File, removedIds: List<String>) {
        val manifestFile = findFile(projectDir, "AndroidManifest.xml") ?: return

        val permissionMap = mapOf(
            "CAMERA"        to listOf("android.permission.CAMERA"),
            "LOCATION"      to listOf("android.permission.ACCESS_FINE_LOCATION"),
            "RECORD_AUDIO"  to listOf("android.permission.RECORD_AUDIO"),
            "STORAGE"       to listOf("android.permission.READ_EXTERNAL_STORAGE"),
            "NOTIFICATIONS" to listOf(
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.SCHEDULE_EXACT_ALARM",
                "android.permission.RECEIVE_BOOT_COMPLETED"
            )
        )

        val permissionsToRemove = removedIds.flatMap { permissionMap[it] ?: emptyList() }
        if (permissionsToRemove.isEmpty()) return

        val lines = manifestFile.readLines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.contains("<uses-permission") && permissionsToRemove.any { line.contains(it) }) {
                toRemove.add(i)
                // Handle multi-line tags (e.g. android:maxSdkVersion on a continuation line)
                var j = i
                while (j < lines.size && !lines[j].trimEnd().endsWith("/>")) {
                    toRemove.add(j)
                    j++
                }
                if (j < lines.size) toRemove.add(j)
                i = j + 1
                continue
            }
            i++
        }

        if (toRemove.isNotEmpty()) {
            manifestFile.writeText(lines.filterIndexed { idx, _ -> idx !in toRemove }.joinToString("\n"))
            println("   Stripped AndroidManifest.xml permissions: ${permissionsToRemove.joinToString(", ")}")
        }
    }

    private fun stripPermissionEnum(projectDir: File, removedIds: List<String>) {
        val permissionFile = findFile(projectDir, "Permission.kt") ?: return
        val lines = permissionFile.readLines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            val trimmed = lines[i].trim().trimEnd(',')
            if (trimmed in removedIds) {
                toRemove.add(i)
            }
        }

        if (toRemove.isNotEmpty()) {
            permissionFile.writeText(lines.filterIndexed { i, _ -> i !in toRemove }.joinToString("\n"))
            println("   Stripped Permission.kt entries: ${removedIds.joinToString(", ")}")
        }
    }

    private fun stripInfoPlistKeys(projectDir: File, keys: List<String>) {
        val plist = findFile(projectDir, "Info.plist") ?: return
        val lines = plist.readLines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            for (key in keys) {
                if (lines[i].contains("<key>$key</key>")) {
                    toRemove.add(i)
                    if (i + 1 < lines.size) toRemove.add(i + 1) // remove the <string> value too
                }
            }
        }

        if (toRemove.isNotEmpty()) {
            plist.writeText(lines.filterIndexed { i, _ -> i !in toRemove }.joinToString("\n"))
            println("   Stripped Info.plist keys: ${keys.joinToString(", ")}")
        }
    }

    private fun stripAndroidManifestCases(projectDir: File, removedIds: List<String>) {
        val androidController = findFile(projectDir, "PermissionController.android.kt") ?: return
        val lines = androidController.readLines().toMutableList()
        val toRemove = mutableSetOf<Int>()

        for (i in lines.indices) {
            for (id in removedIds) {
                if (lines[i].contains("Permission.$id ->")) {
                    toRemove.add(i)
                    var j = i + 1
                    while (j < lines.size && lines[j].trimStart().startsWith("Manifest.") ||
                           (j < lines.size && lines[j].trimStart().startsWith("if (") && toRemove.contains(i))) {
                        toRemove.add(j)
                        j++
                        if (j < lines.size && lines[j-1].contains("else null")) break
                    }
                }
                if (lines[i].contains("import android.Manifest") && removedIds.size == 5) {
                    toRemove.add(i) // all permissions removed — shouldn't happen but guard it
                }
            }
        }

        if (toRemove.isNotEmpty()) {
            androidController.writeText(lines.filterIndexed { i, _ -> i !in toRemove }.joinToString("\n"))
        }
    }
}
