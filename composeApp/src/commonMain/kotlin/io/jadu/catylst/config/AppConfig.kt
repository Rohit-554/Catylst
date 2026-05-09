package io.jadu.catylst.config

/**
 * Provides API keys for all AI providers.
 *
 * Keys are injected at app startup from each platform's entry point before
 * Koin is initialized — so no expect/actual or cross-module BuildConfig is needed.
 *
 * ## How to configure
 * - **Android**: add keys to `local.properties` (see `local.properties.example`).
 *   They are read in `androidApp/build.gradle.kts` and injected in `MainActivity.onCreate`.
 * - **iOS**: set the properties in `MainViewController.kt` (iosMain) before calling `initKoin()`.
 * - **Desktop**: set the properties in `main.kt` (desktopMain) or via environment variables.
 *
 * ## Security
 * Never commit real keys. `local.properties` is already in `.gitignore`.
 */
object AppConfig {
    var claudeApiKey: String = ""
    var groqApiKey: String = ""
    var geminiApiKey: String = ""
}
