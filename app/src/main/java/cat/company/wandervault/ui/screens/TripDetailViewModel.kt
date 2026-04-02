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
import cat.company.wandervault.domain.usecase.SaveTripWhatsNextUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * ViewModel for the Trip Detail screen.
 *
 * @param tripId The ID of the trip to display.
 * @param getTripUseCase Use-case that fetches a single trip by ID.
 * @param getDestinationsForTripUseCase Use-case that returns the ordered destinations for the trip.
 * @param generateTripDescriptionUseCase Use-case that generates a short AI description of the trip.
 * @param saveTripDescriptionUseCase Use-case that persists the AI description (or clears it).
 * @param generateWhatsNextUseCase Use-case that generates the "what's next" notice for the trip.
 * @param saveTripWhatsNextUseCase Use-case that persists the "what's next" notice and its deadline.
 */
class TripDetailViewModel(
    private val tripId: Int,
    private val getTripUseCase: GetTripUseCase,
    private val getDestinationsForTripUseCase: GetDestinationsForTripUseCase,
    private val generateTripDescriptionUseCase: GenerateTripDescriptionUseCase,
    private val saveTripDescriptionUseCase: SaveTripDescriptionUseCase,
    private val generateWhatsNextUseCase: GenerateWhatsNextUseCase,
    private val saveTripWhatsNextUseCase: SaveTripWhatsNextUseCase,
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
     * The itinerary key (trip without nextStep/nextStepDeadline, plus destinations) for which the
     * most recent "what's next" generation was started, or which was initialised from the persisted
     * DB value on first load.  `null` until the first generation is triggered or a persisted notice
     * is loaded.
     *
     * Used to detect when the trip/destinations inputs have changed so that the notice can be
     * invalidated and re-generated automatically, avoiding stale results after itinerary edits.
     * [nextStep] and [nextStepDeadline] are excluded from the comparison so that saving the
     * generated notice back to the DB does not falsely trigger re-generation.
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

                    val currentWhatsNext =
                        (_uiState.value as? TripDetailUiState.Success)?.whatsNextState

                    // Determine the effective persisted state for this emission.
                    val now = ZonedDateTime.now()
                    val isDeadlinePassed =
                        trip.nextStepDeadline?.let { !it.isAfter(now) } ?: false
                    val persistedWhatsNext: WhatsNextState = when {
                        trip.nextStep != null && !isDeadlinePassed ->
                            WhatsNextState.Available(trip.nextStep)
                        else -> WhatsNextState.None
                    }

                    // On the very first load, seed the inputs key from the DB so we don't
                    // immediately re-generate a notice that is still valid.
                    val itineraryKey = trip.itineraryKey()
                    if (whatsNextGeneratedForInputs == null &&
                        persistedWhatsNext is WhatsNextState.Available
                    ) {
                        whatsNextGeneratedForInputs = Pair(itineraryKey, destinations)
                    }

                    val inputsMatchLastGenerated =
                        whatsNextGeneratedForInputs?.let { (t, d) ->
                            t == itineraryKey && d == destinations
                        } ?: false

                    val whatsNextState = when {
                        currentWhatsNext is WhatsNextState.Loading -> currentWhatsNext
                        !aiAvailable -> WhatsNextState.Unavailable
                        // Preserve in-progress or just-generated state when inputs are unchanged.
                        inputsMatchLastGenerated -> currentWhatsNext ?: persistedWhatsNext
                        // Inputs changed: reset to None so auto-generation is triggered.
                        else -> WhatsNextState.None
                    }

                    _uiState.value = TripDetailUiState.Success(
                        trip = trip,
                        descriptionState = descriptionState,
                        whatsNextState = whatsNextState,
                    )

                    // Trigger generation whenever the state is None and AI is available –
                    // this covers first load (no stored notice), overdue deadlines, and
                    // subsequent input changes.
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
        // Record the itinerary key (excluding nextStep/nextStepDeadline) before launching so the
        // stale-check is correct even if the coroutine is cancelled.
        whatsNextGeneratedForInputs = Pair(trip.itineraryKey(), destinations)
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

            // Show the generated text in the UI immediately, even if the DB save fails.
            val latest = _uiState.value
            if (latest is TripDetailUiState.Success) {
                _uiState.value = latest.copy(whatsNextState = WhatsNextState.Available(text))
            }

            // Persist the notice along with the deadline so it survives app restarts and
            // auto-expires when the next upcoming itinerary event passes.
            val deadline = computeNextStepDeadline(destinations)
            try {
                saveTripWhatsNextUseCase(trip, text, deadline)
            } catch (e: Exception) {
                Log.e(TAG, "Generated what's next displayed but not saved to database", e)
            }
        }
    }

    /**
     * Returns the earliest upcoming destination event (arrival or departure) after the current
     * moment, used as the expiry deadline for the "what's next" notice.
     *
     * Returns `null` if there are no future events, which means the notice never auto-expires.
     */
    private fun computeNextStepDeadline(destinations: List<Destination>): ZonedDateTime? {
        val now = ZonedDateTime.now()
        return destinations
            .flatMap { listOf(it.arrivalDateTime, it.departureDateTime) }
            .filterNotNull()
            .filter { it.isAfter(now) }
            .minOrNull()
    }

    companion object {
        private const val TAG = "TripDetailViewModel"
    }
}

/**
 * Returns a copy of this trip with [Trip.nextStep] and [Trip.nextStepDeadline] cleared, used as
 * a stable key for detecting itinerary changes without being affected by persistence round-trips.
 */
private fun Trip.itineraryKey() = copy(nextStep = null, nextStepDeadline = null)

