package io.jadu.catylst

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.jadu.catylst.di.appModule
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Catylst",
        ) {
            App()
        }
    }
}
