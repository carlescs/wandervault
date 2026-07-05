package cat.company.wandervault.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.TripChatMessage as DomainTripChatMessage
import cat.company.wandervault.domain.model.TripChatMessageKind
import cat.company.wandervault.domain.usecase.AskTripQuestionUseCase
import cat.company.wandervault.domain.usecase.CreateTripChatSessionUseCase
import cat.company.wandervault.domain.usecase.DeleteTripChatSessionUseCase
import cat.company.wandervault.domain.usecase.GetActivitiesForTripUseCase
import cat.company.wandervault.domain.usecase.GetAllDocumentsForTripUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetHotelsForDestinationsUseCase
import cat.company.wandervault.domain.usecase.GetTripChatMessagesUseCase
import cat.company.wandervault.domain.usecase.GetTripChatSessionsUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.SaveTripChatMessageUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the trip chat tab.
 *
 * Builds trip-aware AI answers using the stored itinerary, activities, hotels, documents,
 * and document summaries.
 */
class TripChatViewModel(
    private val tripId: Int,
    private val getTripUseCase: GetTripUseCase,
    private val getDestinationsForTripUseCase: GetDestinationsForTripUseCase,
    private val getActivitiesForTripUseCase: GetActivitiesForTripUseCase,
    private val getHotelsForDestinationsUseCase: GetHotelsForDestinationsUseCase,
    private val getAllDocumentsForTripUseCase: GetAllDocumentsForTripUseCase,
    private val askTripQuestionUseCase: AskTripQuestionUseCase,
    private val getTripChatSessionsUseCase: GetTripChatSessionsUseCase,
    private val getTripChatMessagesUseCase: GetTripChatMessagesUseCase,
    private val createTripChatSessionUseCase: CreateTripChatSessionUseCase,
    private val saveTripChatMessageUseCase: SaveTripChatMessageUseCase,
    private val deleteTripChatSessionUseCase: DeleteTripChatSessionUseCase,
) : ViewModel() {

    private val _userSelectedSessionId = MutableStateFlow<Int?>(null)
    private val _isThinking = MutableStateFlow(false)
    private val _downloadingBytes = MutableStateFlow<Long?>(null)
    private val _isAiAvailable = MutableStateFlow(true)

    private var askJob: Job? = null

    private val sessionsFlow = getTripChatSessionsUseCase(tripId)

    // Derives the effective selected session ID reactively to avoid a standalone collector
    private val effectiveSelectedIdFlow = combine(
        sessionsFlow,
        _userSelectedSessionId,
    ) { sessions, userSelected ->
        when {
            sessions.isEmpty() -> null
            userSelected != null && sessions.any { it.id == userSelected } -> userSelected
            else -> sessions.first().id
        }
    }

    private val messagesFlow = effectiveSelectedIdFlow.flatMapLatest { sessionId ->
        if (sessionId == null) {
            flowOf(emptyList())
        } else {
            getTripChatMessagesUseCase(sessionId).map { messages -> messages.map { it.toUiMessage() } }
        }
    }

    val uiState: StateFlow<TripChatUiState> = combine(
        getTripUseCase(tripId),
        sessionsFlow,
        effectiveSelectedIdFlow,
        messagesFlow,
        _isThinking,
        _downloadingBytes,
        _isAiAvailable,
    ) { trip, sessions, selectedSessionId, messages, isThinking, downloadingBytes, isAiAvailable ->
        if (trip == null) {
            TripChatUiState.NotFound
        } else {
            TripChatUiState.Success(
                trip = trip,
                messages = messages,
                chatSessions = sessions,
                selectedChatSessionId = selectedSessionId,
                isThinking = isThinking,
                downloadingBytes = downloadingBytes,
                isAiAvailable = isAiAvailable,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TripChatUiState.Loading,
    )

    init {
        viewModelScope.launch {
            _isAiAvailable.value = try {
                askTripQuestionUseCase.isAvailable()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "AI availability check failed; assuming unavailable", e)
                false
            }
        }
    }

    fun selectChatSession(sessionId: Int) {
        val state = uiState.value as? TripChatUiState.Success ?: return
        if (state.chatSessions.any { it.id == sessionId }) {
            _userSelectedSessionId.value = sessionId
        }
    }

    fun createNewChat() {
        if (askJob?.isActive == true) return
        viewModelScope.launch {
            _userSelectedSessionId.value = createTripChatSessionUseCase(tripId)
        }
    }

    fun deleteChatSession(sessionId: Int) {
        if (askJob?.isActive == true) return
        viewModelScope.launch {
            deleteTripChatSessionUseCase(sessionId)
        }
    }

    /**
     * Sends [question] to Gemini Nano and appends the answer to the in-memory conversation.
     */
    fun sendMessage(question: String) {
        val state = uiState.value as? TripChatUiState.Success ?: return
        if (!_isAiAvailable.value || askJob?.isActive == true) return

        askJob = viewModelScope.launch {
            val sessionId = state.selectedChatSessionId ?: createTripChatSessionUseCase(tripId).also {
                _userSelectedSessionId.value = it
            }
            saveTripChatMessageUseCase(
                sessionId = sessionId,
                kind = TripChatMessageKind.USER,
                text = question,
            )
            _isThinking.value = true
            _downloadingBytes.value = null
            try {
                val destinations = getDestinationsForTripUseCase(tripId).first()
                val activities = getActivitiesForTripUseCase(tripId).first()
                val hotelsByDestination = getHotelsForDestinationsUseCase(destinations.map { it.id })
                val documents = getAllDocumentsForTripUseCase(tripId).first()
                val answer = askTripQuestionUseCase(
                    trip = state.trip,
                    destinations = destinations,
                    activities = activities,
                    hotelsByDestination = hotelsByDestination,
                    documents = documents,
                    question = question,
                ) { bytesDownloaded ->
                    _downloadingBytes.value = bytesDownloaded
                }
                if (answer == null) {
                    Log.w(TAG, "askTripQuestion returned null for trip $tripId; AI unavailable")
                    _isAiAvailable.value = false
                    saveTripChatMessageUseCase(
                        sessionId = sessionId,
                        kind = TripChatMessageKind.ERROR,
                        text = null,
                    )
                } else {
                    saveTripChatMessageUseCase(
                        sessionId = sessionId,
                        kind = TripChatMessageKind.AI,
                        text = answer,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Trip chat question failed for trip $tripId", e)
                saveTripChatMessageUseCase(
                    sessionId = sessionId,
                    kind = TripChatMessageKind.ERROR,
                    text = e.message ?: e.toString(),
                )
            } finally {
                _isThinking.value = false
                _downloadingBytes.value = null
            }
        }
    }

    companion object {
        private const val TAG = "TripChatViewModel"
    }
}

private fun DomainTripChatMessage.toUiMessage(): ChatMessage =
    when (kind) {
        TripChatMessageKind.USER -> ChatMessage.UserMessage(text.orEmpty())
        TripChatMessageKind.AI -> ChatMessage.AiMessage(text.orEmpty())
        TripChatMessageKind.ERROR -> ChatMessage.ErrorMessage(text)
    }
