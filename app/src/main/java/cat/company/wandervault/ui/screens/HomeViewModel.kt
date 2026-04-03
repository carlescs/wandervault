package cat.company.wandervault.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.ImageSearchResult
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.usecase.CopyImageToInternalStorageUseCase
import cat.company.wandervault.domain.usecase.DeleteImageUseCase
import cat.company.wandervault.domain.usecase.DeleteTripUseCase
import cat.company.wandervault.domain.usecase.ArchiveTripUseCase
import cat.company.wandervault.domain.usecase.DownloadImageUseCase
import cat.company.wandervault.domain.usecase.GetTripsUseCase
import cat.company.wandervault.domain.usecase.SaveTripUseCase
import cat.company.wandervault.domain.usecase.SearchImagesUseCase
import cat.company.wandervault.domain.usecase.ToggleFavoriteTripUseCase
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
    private val deleteTrip: DeleteTripUseCase,
    private val toggleFavorite: ToggleFavoriteTripUseCase,
    private val archiveTrip: ArchiveTripUseCase,
    private val searchImages: SearchImagesUseCase,
    private val downloadImage: DownloadImageUseCase,
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
                addTripTimezone = null,
            )
        }
    }

    fun onAddTripTitleChange(title: String) {
        _uiState.update { it.copy(addTripTitle = title) }
    }

    fun onAddTripTimezoneChange(timezone: String?) {
        _uiState.update { it.copy(addTripTimezone = timezone) }
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
                    defaultTimezone = state.addTripTimezone,
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
                editTripTimezone = trip.defaultTimezone,
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
                editTripTimezone = null,
            )
        }
    }

    fun onEditTripTitleChange(title: String) {
        _uiState.update { it.copy(editTripTitle = title) }
    }

    fun onEditTripImageUriChange(uri: String?) {
        _uiState.update { it.copy(editTripImageUri = uri) }
    }

    fun onEditTripTimezoneChange(timezone: String?) {
        _uiState.update { it.copy(editTripTimezone = timezone) }
    }

    fun onUpdateTrip() {
        val state = _uiState.value
        if (!state.isEditTripFormValid) return
        val id = state.editTripId ?: return
        val existingTrip = state.trips.find { it.id == id } ?: return

        viewModelScope.launch {
            val newImageUri = persistImageUri(state.editTripImageUri)
            val oldImageUri = state.editTripOriginalImageUri
            if (oldImageUri != null && oldImageUri.startsWith("file://") && oldImageUri != newImageUri) {
                deleteImage(oldImageUri)
            }
            updateTrip(
                existingTrip.copy(
                    title = state.editTripTitle,
                    imageUri = newImageUri,
                    defaultTimezone = state.editTripTimezone,
                ),
            )
            onDismissEditTripDialog()
        }
    }

    private suspend fun persistImageUri(uri: String?): String? =
        if (uri != null && uri.startsWith("content://")) copyImage(uri) else uri

    fun onDeleteTripClick(trip: Trip) {
        _uiState.update { it.copy(tripToDelete = trip) }
    }

    fun onDismissDeleteTripDialog() {
        _uiState.update { it.copy(tripToDelete = null) }
    }

    fun onConfirmDeleteTrip() {
        val trip = _uiState.value.tripToDelete ?: return
        _uiState.update { it.copy(tripToDelete = null) }
        viewModelScope.launch {
            try {
                deleteTrip(trip)
            } catch (e: Exception) {
                // Trip deletion failed; physical files are preserved to maintain consistency.
                Log.e(TAG, "Failed to delete trip", e)
            }
        }
    }

    fun onToggleFavorite(trip: Trip) {
        viewModelScope.launch {
            toggleFavorite(trip.id)
        }
    }

    fun onArchiveTrip(trip: Trip) {
        viewModelScope.launch {
            archiveTrip(trip.id)
        }
    }

    fun onOpenImageSearch(forAdd: Boolean) {
        _uiState.update {
            it.copy(
                showImageSearchDialog = true,
                imageSearchForAdd = forAdd,
                imageSearchQuery = "",
                imageSearchResults = emptyList(),
                imageSearchLoading = false,
                imageSearchError = false,
                imageSearchNoResults = false,
                imageDownloadError = false,
            )
        }
    }

    fun onDismissImageSearch() {
        _uiState.update {
            it.copy(
                showImageSearchDialog = false,
                imageSearchQuery = "",
                imageSearchResults = emptyList(),
                imageSearchLoading = false,
                imageSearchError = false,
                imageSearchNoResults = false,
                imageDownloadError = false,
            )
        }
    }

    fun onImageSearchQueryChange(query: String) {
        _uiState.update { it.copy(imageSearchQuery = query, imageSearchError = false, imageSearchNoResults = false) }
    }

    fun onSearchImages() {
        val query = _uiState.value.imageSearchQuery.trim()
        if (query.isBlank()) return
        _uiState.update {
            it.copy(
                imageSearchLoading = true,
                imageSearchError = false,
                imageSearchNoResults = false,
                imageSearchResults = emptyList(),
            )
        }
        viewModelScope.launch {
            searchImages(query).fold(
                onSuccess = { results ->
                    _uiState.update {
                        it.copy(
                            imageSearchLoading = false,
                            imageSearchResults = results,
                            imageSearchNoResults = results.isEmpty(),
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            imageSearchLoading = false,
                            imageSearchError = true,
                        )
                    }
                },
            )
        }
    }

    fun onSelectSearchImage(result: ImageSearchResult, isAddDialog: Boolean) {
        _uiState.update { it.copy(imageDownloading = true, imageDownloadError = false) }
        viewModelScope.launch {
            val fileUri = downloadImage(result.fullUrl)
            if (fileUri != null) {
                if (isAddDialog) {
                    _uiState.update { it.copy(addTripImageUri = fileUri, imageDownloading = false) }
                } else {
                    _uiState.update { it.copy(editTripImageUri = fileUri, imageDownloading = false) }
                }
                onDismissImageSearch()
            } else {
                _uiState.update { it.copy(imageDownloading = false, imageDownloadError = true) }
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
