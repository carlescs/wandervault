package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Destination

/**
 * UI state for the Itinerary tab on the Trip Detail screen.
 */
data class ItineraryUiState(
    val destinations: List<Destination> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDestinationDialog: Boolean = false,
    val newDestinationName: String = "",
    val newDestinationLatitude: String = "",
    val newDestinationLongitude: String = "",
    /** Position after which a new destination will be inserted; `null` means append at the end. */
    val insertAfterPosition: Int? = null,
    /** Destination awaiting delete confirmation; `null` when no confirmation dialog is shown. */
    val destinationPendingDelete: Destination? = null,
) {
    val isAddDestinationFormValid: Boolean
        get() = newDestinationName.isNotBlank()
}
