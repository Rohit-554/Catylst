package com.catylst.cli

import com.catylst.cli.model.FeatureDef
import com.catylst.cli.model.GeneratorConfig
import com.catylst.cli.template.FeatureRemover
import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureRemoverTest {

    @Test
    fun `test remove AI feature removes koin bindings`() {
        val tempDir = createTempDir("test-remove")
        val appModule = File(tempDir, "AppModule.kt")
        appModule.writeText(
            """
            package com.example.app.di
            import com.example.app.ai.AiProvider
            import com.example.app.ai.AiRepository
            import com.example.app.ai.providers.ClaudeProvider
            
            fun appModule() = module {
                single<HttpClient> { createHttpClient() }
                single { ApiService(get()) }
                single<AppDatabase> { createAppDatabase() }
                single<AiProvider> { ClaudeProvider(get(), "key") }
                single { AiRepository(get()) }
                viewModel { HomeViewModel() }
                viewModel { AiViewModel(get()) }
            }
            """.trimIndent()
        )

        val config = GeneratorConfig(
            packageName = "com.example.app",
            appName = "MyApp",
            projectName = "MyApp",
            features = setOf("ktor", "room"),
            sampleCode = true,
            aiProvider = com.catylst.cli.model.AiProvider.NONE,
            outputDir = tempDir
        )

        val aiFeature = FeatureDef(
            id = "ai",
            name = "AI Integration",
            description = "AI chat",
            default = true,
            requires = listOf("ktor"),
            files = emptyList(),
            gradleDeps = emptyList(),
            kspProcessors = emptyList(),
            tomlVersionKeys = emptyList(),
            tomlPluginKeys = emptyList(),
            koinBindings = listOf("AiProvider", "AiRepository"),
            platformBindings = emptyMap(),
            screens = listOf("AiDemo"),
            buildConfigFields = listOf("CLAUDE_API_KEY", "GROQ_API_KEY", "GEMINI_API_KEY"),
            iosInfoPlistKeys = emptyList()
        )

        FeatureRemover.removeFeatures(tempDir, config, listOf(aiFeature))

        val content = appModule.readText()
        assertFalse(content.contains("single<AiProvider>"))
        assertFalse(content.contains("single { AiRepository"))
        assertTrue(content.contains("single<HttpClient>"))
        assertTrue(content.contains("single<AppDatabase>"))

        tempDir.deleteRecursively()
    }
}
