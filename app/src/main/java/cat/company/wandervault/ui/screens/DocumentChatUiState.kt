package cat.company.wandervault.ui.screens

/**
 * Represents a single message in the document chat.
 */
sealed class ChatMessage {
    /** A question typed by the user. */
    data class UserMessage(val text: String) : ChatMessage()

    /** An answer returned by the AI. */
    data class AiMessage(val text: String) : ChatMessage()

    /** An error that occurred while processing the preceding user question. */
    data class ErrorMessage(val message: String?) : ChatMessage()
}

/**
 * UI state for the Document Chat screen.
 */
sealed class DocumentChatUiState {
    /** Document is being loaded. */
    data object Loading : DocumentChatUiState()

    /** The requested document does not exist. */
    data object NotFound : DocumentChatUiState()

    /**
     * Document loaded and chat is active.
     *
     * @param documentName Display name of the document being discussed.
     * @param documentUri Content URI of the document file.
     * @param documentMimeType MIME type of the document file.
     * @param messages Chat history (user questions and AI answers).
     * @param isThinking `true` while the AI is processing the latest question.
     * @param downloadingBytes Non-null while the Gemini Nano model is being downloaded;
     *   the value is the total bytes downloaded so far.
     */
    data class Success(
        val documentName: String,
        val documentUri: String,
        val documentMimeType: String,
        val messages: List<ChatMessage> = emptyList(),
        val isThinking: Boolean = false,
        val downloadingBytes: Long? = null,
    ) : DocumentChatUiState()
}
