package io.jadu.catylst.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import io.jadu.catylst.notifications.NotificationScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<ObservableSettings> { NSUserDefaultsSettings.Factory().create("catylst_prefs") }

    single { NotificationScheduler() }
}
