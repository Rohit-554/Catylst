package io.jadu.catylst.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import io.jadu.catylst.ui.screens.DetailScreen
import io.jadu.catylst.ui.screens.HomeScreen
import io.jadu.catylst.ui.screens.PermissionDemoScreen
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
                }
            }
        },
        Screen.Home
    )

    Crossfade(
        targetState = backStack.lastOrNull() ?: Screen.Home
    ) { screen ->
        when (screen) {
            is Screen.Home -> HomeScreen(
                onNavigateToDetail = { id, title -> backStack.add(Screen.Detail(id, title)) },
                onNavigateToPermissions = { backStack.add(Screen.Permissions) },
            )
            is Screen.Detail -> DetailScreen(
                id = screen.id,
                title = screen.title,
                onBack = { backStack.removeLastOrNull() },
            )
            is Screen.Permissions -> PermissionDemoScreen(
                onBack = { backStack.removeLastOrNull() },
            )
        }
    }
}
