package cat.company.wandervault.ui.screens

/**
 * Represents a single message in an AI chat conversation.
 */
sealed class ChatMessage {
    /** A question typed by the user. */
    data class UserMessage(val text: String) : ChatMessage()

    /** An answer returned by the AI. */
    data class AiMessage(val text: String) : ChatMessage()

    /** An error that occurred while processing the preceding user question. */
    data class ErrorMessage(val message: String?) : ChatMessage()
}
