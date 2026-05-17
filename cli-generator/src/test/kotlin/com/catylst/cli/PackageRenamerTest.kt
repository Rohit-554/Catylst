package com.catylst.cli

import com.catylst.cli.model.GeneratorConfig
import com.catylst.cli.template.PackageRenamer
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackageRenamerTest {

    @Test
    fun `test package replacement in kotlin file`() {
        val tempDir = createTempDir("test-rename")
        val ktFile = File(tempDir, "Test.kt")
        ktFile.writeText("package io.jadu.catylst\nimport io.jadu.catylst.ai.AiProvider")

        val config = GeneratorConfig(
            packageName = "com.example.myapp",
            appName = "MyApp",
            projectName = "MyApp",
            features = setOf("ai", "ktor"),
            sampleCode = true,
            aiProvider = com.catylst.cli.model.AiProvider.CLAUDE,
            outputDir = tempDir
        )

        PackageRenamer.rename(tempDir, config)

        val content = ktFile.readText()
        assertTrue(content.contains("package com.example.myapp"))
        assertTrue(content.contains("import com.example.myapp.ai.AiProvider"))
        assertTrue(!content.contains("io.jadu.catylst"))

        tempDir.deleteRecursively()
    }

    @Test
    fun `test app name replacement`() {
        val tempDir = createTempDir("test-rename")
        val stringsFile = File(tempDir, "strings.xml")
        stringsFile.writeText("<string name=\"app_name\">Catylst</string>")

        val config = GeneratorConfig(
            packageName = "com.example.myapp",
            appName = "MyAwesomeApp",
            projectName = "MyAwesomeApp",
            features = setOf(),
            sampleCode = true,
            aiProvider = com.catylst.cli.model.AiProvider.NONE,
            outputDir = tempDir
        )

        PackageRenamer.rename(tempDir, config)

        val content = stringsFile.readText()
        assertTrue(content.contains("MyAwesomeApp"))
        assertTrue(!content.contains("Catylst"))

        tempDir.deleteRecursively()
    }
}
