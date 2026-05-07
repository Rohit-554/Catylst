package io.jadu.catylst.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.jadu.catylst.data.local.AppDatabase
import io.jadu.catylst.data.local.SampleEntity
import io.jadu.catylst.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val apiService: ApiService? = null,
    private val database: AppDatabase? = null
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = HomeUiState.Success(
                message = "Welcome to Catylst!",
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
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val message: String,
        val items: List<String>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
