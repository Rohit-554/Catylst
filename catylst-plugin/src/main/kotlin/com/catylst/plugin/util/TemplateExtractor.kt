package com.catylst.plugin.util

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipInputStream

object TemplateExtractor {

    fun extract(): File {
        val zip = TemplateExtractor::class.java
            .getResourceAsStream("/templates/catylst-template.zip")
            ?: error("Bundled template not found at /templates/catylst-template.zip")

        val tempDir = Files.createTempDirectory("catylst-template").toFile()

        ZipInputStream(zip.buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val target = File(tempDir, entry.name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        return tempDir
    }
}
