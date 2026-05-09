package io.jadu.catylst.di

import android.content.Context.MODE_PRIVATE
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import io.jadu.catylst.notifications.NotificationScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<ObservableSettings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("catylst_prefs", MODE_PRIVATE)
        )
    }

    single { NotificationScheduler(androidContext()) }
}
