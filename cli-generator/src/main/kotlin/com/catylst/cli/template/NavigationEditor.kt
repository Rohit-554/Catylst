package com.catylst.cli.template

import java.io.File

object NavigationEditor {

    /**
     * Regenerates Screen.kt, AppNavigation.kt, and HomeScreen.kt based on remaining screens.
     * screensToRemove: screens that should NOT appear in the generated files.
     */
    fun removeScreens(projectDir: File, screensToRemove: Set<String>) {
        if (screensToRemove.isEmpty()) return

        val screenFile = findFile(projectDir, "Screen.kt") ?: return
        val navFile = findFile(projectDir, "AppNavigation.kt") ?: return
        val homeScreenFile = findFile(projectDir, "HomeScreen.kt") ?: return

        // Detect current package from AppNavigation.kt (not yet modified)
        val navPackage = detectPackage(navFile)
        val packageName = navPackage

        // Determine remaining screens by reading current Screen.kt
        val currentScreens = parseCurrentScreens(screenFile)
        val remainingScreens = currentScreens.filter { it !in screensToRemove }.toSet()

        // Regenerate all three files
        regenerateScreenKt(screenFile, packageName, remainingScreens)
        regenerateAppNavigation(navFile, packageName, remainingScreens)
        regenerateHomeScreen(homeScreenFile, packageName, remainingScreens)
    }

    private fun detectPackage(file: File): String {
        return file.readText().lines().firstOrNull { it.startsWith("package ") }
            ?.removePrefix("package ")?.trim() ?: "com.example.app"
    }

    private fun parseCurrentScreens(screenFile: File): Set<String> {
        val content = screenFile.readText()
        val screens = mutableSetOf<String>()
        // Match: data object Xxx : Screen or data class Xxx(...) : Screen
        val regex = """data\s+(object|class)\s+(\w+)""".toRegex()
        regex.findAll(content).forEach { match ->
            screens.add(match.groupValues[2])
        }
        return screens
    }

    private fun regenerateScreenKt(screenFile: File, packageName: String, screens: Set<String>) {
        val screenDefs = screens.joinToString("\n\n") { screen ->
            when (screen) {
                "Home" -> "    @Serializable\n    data object Home : Screen"
                "Detail" -> "    @Serializable\n    data class Detail(val id: Long, val title: String) : Screen"
                "Permissions" -> "    @Serializable\n    data object Permissions : Screen"
                "Notifications" -> "    @Serializable\n    data object Notifications : Screen"
                "Preferences" -> "    @Serializable\n    data object Preferences : Screen"
                "AiDemo" -> "    @Serializable\n    data object AiDemo : Screen"
                else -> "    @Serializable\n    data object $screen : Screen"
            }
        }

        val content = """package $packageName

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
$screenDefs
}
""".trimIndent()

        screenFile.writeText(content)
    }

