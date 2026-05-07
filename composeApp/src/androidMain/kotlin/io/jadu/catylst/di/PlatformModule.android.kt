package io.jadu.catylst.di

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // Android-specific dependencies (e.g., Context) provided via startKoin { androidContext() }
}
