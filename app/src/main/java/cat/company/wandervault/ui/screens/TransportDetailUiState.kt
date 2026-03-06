package cat.company.wandervault.ui.screens

/**
 * Represents the editing state for a single transport leg.
 *
 * @param id Database ID of the existing transport record, or 0 if this leg is not yet persisted.
 * @param typeName The [cat.company.wandervault.domain.model.TransportType] name, or `null` if no
 *   transport type has been selected for this leg.
 * @param company The carrier or company name.
 * @param flightNumber The flight, train, or route number.
 * @param confirmationNumber The booking or reservation confirmation code.
 */
data class TransportLegEditState(
    val id: Int = 0,
    val typeName: String? = null,
    val company: String = "",
    val flightNumber: String = "",
    val confirmationNumber: String = "",
)

/**
 * Represents the UI state for the Transport Detail screen.
 */
sealed class TransportDetailUiState {
    /** The destination and its transport legs are being loaded. */
    data object Loading : TransportDetailUiState()

    /**
     * The destination was loaded successfully; [destinationName] is used as the screen title
     * and [legs] holds the current (possibly dirty) list of transport leg editing states.
     */
    data class Success(
        val destinationName: String,
        val legs: List<TransportLegEditState>,
    ) : TransportDetailUiState()

    /** An error occurred (e.g. destination not found). */
    data object Error : TransportDetailUiState()
}
