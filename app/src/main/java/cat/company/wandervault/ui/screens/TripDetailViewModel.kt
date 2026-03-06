package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.usecase.GenerateTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
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
 */
class TripDetailViewModel(
    private val tripId: Int,
    private val getTripUseCase: GetTripUseCase,
    private val getDestinationsForTripUseCase: GetDestinationsForTripUseCase,
    private val generateTripDescriptionUseCase: GenerateTripDescriptionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TripDetailUiState>(TripDetailUiState.Loading)
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    /** True once description generation has been kicked off, to avoid duplicate requests. */
    private var descriptionGenerated = false

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
                    // Preserve the description state across DB-driven re-emissions
                    val currentDescription =
                        (_uiState.value as? TripDetailUiState.Success)?.descriptionState
                    _uiState.value = TripDetailUiState.Success(
                        trip = trip,
                        descriptionState = currentDescription ?: DescriptionState.Loading,
                    )
                    // Generate the description only once per ViewModel lifetime
                    if (!descriptionGenerated) {
                        descriptionGenerated = true
                        generateDescription(trip, destinations)
                    }
                }
        }
    }

    private fun generateDescription(trip: Trip, destinations: List<Destination>) {
        viewModelScope.launch {
            val newDescriptionState = try {
                val text = generateTripDescriptionUseCase(trip, destinations)
                if (text != null) DescriptionState.Available(text) else DescriptionState.Unavailable
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
