package io.jadu.catylst.ai

import kotlinx.serialization.Serializable

/**
 * A single turn in an AI conversation.
 *
 * Used as the shared conceptual model across the app. Provider-specific wire
 * formats (request/response JSON) are kept private inside each provider file.
 *
 * @property role    Either `"user"` or `"assistant"`.
 * @property content The plain-text content of the message.
 */
@Serializable
data class AiMessage(
    val role: String,
    val content: String,
)
