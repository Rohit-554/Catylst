package io.jadu.catylst.di

import io.jadu.catylst.data.local.AppDatabase
import io.jadu.catylst.data.local.createAppDatabase
import io.jadu.catylst.network.ApiService
import io.jadu.catylst.network.createHttpClient
import io.jadu.catylst.ui.viewmodel.HomeViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(): Module = module {
    // Network
    single<HttpClient> { createHttpClient() }
    single { ApiService(get()) }

    // Database
    single<AppDatabase> { createAppDatabase() }

    // ViewModels
    viewModel { HomeViewModel() }
}

val appModule = listOf(
    appModule(),
    platformModule()
)
