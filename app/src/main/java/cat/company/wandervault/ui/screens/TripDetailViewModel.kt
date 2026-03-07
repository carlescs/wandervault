package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.usecase.GenerateTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.SaveTripDescriptionUseCase
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
 */
class TripDetailViewModel(
    private val tripId: Int,
    private val getTripUseCase: GetTripUseCase,
    private val getDestinationsForTripUseCase: GetDestinationsForTripUseCase,
    private val generateTripDescriptionUseCase: GenerateTripDescriptionUseCase,
    private val saveTripDescriptionUseCase: SaveTripDescriptionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TripDetailUiState>(TripDetailUiState.Loading)
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    /** Cached trip used for on-demand generation/deletion triggered by the user. */
    private var lastTrip: Trip? = null

    /** Cached destinations used for on-demand re-generation triggered by the user. */
    private var lastDestinations: List<Destination> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                getTripUseCase(tripId),
                getDestinationsForTripUseCase(tripId),
            ) { trip, destinations -> Pair(trip, destinations) }
                .collect { (trip, destinations) ->
                    if (trip == null) {
                        _uiState.value = TripDetailUiState.Error
                        return@collect
                    }
                    lastTrip = trip
                    lastDestinations = destinations
                    // Preserve in-memory description state (e.g. while loading/generating).
                    // On first load, derive the initial state from the persisted description.
                    val currentDescription =
                        (_uiState.value as? TripDetailUiState.Success)?.descriptionState
                    val persistedDescription = if (trip.aiDescription != null) {
                        DescriptionState.Available(trip.aiDescription)
                    } else {
                        DescriptionState.None
                    }
                    _uiState.value = TripDetailUiState.Success(
                        trip = trip,
                        descriptionState = currentDescription ?: persistedDescription,
                    )
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
     */
    fun deleteDescription() {
        val current = _uiState.value as? TripDetailUiState.Success ?: return
        val trip = lastTrip ?: return
        _uiState.value = current.copy(descriptionState = DescriptionState.None)
        viewModelScope.launch {
            saveTripDescriptionUseCase(trip, null)
        }
    }

    private fun generateDescription(trip: Trip, destinations: List<Destination>) {
        viewModelScope.launch {
            val newDescriptionState = try {
                val text = generateTripDescriptionUseCase(trip, destinations)
                if (text != null) {
                    val currentTrip = lastTrip
                    if (currentTrip != null) {
                        saveTripDescriptionUseCase(currentTrip, text)
                    }
                    DescriptionState.Available(text)
                } else {
                    DescriptionState.Unavailable
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate trip description", e)
                DescriptionState.Error
            }
            val current = _uiState.value
            if (current is TripDetailUiState.Success) {
                _uiState.value = current.copy(descriptionState = newDescriptionState)
            }
        }
    }

    companion object {
        private const val TAG = "TripDetailViewModel"
    }
}
