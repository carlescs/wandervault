package cat.company.wandervault.ui.screens

/**
 * Represents the current editing state for the transport leg on the Transport Detail screen.
 *
 * @param typeName The [cat.company.wandervault.domain.model.TransportType] name, or `null` if no
 *   transport is selected.
 * @param company The carrier or company name.
 * @param flightNumber The flight, train, or route number.
 * @param confirmationNumber The booking or reservation confirmation code.
 */
data class TransportDetailEditState(
    val typeName: String? = null,
    val company: String = "",
    val flightNumber: String = "",
    val confirmationNumber: String = "",
)

/**
 * Represents the UI state for the Transport Detail screen.
 */
sealed class TransportDetailUiState {
    /** The destination and its transport are being loaded. */
    data object Loading : TransportDetailUiState()

    /**
     * The destination was loaded successfully; [destinationName] is used as the screen title
     * and [editState] holds the current (possibly dirty) editing values.
     */
    data class Success(
        val destinationName: String,
        val editState: TransportDetailEditState,
    ) : TransportDetailUiState()

    /** An error occurred (e.g. destination not found). */
    data object Error : TransportDetailUiState()
}