    private fun regenerateAppNavigation(navFile: File, packageName: String, screens: Set<String>) {
        // Base package for ui.screens imports (strip .navigation suffix)
        val basePackage = packageName.substringBeforeLast(".navigation")
        val imports = mutableListOf(
            "import androidx.compose.animation.Crossfade",
            "import androidx.compose.runtime.Composable",
            "import androidx.navigation3.runtime.NavKey",
            "import androidx.navigation3.runtime.rememberNavBackStack",
            "import androidx.savedstate.serialization.SavedStateConfiguration",
            "import ${basePackage}.ui.screens.HomeScreen",
            "import kotlinx.serialization.modules.SerializersModule",
            "import kotlinx.serialization.modules.polymorphic",
            "import kotlinx.serialization.modules.subclass"
        )

        for (screen in screens) {
            when (screen) {
                "Detail" -> imports.add("import ${basePackage}.ui.screens.DetailScreen")
                "Permissions" -> imports.add("import ${basePackage}.ui.screens.PermissionDemoScreen")
                "Notifications" -> imports.add("import ${basePackage}.ui.screens.NotificationDemoScreen")
                "Preferences" -> imports.add("import ${basePackage}.ui.screens.PreferencesDemoScreen")
                "AiDemo" -> imports.add("import ${basePackage}.ui.screens.AiDemoScreen")
            }
        }

        val subclassRegs = screens.joinToString("\n                    ") {
            "subclass(Screen.$it::class)"
        }

        val homeParams = screens.filter { it != "Home" }.joinToString(",\n                ") { screen ->
            when (screen) {
                "Detail" -> "onNavigateToDetail = { id, title -> backStack.add(Screen.Detail(id, title)) }"
                "Permissions" -> "onNavigateToPermissions = { backStack.add(Screen.Permissions) }"
                "Notifications" -> "onNavigateToNotifications = { backStack.add(Screen.Notifications) }"
                "Preferences" -> "onNavigateToPreferences = { backStack.add(Screen.Preferences) }"
                "AiDemo" -> "onNavigateToAiDemo = { backStack.add(Screen.AiDemo) }"
                else -> ""
            }
        }

        val whenBranches = screens.filter { it != "Home" }.joinToString("\n            ") { screen ->
            when (screen) {
                "Detail" -> """is Screen.Detail -> DetailScreen(
                id = screen.id,
                title = screen.title,
                onBack = { backStack.removeLastOrNull() },
            )"""
                "Permissions" -> """is Screen.Permissions -> PermissionDemoScreen(
                onBack = { backStack.removeLastOrNull() },
            )"""
                "Notifications" -> """is Screen.Notifications -> NotificationDemoScreen(
                onBack = { backStack.removeLastOrNull() },
            )"""
                "Preferences" -> """is Screen.Preferences -> PreferencesDemoScreen(
                onBack = { backStack.removeLastOrNull() },
            )"""
                "AiDemo" -> """is Screen.AiDemo -> AiDemoScreen(
                onBack = { backStack.removeLastOrNull() },
            )"""
                else -> ""
            }
        }

        val homeScreenCall = if (homeParams.isNotEmpty()) {
            """is Screen.Home -> HomeScreen(
                $homeParams,
            )"""
        } else {
            "is Screen.Home -> HomeScreen()"
        }

        val content = """package $packageName

${imports.sorted().joinToString("\n")}

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    $subclassRegs
                }
            }
        },
        Screen.Home
    )

    Crossfade(targetState = backStack.lastOrNull() ?: Screen.Home) { screen ->
        when (screen) {
            $homeScreenCall
            $whenBranches
        }
    }
}
""".trimIndent()

        navFile.writeText(content)
    }

    private fun regenerateHomeScreen(homeScreenFile: File, navPackage: String, screens: Set<String>) {
        // HomeScreen is in {basePackage}.ui.screens
        val packageName = navPackage.substringBeforeLast(".navigation")
        val params = screens.filter { it != "Home" }.joinToString(",\n    ") { screen ->
            when (screen) {
                "Detail" -> "onNavigateToDetail: (Long, String) -> Unit"
                "Permissions" -> "onNavigateToPermissions: () -> Unit"
                "Notifications" -> "onNavigateToNotifications: () -> Unit"
                "Preferences" -> "onNavigateToPreferences: () -> Unit"
                "AiDemo" -> "onNavigateToAiDemo: () -> Unit"
                else -> ""
            }
        }

        val paramList = if (params.isNotEmpty()) {
            "$params,\n    viewModel: HomeViewModel = koinViewModel(),"
        } else {
            "viewModel: HomeViewModel = koinViewModel(),"
        }

        val buttons = screens.filter { it != "Home" }.joinToString("\n\n") { screen ->
            when (screen) {
                "Detail" -> """                    Button(
                        onClick = { onNavigateToDetail(1, "Sample Detail") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Go to Detail")
                    }"""
                "Permissions" -> """                    OutlinedButton(
                        onClick = onNavigateToPermissions,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Permissions Demo")
                    }"""
                "Notifications" -> """                    OutlinedButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Notifications Demo")
                    }"""
                "Preferences" -> """                    OutlinedButton(
                        onClick = onNavigateToPreferences,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Preferences Demo")
                    }"""
                "AiDemo" -> """                    OutlinedButton(
                        onClick = onNavigateToAiDemo,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("AI Demo")
                    }"""
                else -> ""
            }
        }

        val content = """package ${packageName}.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ${packageName}.ui.viewmodel.HomeUiState
import ${packageName}.ui.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    $paramList
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catylst") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val currentState = state) {
                is HomeUiState.Loading -> {
                    Text("Loading...", style = MaterialTheme.typography.bodyLarge)
                }
                is HomeUiState.Success -> {
                    Text(
                        text = currentState.message,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

$buttons

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(currentState.items) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ) {
                                Text(
                                    text = item,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
                is HomeUiState.Error -> {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
""".trimIndent()

        homeScreenFile.writeText(content)
    }

}
