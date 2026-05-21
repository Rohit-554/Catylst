package com.catylst.cli.template

import com.catylst.cli.model.PermissionTypeDef
import java.io.File

/**
 * Strips unused permission types from the generated project when the user selects
 * only a subset of the available permissions.
 *
 * Touches:
 *  - Permission.kt        — removes unused enum entries
 *  - Info.plist           — removes unused NSXxxUsageDescription keys
 *  - LocationPermissionDelegate.kt — deleted if LOCATION is not selected
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

        // 1. Remove enum entries from Permission.kt
        stripPermissionEnum(projectDir, removed.map { it.id })

        // 2. Remove iOS Info.plist keys for removed permissions
        val plistKeys = removed.mapNotNull { it.iosInfoPlistKey }
        if (plistKeys.isNotEmpty()) {
            stripInfoPlistKeys(projectDir, plistKeys)
        }

        // 3. Delete LocationPermissionDelegate.kt if LOCATION is not selected
        if ("LOCATION" !in selectedIds) {
            val delegate = findFile(projectDir, "LocationPermissionDelegate.kt")
            if (delegate != null) {
                delegate.delete()
                println("   Deleted: LocationPermissionDelegate.kt")
            }
        }

        // 4. Remove unused cases from Android PermissionController.toManifestString()
        stripAndroidManifestCases(projectDir, removed.map { it.id })

        // 5. Remove unselected <uses-permission> tags from AndroidManifest.xml
        stripAndroidManifestPermissions(projectDir, removed.map { it.id })
    }

    /**
     * Strips <uses-permission> lines from AndroidManifest.xml for permissions that
     * were not selected by the user.
     *
     * Mapping of permission ID → AndroidManifest permission name(s):
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
            "CAMERA"       to listOf("android.permission.CAMERA"),
            "LOCATION"     to listOf("android.permission.ACCESS_FINE_LOCATION"),
            "RECORD_AUDIO" to listOf("android.permission.RECORD_AUDIO"),
            "STORAGE"      to listOf("android.permission.READ_EXTERNAL_STORAGE"),
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
            if (line.contains("<uses-permission")) {
                val matchesRemoved = permissionsToRemove.any { line.contains(it) }
                if (matchesRemoved) {
                    toRemove.add(i)
                    // Handle multi-line <uses-permission> tags (e.g. with android:maxSdkVersion on next line)
                    var j = i
                    while (j < lines.size && !lines[j].trimEnd().endsWith("/>")) {
                        toRemove.add(j)
                        j++
                    }
                    if (j < lines.size) toRemove.add(j) // closing />
                    i = j + 1
                    continue
                }
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
                // Match lines like: Permission.CAMERA -> Manifest.permission.CAMERA
                if (lines[i].contains("Permission.$id ->")) {
                    toRemove.add(i)
                    // Also remove continuation lines (multi-line when/if blocks)
                    var j = i + 1
                    while (j < lines.size && lines[j].trimStart().startsWith("Manifest.") ||
                           (j < lines.size && lines[j].trimStart().startsWith("if (") && toRemove.contains(i))) {
                        toRemove.add(j)
                        j++
                        if (j < lines.size && lines[j-1].contains("else null")) break
                    }
                }
                // Match import lines for removed permissions if any
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
