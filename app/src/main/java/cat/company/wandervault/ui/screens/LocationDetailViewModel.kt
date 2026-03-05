package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.usecase.DeleteHotelUseCase
import cat.company.wandervault.domain.usecase.GetHotelForDestinationUseCase
import cat.company.wandervault.domain.usecase.SaveHotelUseCase
import cat.company.wandervault.domain.usecase.UpdateHotelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for the Location Detail screen.
 *
 * Reactively loads the [Hotel] for the given [destination] and manages the hotel reservation form.
 *
 * @param destination The destination whose details are being viewed.
 * @param getHotelForDestination Use-case that returns a live [Hotel] for the destination.
 * @param saveHotel Use-case that persists a new hotel reservation.
 * @param updateHotel Use-case that updates an existing hotel reservation.
 * @param deleteHotel Use-case that removes a hotel reservation.
 */
class LocationDetailViewModel(
    private val destination: Destination,
    private val getHotelForDestination: GetHotelForDestinationUseCase,
    private val saveHotel: SaveHotelUseCase,
    private val updateHotel: UpdateHotelUseCase,
    private val deleteHotel: DeleteHotelUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationDetailUiState(destination = destination))
    val uiState: StateFlow<LocationDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getHotelForDestination(destination.id).collect { hotel ->
                _uiState.update { state ->
                    val updatedState = state.copy(hotel = hotel, isLoading = false)
                    if (!state.isHotelFormDirty && hotel != null) {
                        updatedState.copy(
                            hotelName = hotel.name,
                            hotelAddress = hotel.address,
                            hotelCheckInDate = hotel.checkInDate,
                            hotelCheckOutDate = hotel.checkOutDate,
                            hotelConfirmationNumber = hotel.confirmationNumber,
                            hotelNotes = hotel.notes,
                        )
                    } else if (!state.isHotelFormDirty && hotel == null) {
                        updatedState.copy(
                            hotelName = "",
                            hotelAddress = "",
                            hotelCheckInDate = null,
                            hotelCheckOutDate = null,
                            hotelConfirmationNumber = "",
                            hotelNotes = "",
                        )
                    } else {
                        updatedState
                    }
                }
            }
        }
    }

    fun onHotelNameChange(name: String) {
        _uiState.update { it.copy(hotelName = name, isHotelFormDirty = true) }
    }

    fun onHotelAddressChange(address: String) {
        _uiState.update { it.copy(hotelAddress = address, isHotelFormDirty = true) }
    }

    fun onHotelCheckInDateChange(date: LocalDate?) {
        _uiState.update { it.copy(hotelCheckInDate = date, isHotelFormDirty = true) }
    }

    fun onHotelCheckOutDateChange(date: LocalDate?) {
        _uiState.update { it.copy(hotelCheckOutDate = date, isHotelFormDirty = true) }
    }

    fun onHotelConfirmationNumberChange(number: String) {
        _uiState.update { it.copy(hotelConfirmationNumber = number, isHotelFormDirty = true) }
    }

    fun onHotelNotesChange(notes: String) {
        _uiState.update { it.copy(hotelNotes = notes, isHotelFormDirty = true) }
    }

    fun onSaveHotel() {
        val state = _uiState.value
        viewModelScope.launch {
            val hotel = Hotel(
                id = state.hotel?.id ?: 0,
                destinationId = destination.id,
                name = state.hotelName.trim(),
                address = state.hotelAddress.trim(),
                checkInDate = state.hotelCheckInDate,
                checkOutDate = state.hotelCheckOutDate,
                confirmationNumber = state.hotelConfirmationNumber.trim(),
                notes = state.hotelNotes.trim(),
            )
            if (state.hotel == null) {
                saveHotel(hotel)
            } else {
                updateHotel(hotel)
            }
            _uiState.update { it.copy(isHotelFormDirty = false) }
        }
    }

    fun onDeleteHotel() {
        val hotel = _uiState.value.hotel ?: return
        viewModelScope.launch {
            deleteHotel(hotel)
            _uiState.update { it.copy(isHotelFormDirty = false) }
        }
    }
}
