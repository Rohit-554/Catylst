package com.catylst.cli.template

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Validates and copies font files (.ttf / .otf) into the project's composeResources/font/ directory.
 * Returns the resource ID (filename without extension) for each copied font, keyed by slot name.
 */
object FontCopier {

    private val ALLOWED_EXTENSIONS = setOf("ttf", "otf")

    /**
     * @param projectDir  Root of the generated project
     * @param fonts       Map of slot name → source font file path (null = skip slot)
     *                    Slots: "displayHeadline", "body", "labelTitle"
     * @return Map of slot name → resource ID (e.g., "inter_regular"), or empty if no fonts provided
     */
    fun copy(projectDir: File, fonts: Map<String, File?>): Map<String, String> {
        val provided = fonts.filterValues { it != null }.mapValues { it.value!! }
        if (provided.isEmpty()) return emptyMap()

        val fontDir = resolveFontDir(projectDir)
        fontDir.mkdirs()

        return provided.mapNotNull { (slot, srcFile) ->
            try {
                val resourceId = copyFont(srcFile, fontDir)
                println("   Font [$slot]: ${srcFile.name} → composeResources/font/${srcFile.name}")
                slot to resourceId
            } catch (e: IllegalArgumentException) {
                System.err.println("   ⚠️  Skipping font [$slot]: ${e.message}")
                null
            }
        }.toMap()
    }

    /**
     * Copy a single font file to the font directory after validating it.
     * Returns the resource ID (filename without extension, sanitized for Compose Resources).
     */
    private fun copyFont(src: File, fontDir: File): String {
        require(src.exists()) { "Font file not found: ${src.absolutePath}" }
        require(src.extension.lowercase() in ALLOWED_EXTENSIONS) {
            "Unsupported font format '${src.extension}'. Only .ttf and .otf are supported."
        }
        require(src.length() > 0) { "Font file is empty: ${src.name}" }
        // Basic magic-byte check for TTF (0x00010000) and OTF (OTTO)
        val magic = src.inputStream().use { it.readNBytes(4) }
        val isTtf = magic.contentEquals(byteArrayOf(0x00, 0x01, 0x00, 0x00))
        val isTrueTypeCollection = magic.contentEquals(byteArrayOf(0x74, 0x74, 0x63, 0x66)) // ttcf
        val isOtf = magic[0] == 'O'.code.toByte() && magic[1] == 'T'.code.toByte() &&
                magic[2] == 'T'.code.toByte() && magic[3] == 'O'.code.toByte()
        require(isTtf || isOtf || isTrueTypeCollection) {
            "File '${src.name}' does not appear to be a valid TTF or OTF font."
        }

        val dest = File(fontDir, src.name)
        Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return src.nameWithoutExtension.lowercase().replace(Regex("[^a-z0-9_]"), "_")
    }

    private fun resolveFontDir(projectDir: File): File {
        // Standard Compose Multiplatform resources location
        val candidates = listOf(
            File(projectDir, "composeApp/src/commonMain/composeResources/font"),
            File(projectDir, "composeApp/src/commonMain/resources/font")
        )
        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }
}
