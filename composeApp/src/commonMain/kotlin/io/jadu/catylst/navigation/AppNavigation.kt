package io.jadu.catylst.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import io.jadu.catylst.ui.screens.AiDemoScreen
import io.jadu.catylst.ui.screens.DetailScreen
import io.jadu.catylst.ui.screens.HomeScreen
import io.jadu.catylst.ui.screens.NotificationDemoScreen
import io.jadu.catylst.ui.screens.PermissionDemoScreen
import io.jadu.catylst.ui.screens.PreferencesDemoScreen
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Screen.Home::class)
                    subclass(Screen.Detail::class)
                    subclass(Screen.Permissions::class)
                    subclass(Screen.Notifications::class)
                    subclass(Screen.Preferences::class)
                    subclass(Screen.AiDemo::class)
                }
            }
        },
        Screen.Home
    )

    Crossfade(targetState = backStack.lastOrNull() ?: Screen.Home) { screen ->
        when (screen) {
            is Screen.Home -> HomeScreen(
                onNavigateToDetail = { id, title -> backStack.add(Screen.Detail(id, title)) },
                onNavigateToPermissions = { backStack.add(Screen.Permissions) },
                onNavigateToNotifications = { backStack.add(Screen.Notifications) },
                onNavigateToPreferences = { backStack.add(Screen.Preferences) },
                onNavigateToAiDemo = { backStack.add(Screen.AiDemo) },
            )
            is Screen.Detail -> DetailScreen(
                id = screen.id,
                title = screen.title,
                onBack = { backStack.removeLastOrNull() },
            )
            is Screen.Permissions -> PermissionDemoScreen(
                onBack = { backStack.removeLastOrNull() },
            )
            is Screen.Notifications -> NotificationDemoScreen(
                onBack = { backStack.removeLastOrNull() },
            )
            is Screen.Preferences -> PreferencesDemoScreen(
                onBack = { backStack.removeLastOrNull() },
            )
            is Screen.AiDemo -> AiDemoScreen(
                onBack = { backStack.removeLastOrNull() },
            )
        }
    }
}
