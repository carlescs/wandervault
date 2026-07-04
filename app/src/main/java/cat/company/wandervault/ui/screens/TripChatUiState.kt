package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.model.TripChatSession

/**
 * UI state for the trip chat tab.
 */
sealed class TripChatUiState {
    /** Trip is being loaded. */
    data object Loading : TripChatUiState()

    /** The requested trip does not exist. */
    data object NotFound : TripChatUiState()

    /**
     * Trip loaded and chat is active.
     *
     * @param trip The trip being discussed.
     * @param messages Chat history.
     * @param chatSessions Saved chat sessions for the trip.
     * @param selectedChatSessionId The currently selected chat session ID.
     * @param isThinking `true` while the AI is processing the latest question.
     * @param downloadingBytes Non-null while the Gemini Nano model is being downloaded.
     * @param isAiAvailable Whether trip chat can currently use on-device AI.
     */
    data class Success(
        val trip: Trip,
        val messages: List<ChatMessage> = emptyList(),
        val chatSessions: List<TripChatSession> = emptyList(),
        val selectedChatSessionId: Int? = null,
        val isThinking: Boolean = false,
        val downloadingBytes: Long? = null,
        val isAiAvailable: Boolean = true,
    ) : TripChatUiState()
}
