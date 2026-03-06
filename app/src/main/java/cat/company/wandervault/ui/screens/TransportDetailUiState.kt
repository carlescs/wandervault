package cat.company.wandervault.ui.screens

/**
 * Represents the editing state for a single transport leg.
 *
 * @param id Database ID of the existing transport record, or 0 if this leg is not yet persisted.
 * @param clientKey A stable identifier used as a Compose key for this item in the reorderable
 *   leg list.  For persisted legs this equals [id]; for new (unsaved) legs the ViewModel assigns
 *   a unique negative value so that Compose can distinguish items after reordering or deletion.
 * @param typeName The [cat.company.wandervault.domain.model.TransportType] name, or `null` if no
 *   transport type has been selected for this leg.
 * @param stopName The name of the stop or place where this leg ends (e.g. an intermediate city).
 * @param company The carrier or company name.
 * @param flightNumber The flight, train, or route number.
 * @param confirmationNumber The booking or reservation confirmation code.
 */
data class TransportLegEditState(
    val id: Int = 0,
    val clientKey: Int = id,
    val typeName: String? = null,
    val stopName: String = "",
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
     * The destination was loaded successfully.
     *
     * @param destinationName Name of the current destination (used as the screen title).
     * @param originName Name of the origin stop (the departure point of the transport, equal to
     *   [destinationName]).
     * @param nextDestinationName Name of the next destination in the trip itinerary (the overall
     *   arrival point of the transport), or `null` if this is the last stop.
     * @param legs The current (possibly dirty) list of transport leg editing states.
     */
    data class Success(
        val destinationName: String,
        val originName: String = destinationName,
        val nextDestinationName: String? = null,
        val legs: List<TransportLegEditState>,
    ) : TransportDetailUiState()

    /** An error occurred (e.g. destination not found). */
    data object Error : TransportDetailUiState()
}
