package io.jadu.catylst.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import io.jadu.catylst.notifications.NotificationScheduler
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences

actual fun platformModule(): Module = module {
    single<ObservableSettings> { PreferencesSettings(Preferences.userRoot().node("catylst_prefs")) }

    single { NotificationScheduler() }
}
