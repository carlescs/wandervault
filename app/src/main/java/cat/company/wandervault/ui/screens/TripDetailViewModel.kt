package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.usecase.DeleteTripDocumentUseCase
import cat.company.wandervault.domain.usecase.GenerateTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetTripDocumentsUseCase
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
 * @param getTripDocumentsUseCase Use-case that returns the documents attached to the trip.
 * @param deleteTripDocumentUseCase Use-case that deletes a document from the trip.
 */
class TripDetailViewModel(
    private val tripId: Int,
    private val getTripUseCase: GetTripUseCase,
    private val getDestinationsForTripUseCase: GetDestinationsForTripUseCase,
    private val generateTripDescriptionUseCase: GenerateTripDescriptionUseCase,
    private val saveTripDescriptionUseCase: SaveTripDescriptionUseCase,
    private val getTripDocumentsUseCase: GetTripDocumentsUseCase,
    private val deleteTripDocumentUseCase: DeleteTripDocumentUseCase,
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
                getTripDocumentsUseCase(tripId),
            ) { trip, destinations, documents -> Triple(trip, destinations, documents) }
                .collect { (trip, destinations, documents) ->
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
                    val persistedDescription = if (trip.aiDescription != null) {
                        DescriptionState.Available(trip.aiDescription)
                    } else {
                        DescriptionState.None
                    }
                    // Only preserve Loading to avoid a flicker while generation is in progress;
                    // for all other states the DB value is the source of truth.
                    val descriptionState = if (currentDescription is DescriptionState.Loading) {
                        currentDescription
                    } else {
                        persistedDescription
                    }
                    _uiState.value = TripDetailUiState.Success(
                        trip = trip,
                        descriptionState = descriptionState,
                        documents = documents,
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

    /** Deletes a document from the trip. */
    fun deleteDocument(documentId: Int) {
        val current = _uiState.value as? TripDetailUiState.Success ?: return
        val document = current.documents.firstOrNull { it.id == documentId } ?: return
        viewModelScope.launch {
            try {
                deleteTripDocumentUseCase(document)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete document", e)
            }
        }
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

    companion object {
        private const val TAG = "TripDetailViewModel"
    }
}

