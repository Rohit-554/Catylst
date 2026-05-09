package io.jadu.catylst.ai

/**
 * Single source of truth for AI interactions.
 *
 * Delegates to whichever [AiProvider] is currently bound in Koin.
 * Inject this class into ViewModels — do not inject [AiProvider] directly,
 * as the repository is the correct abstraction boundary for testing and future
 * enhancements (caching, rate-limiting, conversation history, etc.).
 *
 * @param provider The active AI backend, resolved by Koin.
 */
class AiRepository(private val provider: AiProvider) {

    /**
     * Sends [prompt] to the active provider and returns the reply text.
     * Propagates [Result.failure] from the provider without rethrowing.
     */
    suspend fun chat(prompt: String): Result<String> = provider.chat(prompt)
}
