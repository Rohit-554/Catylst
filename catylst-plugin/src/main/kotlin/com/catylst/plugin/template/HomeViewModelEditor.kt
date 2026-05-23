package com.catylst.plugin.template

import java.io.File

object HomeViewModelEditor {

    fun regenerate(
        projectDir: File,
        packageName: String,
        hasRoom: Boolean,
        hasKtor: Boolean,
        selectedFeatures: Set<String> = emptySet()
    ) {
        val vmFile = findFile(projectDir, "HomeViewModel.kt") ?: return

        val imports = buildList {
            if (hasKtor) add("import ${packageName}.network.ApiService")
            if (hasRoom) {
                add("import ${packageName}.data.local.AppDatabase")
                add("import ${packageName}.data.local.SampleEntity")
            }
        }.joinToString("\n")

        val params = buildList {
            if (hasKtor) add("    private val apiService: ApiService? = null")
            if (hasRoom) add("    private val database: AppDatabase? = null")
        }
        val constructorParams = when {
            params.isEmpty() -> "()"
            else -> "(\n${params.joinToString(",\n")}\n)"
        }

        val featureItems = buildList {
            add("\"Compose Multiplatform\"")
            add("\"Koin DI\"")
            add("\"Navigation3\"")
            if ("ktor" in selectedFeatures) add("\"Ktor Networking\"")
            if ("room" in selectedFeatures) add("\"Room 3.0\"")
            if ("preferences" in selectedFeatures) add("\"Multiplatform Preferences\"")
            if ("notifications" in selectedFeatures) add("\"Push Notifications\"")
            if ("permissions" in selectedFeatures) add("\"Runtime Permissions\"")
            if ("ai" in selectedFeatures) add("\"AI Integration\"")
            if ("server" in selectedFeatures) add("\"Ktor Server\"")
        }.joinToString(",\n                    ")

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
$imports
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
                    $featureItems
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
}
