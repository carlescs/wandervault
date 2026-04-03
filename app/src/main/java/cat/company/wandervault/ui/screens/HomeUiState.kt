package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.ImageSearchResult
import cat.company.wandervault.domain.model.Trip

data class HomeUiState(
    val trips: List<Trip> = emptyList(),
    val showAddTripDialog: Boolean = false,
    val addTripTitle: String = "",
    val addTripImageUri: String? = null,
    val addTripTimezone: String? = null,
    val showEditTripDialog: Boolean = false,
    val editTripId: Int? = null,
    val editTripTitle: String = "",
    val editTripImageUri: String? = null,
    val editTripOriginalImageUri: String? = null,
    val editTripTimezone: String? = null,
    val tripToDelete: Trip? = null,
    val showImageSearchDialog: Boolean = false,
    val imageSearchForAdd: Boolean = true,
    val imageSearchQuery: String = "",
    val imageSearchResults: List<ImageSearchResult> = emptyList(),
    val imageSearchLoading: Boolean = false,
    /** True when a network/API error prevented the search from completing. */
    val imageSearchError: Boolean = false,
    /** True when the search succeeded but returned no results. */
    val imageSearchNoResults: Boolean = false,
    val imageDownloading: Boolean = false,
    /** True when the selected image could not be downloaded. */
    val imageDownloadError: Boolean = false,
) {
    val isAddTripFormValid: Boolean
        get() = addTripTitle.isNotBlank()

    val isEditTripFormValid: Boolean
        get() = editTripTitle.isNotBlank()
}
