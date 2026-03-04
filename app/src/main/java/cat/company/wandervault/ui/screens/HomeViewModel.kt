package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.usecase.CopyImageToInternalStorageUseCase
import cat.company.wandervault.domain.usecase.DeleteImageUseCase
import cat.company.wandervault.domain.usecase.GetTripsUseCase
import cat.company.wandervault.domain.usecase.SaveTripUseCase
import cat.company.wandervault.domain.usecase.UpdateTripUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getTrips: GetTripsUseCase,
    private val saveTrip: SaveTripUseCase,
    private val updateTrip: UpdateTripUseCase,
    private val copyImage: CopyImageToInternalStorageUseCase,
    private val deleteImage: DeleteImageUseCase,
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
                addTripImageUri = null,
            )
        }
    }

    fun onAddTripTitleChange(title: String) {
        _uiState.update { it.copy(addTripTitle = title) }
    }

    fun onAddTripImageUriChange(uri: String?) {
        _uiState.update { it.copy(addTripImageUri = uri) }
    }

    fun onSaveTrip() {
        val state = _uiState.value
        if (!state.isAddTripFormValid) return

        viewModelScope.launch {
            saveTrip(
                Trip(
                    id = 0,
                    title = state.addTripTitle,
                    imageUri = persistImageUri(state.addTripImageUri),
                ),
            )
            onDismissAddTripDialog()
        }
    }

    fun onEditTripClick(trip: Trip) {
        _uiState.update {
            it.copy(
                showEditTripDialog = true,
                editTripId = trip.id,
                editTripTitle = trip.title,
                editTripImageUri = trip.imageUri,
                editTripOriginalImageUri = trip.imageUri,
            )
        }
    }

    fun onDismissEditTripDialog() {
        _uiState.update {
            it.copy(
                showEditTripDialog = false,
                editTripId = null,
                editTripTitle = "",
                editTripImageUri = null,
                editTripOriginalImageUri = null,
            )
        }
    }

    fun onEditTripTitleChange(title: String) {
        _uiState.update { it.copy(editTripTitle = title) }
    }

    fun onEditTripImageUriChange(uri: String?) {
        _uiState.update { it.copy(editTripImageUri = uri) }
    }

    fun onUpdateTrip() {
        val state = _uiState.value
        if (!state.isEditTripFormValid) return
        val id = state.editTripId ?: return

        viewModelScope.launch {
            val newImageUri = persistImageUri(state.editTripImageUri)
            val oldImageUri = state.editTripOriginalImageUri
            if (oldImageUri != null && oldImageUri.startsWith("file://") && oldImageUri != newImageUri) {
                deleteImage(oldImageUri)
            }
            updateTrip(
                Trip(
                    id = id,
                    title = state.editTripTitle,
                    imageUri = newImageUri,
                ),
            )
            onDismissEditTripDialog()
        }
    }

    private suspend fun persistImageUri(uri: String?): String? =
        if (uri != null && uri.startsWith("content://")) copyImage(uri) else uri
}
