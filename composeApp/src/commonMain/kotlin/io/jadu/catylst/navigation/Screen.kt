package io.jadu.catylst.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Home : Screen

    @Serializable
    data class Detail(val id: Long, val title: String) : Screen

    @Serializable
    data object Permissions : Screen

    @Serializable
    data object Notifications : Screen

    @Serializable
    data object Preferences : Screen

    @Serializable
    data object AiDemo : Screen
}
