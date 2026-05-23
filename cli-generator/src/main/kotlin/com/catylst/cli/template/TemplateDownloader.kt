package com.catylst.cli.template

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.util.zip.ZipInputStream

object TemplateDownloader {

    private const val GITHUB_API_URL =
        "https://api.github.com/repos/Rohit-554/Catylst/releases/latest"

    fun downloadAndExtract(templateZipUrl: String? = null, bundledZip: File? = null, localProjectDir: File? = null): File {
        // If local project dir is provided, use it directly (for CLI bundled with repo)
        if (localProjectDir != null && localProjectDir.exists()) {
            println("📁 Using local template: ${localProjectDir.absolutePath}")
            return localProjectDir
        }

        val tempDir = Files.createTempDirectory("catylst-template-").toFile()

        val zipFile = if (templateZipUrl != null) {
            downloadZip(templateZipUrl, tempDir)
        } else {
            try {
                val latestUrl = fetchLatestReleaseAssetUrl()
                downloadZip(latestUrl, tempDir)
            } catch (e: Exception) {
                println("⚠️  Failed to fetch from GitHub (${e.message}). Falling back to bundled template...")
                bundledZip ?: error("No bundled template available and network fetch failed.")
                bundledZip
            }
        }

        val extractDir = File(tempDir, "extracted")
        extractDir.mkdirs()
        extractZip(zipFile, extractDir)

        return findProjectRoot(extractDir)
    }

    private fun fetchLatestReleaseAssetUrl(): String {
        val conn = URL(GITHUB_API_URL).openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        conn.setRequestProperty("User-Agent", "Catylst-CLI")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val assetsRegex = """"browser_download_url"\s*:\s*"([^"]+catylst-template[^"]*)"""".toRegex()
        val match = assetsRegex.find(response)
            ?: error("No catylst-template asset found in latest release")
        return match.groupValues[1]
    }

    private fun downloadZip(url: String, destDir: File): File {
        println("📥 Downloading template from $url ...")
        val destFile = File(destDir, "template.zip")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 30000
        conn.readTimeout = 60000
        conn.inputStream.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        conn.disconnect()
        println("✅ Downloaded ${destFile.length() / 1024} KB")
        return destFile
    }

    private fun extractZip(zipFile: File, destDir: File) {
        val canonicalDest = destDir.canonicalFile
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name).canonicalFile
                require(outFile.startsWith(canonicalDest)) {
                    "Zip slip blocked: ${entry.name}"
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { zis.copyTo(it) }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun findProjectRoot(dir: File): File {
        val candidates = dir.listFiles { f -> f.isDirectory } ?: emptyArray()
        return candidates.firstOrNull { File(it, "settings.gradle.kts").exists() }
            ?: candidates.firstOrNull { File(it, "build.gradle.kts").exists() }
            ?: dir
    }
}
