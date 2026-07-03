package cat.company.wandervault.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.usecase.AskTripQuestionUseCase
import cat.company.wandervault.domain.usecase.GetActivitiesForTripUseCase
import cat.company.wandervault.domain.usecase.GetAllDocumentsForTripUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetHotelsForDestinationsUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _isThinking = MutableStateFlow(false)
    private val _downloadingBytes = MutableStateFlow<Long?>(null)
    private val _isAiAvailable = MutableStateFlow(true)

    private var askJob: Job? = null

    val uiState: StateFlow<TripChatUiState> = combine(
        getTripUseCase(tripId),
        _messages,
        _isThinking,
        _downloadingBytes,
        _isAiAvailable,
    ) { trip, messages, isThinking, downloadingBytes, isAiAvailable ->
        if (trip == null) {
            TripChatUiState.NotFound
        } else {
            TripChatUiState.Success(
                trip = trip,
                messages = messages,
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

    /**
     * Sends [question] to Gemini Nano and appends the answer to the in-memory conversation.
     */
    fun sendMessage(question: String) {
        val state = uiState.value as? TripChatUiState.Success ?: return
        if (!_isAiAvailable.value || askJob?.isActive == true) return

        askJob = viewModelScope.launch {
            _messages.value = _messages.value + ChatMessage.UserMessage(question)
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
                    _messages.value = _messages.value + ChatMessage.ErrorMessage(null)
                } else {
                    _messages.value = _messages.value + ChatMessage.AiMessage(answer)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Trip chat question failed for trip $tripId", e)
                _messages.value = _messages.value + ChatMessage.ErrorMessage(e.message ?: e.toString())
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
