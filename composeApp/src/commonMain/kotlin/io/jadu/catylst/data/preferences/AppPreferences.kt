package io.jadu.catylst.data.preferences

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalSettingsApi::class)
class AppPreferences(private val settings: ObservableSettings) {

    val authTokenFlow: Flow<String?> = settings.getStringOrNullFlow(KEY_AUTH_TOKEN)

    var authToken: String?
        get() = settings.getStringOrNull(KEY_AUTH_TOKEN)
        set(value) = if (value != null) settings.putString(KEY_AUTH_TOKEN, value)
                     else settings.remove(KEY_AUTH_TOKEN)

    var onboardingComplete: Boolean
        get() = settings.getBoolean(KEY_ONBOARDING, defaultValue = false)
        set(value) = settings.putBoolean(KEY_ONBOARDING, value)

    var username: String
        get() = settings.getString(KEY_USERNAME, defaultValue = "")
        set(value) = settings.putString(KEY_USERNAME, value)

    fun clearAll() = settings.clear()

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_ONBOARDING = "onboarding_complete"
        private const val KEY_USERNAME = "username"
    }
}
