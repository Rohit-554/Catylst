package com.catylst.cli.template

import java.io.File

object HomeViewModelEditor {

    fun regenerate(projectDir: File, packageName: String, hasRoom: Boolean, hasKtor: Boolean) {
        val vmFile = findFile(projectDir, "HomeViewModel.kt") ?: return

        val dbImport = if (hasRoom) "import ${packageName}.data.local.AppDatabase\nimport ${packageName}.data.local.SampleEntity" else ""
        val dbParam = if (hasRoom) "private val database: AppDatabase? = null" else ""
        val dbParamComma = if (hasRoom && hasKtor) "," else ""
        val apiParam = if (hasKtor) "private val apiService: ApiService? = null" else ""
        val apiImport = if (hasKtor) "import ${packageName}.network.ApiService" else ""

        val constructorParams = when {
            hasKtor && hasRoom -> "(\n    $apiParam$dbParamComma\n    $dbParam\n)"
            hasKtor -> "(\n    $apiParam\n)"
            hasRoom -> "(\n    $dbParam\n)"
            else -> "()"
        }

        val addSampleFunction = if (hasRoom) {
            """
    fun addSampleEntity(title: String, description: String) {
        viewModelScope.launch {
            database?.sampleEntityDao()?.insert(
                SampleEntity(
                    title = title,
                    description = description,
                    createdAt = 0
                )
            )
        }
    }"""
        } else ""

        val content = """package ${packageName}.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
$apiImport
$dbImport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel$constructorParams : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = HomeUiState.Success(
                message = "Welcome to ${packageName.substringAfterLast(".")}!",
                items = listOf(
                    "KMP Starter Template",
                    "AGP 9 + Kotlin 2.3",
                    "Compose Multiplatform",
                    "Room 3.0",
                    "Koin DI",
                    "Ktor Networking",
                    "Navigation3"
                )
            )
        }
    }
$addSampleFunction
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val message: String,
        val items: List<String>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
""".trimIndent()

        vmFile.writeText(content)
    }

    private fun findFile(projectDir: File, name: String): File? {
        return projectDir.walkTopDown()
            .filter { it.isFile && it.name == name }
            .firstOrNull()
    }
}
