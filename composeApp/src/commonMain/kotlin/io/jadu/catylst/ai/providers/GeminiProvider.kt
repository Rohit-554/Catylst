package io.jadu.catylst.ai.providers

import io.jadu.catylst.ai.AiProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * AI provider backed by Google's Gemini API (generateContent endpoint).
 *
 * Endpoint: `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
 * Docs: https://ai.google.dev/api/generate-content
 *
 * Get your API key at: https://aistudio.google.com/app/apikey
 *
 * Authentication: API key is passed as the `key` query parameter (Google's convention),
 * not in an Authorization header.
 *
 * @param client Shared [HttpClient] from Koin.
 * @param apiKey Your Google AI Studio API key — supply via `AppConfig.geminiApiKey`.
 * @param model  Gemini model ID. Defaults to `gemini-2.0-flash` (fast and free-tier friendly).
 *               Use `gemini-1.5-pro` for higher quality.
 */
class GeminiProvider(
    private val client: HttpClient,
    private val apiKey: String,
    private val model: String = "gemini-2.0-flash",
) : AiProvider {

    override suspend fun chat(prompt: String): Result<String> = runCatching {
        val response: GeminiResponse = client.post("$BASE_URL$model:generateContent") {
            contentType(ContentType.Application.Json)
            parameter("key", apiKey)
            setBody(
                GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    )
                )
            )
        }.body()
        response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: error("Gemini returned an empty response")
    }

    @Serializable
    private data class GeminiRequest(val contents: List<GeminiContent>)

    @Serializable
    private data class GeminiContent(
        val parts: List<GeminiPart>,
        val role: String = "user",
    )

    @Serializable
    private data class GeminiPart(val text: String)

    @Serializable
    private data class GeminiResponse(val candidates: List<GeminiCandidate>)

    @Serializable
    private data class GeminiCandidate(val content: GeminiContent)

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }
}
