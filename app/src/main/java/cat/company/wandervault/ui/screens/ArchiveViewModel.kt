package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.usecase.ArchiveTripUseCase
import cat.company.wandervault.domain.usecase.GetArchivedTripsUseCase
import cat.company.wandervault.domain.usecase.UnarchiveTripUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArchiveViewModel(
    private val getArchivedTrips: GetArchivedTripsUseCase,
    private val unarchiveTrip: UnarchiveTripUseCase,
    private val archiveTrip: ArchiveTripUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getArchivedTrips().collect { trips ->
                _uiState.update { it.copy(trips = trips, isLoading = false) }
            }
        }
    }

    fun onUnarchiveTrip(trip: Trip) {
        viewModelScope.launch {
            unarchiveTrip(trip.id)
            _uiState.update { it.copy(pendingUnarchiveUndo = trip) }
        }
    }

    fun onUndoUnarchive() {
        val trip = _uiState.value.pendingUnarchiveUndo ?: return
        viewModelScope.launch {
            archiveTrip(trip.id)
            _uiState.update { it.copy(pendingUnarchiveUndo = null) }
        }
    }

    fun onDismissUnarchiveSnackbar() {
        _uiState.update { it.copy(pendingUnarchiveUndo = null) }
    }
}
