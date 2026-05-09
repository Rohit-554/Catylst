package io.jadu.catylst.ai.providers

import io.jadu.catylst.ai.AiProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * AI provider backed by Groq's OpenAI-compatible API.
 *
 * Endpoint: `POST https://api.groq.com/openai/v1/chat/completions`
 * Docs: https://console.groq.com/docs/openai
 *
 * Get your API key at: https://console.groq.com/keys
 *
 * Groq runs open-source models (Llama, Gemma, Mixtral) at very high speed,
 * making it ideal for latency-sensitive use cases.
 *
 * @param client Shared [HttpClient] from Koin.
 * @param apiKey Your Groq API key — supply via `AppConfig.groqApiKey`.
 * @param model  Groq model ID. Defaults to `llama-3.3-70b-versatile`.
 *               Other options: `gemma2-9b-it`, `mixtral-8x7b-32768`.
 */
class GroqProvider(
    private val client: HttpClient,
    private val apiKey: String,
    private val model: String = "llama-3.3-70b-versatile",
) : AiProvider {

    override suspend fun chat(prompt: String): Result<String> = runCatching {
        val response: GroqResponse = client.post(BASE_URL) {
            contentType(ContentType.Application.Json)
            bearerAuth(apiKey)
            setBody(
                GroqRequest(
                    model = model,
                    messages = listOf(GroqMessage(role = "user", content = prompt)),
                )
            )
        }.body()
        response.choices.firstOrNull()?.message?.content
            ?: error("Groq returned an empty response")
    }

    @Serializable
    private data class GroqRequest(
        val model: String,
        val messages: List<GroqMessage>,
    )

    @Serializable
    private data class GroqMessage(val role: String, val content: String)

    @Serializable
    private data class GroqResponse(val choices: List<GroqChoice>)

    @Serializable
    private data class GroqChoice(val message: GroqMessage)

    private companion object {
        const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
    }
}
