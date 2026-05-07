package io.jadu.catylst

import androidx.compose.ui.window.ComposeUIViewController
import io.jadu.catylst.di.appModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(appModule)
    }
}

fun MainViewController() = ComposeUIViewController(
    configure = { initKoin() }
) { App() }
