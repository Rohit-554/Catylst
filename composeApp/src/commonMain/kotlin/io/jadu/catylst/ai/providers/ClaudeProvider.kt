package io.jadu.catylst.ai.providers

import io.jadu.catylst.ai.AiProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AI provider backed by Anthropic's Claude API (Messages endpoint).
 *
 * Endpoint: `POST https://api.anthropic.com/v1/messages`
 * Docs: https://docs.anthropic.com/en/api/messages
 *
 * Get your API key at: https://console.anthropic.com
 *
 * @param client Shared [HttpClient] from Koin.
 * @param apiKey Your Anthropic API key — supply via `AppConfig.claudeApiKey`.
 * @param model  Claude model ID. Defaults to `claude-3-5-haiku-20241022`
 *               (fast and cost-efficient). Use `claude-sonnet-4-6` for higher quality.
 */
class ClaudeProvider(
    private val client: HttpClient,
    private val apiKey: String,
    private val model: String = "claude-3-5-haiku-20241022",
) : AiProvider {

    override suspend fun chat(prompt: String): Result<String> = runCatching {
        val response: ClaudeResponse = client.post(BASE_URL) {
            contentType(ContentType.Application.Json)
            headers {
                append("x-api-key", apiKey)
                append("anthropic-version", "2023-06-01")
            }
            setBody(
                ClaudeRequest(
                    model = model,
                    maxTokens = 1024,
                    messages = listOf(ClaudeMessage(role = "user", content = prompt)),
                )
            )
        }.body()
        response.content.firstOrNull()?.text
            ?: error("Claude returned an empty response")
    }

    @Serializable
    private data class ClaudeRequest(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val messages: List<ClaudeMessage>,
    )

    @Serializable
    private data class ClaudeMessage(val role: String, val content: String)

    @Serializable
    private data class ClaudeResponse(val content: List<ClaudeContent>)

    @Serializable
    private data class ClaudeContent(val type: String, val text: String)

    private companion object {
        const val BASE_URL = "https://api.anthropic.com/v1/messages"
    }
}
