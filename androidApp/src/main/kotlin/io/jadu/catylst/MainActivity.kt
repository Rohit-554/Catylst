package io.jadu.catylst

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.jadu.catylst.android.BuildConfig
import io.jadu.catylst.config.AppConfig
import io.jadu.catylst.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Inject AI provider keys from BuildConfig (sourced from local.properties).
        // See local.properties.example for setup instructions.
        AppConfig.claudeApiKey = BuildConfig.CLAUDE_API_KEY
        AppConfig.groqApiKey   = BuildConfig.GROQ_API_KEY
        AppConfig.geminiApiKey = BuildConfig.GEMINI_API_KEY

        startKoin {
            androidLogger()
            androidContext(applicationContext)
            modules(appModule)
        }

        setContent {
            App()
        }
    }
}
