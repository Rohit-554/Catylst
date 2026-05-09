package io.jadu.catylst.di

import io.jadu.catylst.ai.AiProvider
import io.jadu.catylst.ai.AiRepository
import io.jadu.catylst.ai.providers.ClaudeProvider
import io.jadu.catylst.ai.providers.GeminiProvider
import io.jadu.catylst.ai.providers.GroqProvider
import io.jadu.catylst.config.AppConfig
import io.jadu.catylst.data.local.AppDatabase
import io.jadu.catylst.data.local.createAppDatabase
import io.jadu.catylst.data.preferences.AppPreferences
import io.jadu.catylst.network.ApiService
import io.jadu.catylst.network.createHttpClient
import io.jadu.catylst.ui.viewmodel.AiViewModel
import io.jadu.catylst.ui.viewmodel.HomeViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(): Module = module {
    single<HttpClient> { createHttpClient() }
    single { ApiService(get()) }

    single<AppDatabase> { createAppDatabase() }
    single { AppPreferences(get()) }

    /*
     * AI PROVIDER — change this ONE line to swap providers.
     * Add your API key to local.properties (see local.properties.example).
     *
     *   single<AiProvider> { ClaudeProvider(get(), AppConfig.claudeApiKey) }
     *   single<AiProvider> { GroqProvider(get(), AppConfig.groqApiKey)     }
     *   single<AiProvider> { GeminiProvider(get(), AppConfig.geminiApiKey) }
     */
    single<AiProvider> { ClaudeProvider(get(), AppConfig.claudeApiKey) }

    single { AiRepository(get()) }

    viewModel { HomeViewModel() }
    viewModel { AiViewModel(get()) }
}

val appModule = listOf(
    appModule(),
    platformModule()
)
