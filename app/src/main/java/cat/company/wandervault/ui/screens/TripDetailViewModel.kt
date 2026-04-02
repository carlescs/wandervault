package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.usecase.GenerateTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.GenerateWhatsNextUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.SaveTripDescriptionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for the Trip Detail screen.
 *
 * @param tripId The ID of the trip to display.
 * @param getTripUseCase Use-case that fetches a single trip by ID.
 * @param getDestinationsForTripUseCase Use-case that returns the ordered destinations for the trip.
 * @param generateTripDescriptionUseCase Use-case that generates a short AI description of the trip.
 * @param saveTripDescriptionUseCase Use-case that persists the AI description (or clears it).
 * @param generateWhatsNextUseCase Use-case that generates the "what's next" notice for the trip.
 */
class TripDetailViewModel(
    private val tripId: Int,
    private val getTripUseCase: GetTripUseCase,
    private val getDestinationsForTripUseCase: GetDestinationsForTripUseCase,
    private val generateTripDescriptionUseCase: GenerateTripDescriptionUseCase,
    private val saveTripDescriptionUseCase: SaveTripDescriptionUseCase,
    private val generateWhatsNextUseCase: GenerateWhatsNextUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TripDetailUiState>(TripDetailUiState.Loading)
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    /** Cached trip used for on-demand generation/deletion triggered by the user. */
    private var lastTrip: Trip? = null

    /** Cached destinations used for on-demand re-generation triggered by the user. */
    private var lastDestinations: List<Destination> = emptyList()

    /**
     * Tracks on-device AI availability.
     * Initialised to `false` (fail-closed) and updated once in [init] after checking the model.
     * Drives whether the AI description section is shown when no description is stored.
     */
    private val _isAiAvailable = MutableStateFlow(false)

    /**
     * The trip and destinations for which the most recent "what's next" generation was started.
     * `null` until the first generation is triggered.
     *
     * Used to detect when the trip/destinations inputs have changed so that the notice can be
     * invalidated and re-generated automatically, avoiding stale results after itinerary edits.
     */
    private var whatsNextGeneratedForInputs: Pair<Trip, List<Destination>>? = null

    init {
        // Check AI availability upfront so the description section is hidden proactively
        // on devices that do not support Gemini Nano, without requiring a generate attempt.
        viewModelScope.launch {
            val available = try {
                generateTripDescriptionUseCase.isAvailable()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "AI availability check failed; assuming unavailable", e)
                false
            }
            _isAiAvailable.value = available
        }

        viewModelScope.launch {
            combine(
                getTripUseCase(tripId),
                getDestinationsForTripUseCase(tripId),
                _isAiAvailable,
            ) { trip, destinations, aiAvailable -> Triple(trip, destinations, aiAvailable) }
                .collect { (trip, destinations, aiAvailable) ->
                    if (trip == null) {
                        _uiState.value = TripDetailUiState.Error
                        return@collect
                    }
                    lastTrip = trip
                    lastDestinations = destinations
                    // Only preserve the in-memory state while generation is actively in progress,
                    // so DB remains the source of truth for all other states.
                    val currentDescription =
                        (_uiState.value as? TripDetailUiState.Success)?.descriptionState
                    val persistedDescription = when {
                        trip.aiDescription != null -> DescriptionState.Available(trip.aiDescription)
                        !aiAvailable -> DescriptionState.Unavailable
                        else -> DescriptionState.None
                    }
                    // Only preserve Loading to avoid a flicker while generation is in progress;
                    // for all other states the DB value is the source of truth.
                    val descriptionState = if (currentDescription is DescriptionState.Loading) {
                        currentDescription
                    } else {
                        persistedDescription
                    }

                    // Preserve the current what's next state only while generation is in progress,
                    // so it is reset (and re-generated) whenever the trip/destinations inputs change.
                    val currentWhatsNext =
                        (_uiState.value as? TripDetailUiState.Success)?.whatsNextState
                    val inputsMatchLastGenerated =
                        whatsNextGeneratedForInputs?.let { (t, d) ->
                            t == trip && d == destinations
                        } ?: false
                    val whatsNextState = when {
                        currentWhatsNext is WhatsNextState.Loading -> currentWhatsNext
                        !aiAvailable -> WhatsNextState.Unavailable
                        inputsMatchLastGenerated -> currentWhatsNext ?: WhatsNextState.None
                        else -> WhatsNextState.None
                    }

                    _uiState.value = TripDetailUiState.Success(
                        trip = trip,
                        descriptionState = descriptionState,
                        whatsNextState = whatsNextState,
                    )

                    // Trigger generation whenever the state is None and AI is available –
                    // this covers both first load and subsequent input changes.
                    if (aiAvailable && whatsNextState is WhatsNextState.None) {
                        generateWhatsNext(trip, destinations)
                    }
                }
        }
    }

    /**
     * Triggers AI description generation, replacing the current description state.
     *
     * No-op if generation is already in progress.
     */
    fun regenerateDescription() {
        val current = _uiState.value as? TripDetailUiState.Success ?: return
        if (current.descriptionState is DescriptionState.Loading) return
        val trip = lastTrip ?: return
        val destinations = lastDestinations
        _uiState.value = current.copy(descriptionState = DescriptionState.Loading)
        generateDescription(trip, destinations)
    }

    /**
     * Clears the current AI description and removes it from the database.
     * Reverts to the previous state if the DB update fails.
     */
    fun deleteDescription() {
        val current = _uiState.value as? TripDetailUiState.Success ?: return
        val trip = lastTrip ?: return
        val previousDescriptionState = current.descriptionState
        _uiState.value = current.copy(descriptionState = DescriptionState.None)
        viewModelScope.launch {
            try {
                saveTripDescriptionUseCase(trip, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete trip description", e)
                val latest = _uiState.value
                if (latest !is TripDetailUiState.Success) return@launch
                if (latest.descriptionState !is DescriptionState.None) return@launch
                _uiState.value = latest.copy(descriptionState = previousDescriptionState)
            }
        }
    }

    /**
     * Re-triggers "what's next" generation with the current date and time.
     *
     * No-op if generation is already in progress.
     */
    fun refreshWhatsNext() {
        val current = _uiState.value as? TripDetailUiState.Success ?: return
        if (current.whatsNextState is WhatsNextState.Loading) return
        val trip = lastTrip ?: return
        val destinations = lastDestinations
        _uiState.value = current.copy(whatsNextState = WhatsNextState.Loading)
        generateWhatsNext(trip, destinations)
    }

    private fun generateDescription(trip: Trip, destinations: List<Destination>) {
        viewModelScope.launch {
            val text = try {
                generateTripDescriptionUseCase(trip, destinations)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate trip description", e)
                val current = _uiState.value
                if (current is TripDetailUiState.Success) {
                    _uiState.value = current.copy(descriptionState = DescriptionState.Error)
                }
                return@launch
            }

            if (text == null) {
                val current = _uiState.value
                if (current is TripDetailUiState.Success) {
                    _uiState.value = current.copy(descriptionState = DescriptionState.Unavailable)
                }
                return@launch
            }

            // Show the generated text in the UI immediately, even if the DB save fails.
            val current = _uiState.value
            if (current is TripDetailUiState.Success) {
                _uiState.value = current.copy(descriptionState = DescriptionState.Available(text))
            }

            try {
                saveTripDescriptionUseCase(trip, text)
            } catch (e: Exception) {
                Log.e(TAG, "Generated description displayed but not saved to database", e)
            }
        }
    }

    private fun generateWhatsNext(trip: Trip, destinations: List<Destination>) {
        val current = _uiState.value as? TripDetailUiState.Success ?: return
        // Record the inputs before launching so stale-check is correct even if the coroutine is
        // cancelled, and only after confirming this call will actually proceed.
        whatsNextGeneratedForInputs = Pair(trip, destinations)
        _uiState.value = current.copy(whatsNextState = WhatsNextState.Loading)
        viewModelScope.launch {
            val text = try {
                generateWhatsNextUseCase(trip, destinations)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate what's next notice", e)
                val latest = _uiState.value
                if (latest is TripDetailUiState.Success) {
                    _uiState.value = latest.copy(whatsNextState = WhatsNextState.Error)
                }
                return@launch
            }

            if (text == null) {
                val latest = _uiState.value
                if (latest is TripDetailUiState.Success) {
                    _uiState.value = latest.copy(whatsNextState = WhatsNextState.Unavailable)
                }
                return@launch
            }

            val latest = _uiState.value
            if (latest is TripDetailUiState.Success) {
                _uiState.value = latest.copy(whatsNextState = WhatsNextState.Available(text))
            }
        }
    }

    companion object {
        private const val TAG = "TripDetailViewModel"
    }
}
