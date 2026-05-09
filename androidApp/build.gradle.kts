import java.util.Properties

// AI provider keys — read from local.properties at build time.
// See local.properties.example for the expected key names.
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) localPropsFile.inputStream().use { localProps.load(it) }

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "io.jadu.catylst.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.jadu.catylst"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "CLAUDE_API_KEY", "\"${localProps.getProperty("claude.api.key", "demo-claude-key")}\"")
        buildConfigField("String", "GROQ_API_KEY",   "\"${localProps.getProperty("groq.api.key",   "demo-groq-key")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps.getProperty("gemini.api.key", "demo-gemini-key")}\"")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    debugImplementation(libs.compose.uiTooling)
}
