package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.usecase.GetTripsUseCase
import cat.company.wandervault.domain.usecase.SaveTripUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val getTrips: GetTripsUseCase,
    private val saveTrip: SaveTripUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTrips().collect { trips ->
                _uiState.update { it.copy(trips = trips) }
            }
        }
    }

    fun onAddTripClick() {
        _uiState.update { it.copy(showAddTripDialog = true) }
    }

    fun onDismissAddTripDialog() {
        _uiState.update {
            it.copy(
                showAddTripDialog = false,
                addTripTitle = "",
                addTripStartDate = null,
                addTripEndDate = null,
            )
        }
    }

    fun onAddTripTitleChange(title: String) {
        _uiState.update { it.copy(addTripTitle = title) }
    }

    fun onAddTripStartDateChange(date: LocalDate) {
        _uiState.update { it.copy(addTripStartDate = date) }
    }

    fun onAddTripEndDateChange(date: LocalDate) {
        _uiState.update { it.copy(addTripEndDate = date) }
    }

    fun onSaveTrip() {
        val state = _uiState.value
        val startDate = state.addTripStartDate ?: return
        val endDate = state.addTripEndDate ?: return
        if (state.addTripTitle.isBlank()) return

        viewModelScope.launch {
            saveTrip(
                Trip(
                    id = 0,
                    title = state.addTripTitle,
                    startDate = startDate,
                    endDate = endDate,
                ),
            )
            onDismissAddTripDialog()
        }
    }
}
