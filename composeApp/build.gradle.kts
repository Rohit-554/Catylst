import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.workmanager)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodel)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // Room 3.0
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)

            // Navigation3 (navigation3-runtime is bundled in navigation3-ui)
            implementation(libs.navigation3.ui)

            // Kotlin Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coil 3
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // kotlinx-datetime
            implementation(libs.kotlinx.datetime)

            // multiplatform-settings
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.multiplatform.settings.no.arg)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.kotest.assertions)
            implementation(libs.mockative)
        }
    }

    android {
        namespace = "io.jadu.catylst"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources {
            enable = true
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)

    // Room KSP processors
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)

    // Mockative KSP processor
    add("kspCommonMainMetadata", libs.mockative.processor)
    add("kspAndroid", libs.mockative.processor)
    add("kspIosArm64", libs.mockative.processor)
    add("kspIosSimulatorArm64", libs.mockative.processor)
}

// Room schema directory configured via KSP arguments
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
