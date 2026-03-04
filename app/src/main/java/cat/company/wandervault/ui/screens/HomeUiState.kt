package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Trip

data class HomeUiState(
    val trips: List<Trip> = emptyList(),
    val showAddTripDialog: Boolean = false,
    val addTripTitle: String = "",
    val addTripImageUri: String? = null,
    val showEditTripDialog: Boolean = false,
    val editTripId: Int? = null,
    val editTripTitle: String = "",
    val editTripImageUri: String? = null,
    val editTripOriginalImageUri: String? = null,
) {
    val isAddTripFormValid: Boolean
        get() = addTripTitle.isNotBlank()

    val isEditTripFormValid: Boolean
        get() = editTripTitle.isNotBlank()
}
