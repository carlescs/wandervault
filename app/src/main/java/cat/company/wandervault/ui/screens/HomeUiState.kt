package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Trip
import java.time.LocalDate

data class HomeUiState(
    val trips: List<Trip> = emptyList(),
    val showAddTripDialog: Boolean = false,
    val addTripTitle: String = "",
    val addTripStartDate: LocalDate? = null,
    val addTripEndDate: LocalDate? = null,
    val showEditTripDialog: Boolean = false,
    val editTripId: Int? = null,
    val editTripTitle: String = "",
    val editTripStartDate: LocalDate? = null,
    val editTripEndDate: LocalDate? = null,
) {
    val isAddTripFormValid: Boolean
        get() = addTripTitle.isNotBlank() &&
            addTripStartDate != null &&
            addTripEndDate != null &&
            !addTripEndDate.isBefore(addTripStartDate)

    val isEditTripFormValid: Boolean
        get() = editTripTitle.isNotBlank() &&
            editTripStartDate != null &&
            editTripEndDate != null &&
            !editTripEndDate.isBefore(editTripStartDate)
}
