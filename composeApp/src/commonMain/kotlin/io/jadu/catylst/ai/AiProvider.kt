package io.jadu.catylst.ai

/**
 * Contract for any AI chat provider.
 *
 * ## Swapping providers
 * In `AppModule.kt`, change the single `AiProvider` binding:
 * ```kotlin
 * single<AiProvider> { ClaudeProvider(get(), AppConfig.claudeApiKey) }
 * // → single<AiProvider> { GroqProvider(get(), AppConfig.groqApiKey) }
 * // → single<AiProvider> { GeminiProvider(get(), AppConfig.geminiApiKey) }
 * ```
 * Everything above — `AiRepository`, `AiViewModel`, `AiDemoScreen` — stays unchanged.
 *
 * ## Adding a custom provider
 * 1. Implement this interface.
 * 2. Register it in `AppModule` as `single<AiProvider> { MyProvider(...) }`.
 * 3. Done — no other files need to change.
 *
 * @see io.jadu.catylst.ai.providers.ClaudeProvider
 * @see io.jadu.catylst.ai.providers.GroqProvider
 * @see io.jadu.catylst.ai.providers.GeminiProvider
 */
interface AiProvider {

    /**
     * Sends [prompt] to the AI backend and returns the reply text.
     *
     * Returns [Result.failure] on network errors or unexpected API responses
     * so callers never need to catch exceptions.
     */
    suspend fun chat(prompt: String): Result<String>
}
