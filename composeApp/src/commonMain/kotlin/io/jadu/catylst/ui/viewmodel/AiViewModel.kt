package io.jadu.catylst.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.jadu.catylst.ai.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the AI demo screen.
 *
 * Exposes [state] as a [StateFlow] of [AiUiState]. Call [send] from the UI
 * when the user submits a prompt.
 *
 * The active AI provider is determined by the Koin binding in `AppModule.kt` —
 * swap `single<AiProvider> { ... }` there to change providers without touching this class.
 *
 * @param repository The [AiRepository] injected by Koin.
 */
class AiViewModel(private val repository: AiRepository) : ViewModel() {

    private val _state = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    /**
     * Sends [prompt] to the active AI provider.
     *
     * State transitions: `Idle | Success | Error` → `Loading` → `Success | Error`
     * Blank prompts are ignored.
     */
    fun send(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _state.value = AiUiState.Loading
            _state.value = repository.chat(prompt).fold(
                onSuccess = { AiUiState.Success(prompt = prompt, reply = it) },
                onFailure = { AiUiState.Error(message = it.message ?: "Unknown error") },
            )
        }
    }
}

/** Complete UI state of the AI demo screen. */
sealed interface AiUiState {
    /** Initial state — no request has been made yet. */
    data object Idle : AiUiState

    /** A request is in-flight. The send button should be disabled. */
    data object Loading : AiUiState

    /**
     * A reply was received successfully.
     * @property prompt The original user prompt (shown for context in the UI).
     * @property reply  The text returned by the AI provider.
     */
    data class Success(val prompt: String, val reply: String) : AiUiState

    /**
     * The request failed (network error, bad API key, quota exceeded, etc.)
     * @property message A user-displayable description of the failure.
     */
    data class Error(val message: String) : AiUiState
}
