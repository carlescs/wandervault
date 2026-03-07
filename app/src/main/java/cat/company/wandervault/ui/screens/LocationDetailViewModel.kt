package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.usecase.GetArrivalTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetDestinationByIdUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetHotelForDestinationUseCase
import cat.company.wandervault.domain.usecase.SaveHotelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel for the Location Detail screen.
 *
 * A single instance is reused across destinations. Call [loadDestination] whenever the
 * displayed destination changes; the ViewModel switches its collection to the new ID via
 * [flatMapLatest], cancelling the previous subscription automatically.
 *
 * Hotel edits are tracked in memory and auto-saved to the database after a short debounce.
 *
 * @param getDestinationById Use-case that fetches a single destination by ID.
 * @param getArrivalTransport Use-case that fetches the transport used to arrive at a destination.
 * @param getDestinationsForTrip Use-case that fetches all destinations for a trip, used to
 *   determine whether the destination is the first or last stop.
 * @param getHotelForDestination Use-case that fetches the hotel for a destination.
 * @param saveHotel Use-case that persists a hotel record.
 */
class LocationDetailViewModel(
    private val getDestinationById: GetDestinationByIdUseCase,
    private val getArrivalTransport: GetArrivalTransportForDestinationUseCase,
    private val getDestinationsForTrip: GetDestinationsForTripUseCase,
    private val getHotelForDestination: GetHotelForDestinationUseCase,
    private val saveHotel: SaveHotelUseCase,
) : ViewModel() {

    private val _destinationId = MutableStateFlow<Int?>(null)

    /**
     * True when the user has made unsaved hotel edits. While true, incoming DB emissions do not
     * overwrite the hotel edit state. Reset to false after a save so the next emission refreshes
     * from the persisted record (picking up the real DB-assigned ID on first insert).
     */
    private var _hasUnsavedHotelEdits = false

    private val _uiState = MutableStateFlow<LocationDetailUiState>(LocationDetailUiState.Loading)
    val uiState: StateFlow<LocationDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _destinationId
                .filterNotNull()
                .flatMapLatest { id ->
                    combine(
                        getDestinationById(id),
                        getArrivalTransport(id),
                        getHotelForDestination(id),
                    ) { destination, arrivalTransport, hotel ->
                        Triple(destination, arrivalTransport, hotel)
                    }.flatMapLatest { (destination, arrivalTransport, hotel) ->
                        if (destination == null) {
                            flowOf(LocationDetailUiState.Error)
                        } else {
                            getDestinationsForTrip(destination.tripId).map { tripDestinations ->
                                val firstPosition = tripDestinations.firstOrNull()?.position
                                val lastPosition = tripDestinations.lastOrNull()?.position
                                val isFirst = destination.position == firstPosition
                                val isLast = destination.position == lastPosition
                                val hotelEditState = if (_hasUnsavedHotelEdits &&
                                    _uiState.value is LocationDetailUiState.Success
                                ) {
                                    (_uiState.value as LocationDetailUiState.Success).hotelEditState
                                } else {
                                    hotel?.toEditState() ?: HotelEditState()
                                }
                                LocationDetailUiState.Success(
                                    destination = destination,
                                    arrivalTransport = arrivalTransport,
                                    isFirst = isFirst,
                                    isLast = isLast,
                                    hotelEditState = hotelEditState,
                                )
                            }
                        }
                    }
                }
                .collect { state -> _uiState.value = state }
        }

        // Auto-save: persist hotel changes after a short debounce once editing stops.
        viewModelScope.launch {
            _uiState
                .debounce(AUTO_SAVE_DEBOUNCE_MS)
                .collectLatest { persistHotel() }
        }
    }

    /** Switch the screen to display the destination with the given [id]. */
    fun loadDestination(id: Int) {
        if (_destinationId.value != id) {
            _uiState.value = LocationDetailUiState.Loading
            _hasUnsavedHotelEdits = false
            _destinationId.value = id
        }
    }

    /** Updates the hotel name field. */
    fun onHotelNameChange(value: String) {
        _hasUnsavedHotelEdits = true
        updateHotelEditState { copy(name = value) }
    }

    /** Updates the hotel address field. */
    fun onHotelAddressChange(value: String) {
        _hasUnsavedHotelEdits = true
        updateHotelEditState { copy(address = value) }
    }

    /** Updates the hotel reservation number field. */
    fun onHotelReservationNumberChange(value: String) {
        _hasUnsavedHotelEdits = true
        updateHotelEditState { copy(reservationNumber = value) }
    }

    /** Flushes any pending hotel edits immediately (e.g. on navigate-away). */
    fun onSave() {
        viewModelScope.launch { persistHotel() }
    }

    private suspend fun persistHotel() {
        if (!_hasUnsavedHotelEdits) return
        val state = _uiState.value as? LocationDetailUiState.Success ?: return
        val destinationId = state.destination.id
        val edit = state.hotelEditState
        _hasUnsavedHotelEdits = false

        // Only persist if there is at least one non-blank field.
        val hasContent = edit.name.isNotBlank() || edit.address.isNotBlank() || edit.reservationNumber.isNotBlank()
        if (hasContent) {
            saveHotel(
                Hotel(
                    id = edit.id,
                    destinationId = destinationId,
                    name = edit.name.trim(),
                    address = edit.address.trim(),
                    reservationNumber = edit.reservationNumber.trim(),
                ),
            )
        }
    }

    private inline fun updateHotelEditState(update: HotelEditState.() -> HotelEditState) {
        val current = _uiState.value as? LocationDetailUiState.Success ?: return
        _uiState.value = current.copy(hotelEditState = current.hotelEditState.update())
    }

    companion object {
        private const val AUTO_SAVE_DEBOUNCE_MS = 300L
    }
}

private fun Hotel.toEditState() = HotelEditState(
    id = id,
    name = name,
    address = address,
    reservationNumber = reservationNumber,
)

