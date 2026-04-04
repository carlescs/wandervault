package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.TransportLeg

/**
 * State of an in-progress or completed document analysis, shown in the document analysis dialog.
 */
sealed class AnalyzeDocumentUiState {
    /** ML Kit analysis is running (reading the document and sending it to the model). */
    data object Loading : AnalyzeDocumentUiState()

    /**
     * The Gemini Nano model weights are being downloaded to the device before analysis can begin.
     *
     * @param bytesDownloaded Total bytes of model data downloaded so far.
     */
    data class Downloading(val bytesDownloaded: Long) : AnalyzeDocumentUiState()

    /**
     * AI analysis is not available on this device or the document content could not be read.
     * This is a permanent state — retrying will not help.
     */
    data object Unavailable : AnalyzeDocumentUiState()

    /**
     * Analysis failed with a transient error. The user may try again.
     *
     * @param message Optional error detail (e.g. the exception message) that can be shown in
     *   the dialog to help diagnose the problem.
     */
    data class Error(val message: String? = null) : AnalyzeDocumentUiState()

    /**
     * A confident match was found for the extracted flight info.
     * The user reviews what will be updated and confirms or cancels.
     *
     * @param flightInfo The extracted flight details.
     * @param matchedLeg The flight leg that will be updated.
     */
    data class FlightConfirm(
        val flightInfo: FlightInfo,
        val matchedLeg: TransportLeg,
    ) : AnalyzeDocumentUiState()

    /**
     * ML Kit extracted flight information but could not find a confident match in the trip's
     * itinerary. The user must select one of [candidates] to apply the data to, or skip.
     * Candidates are sorted by relevance (closest partial match first).
     *
     * @param flightInfo The extracted flight details.
     * @param candidates Flight legs available in the selected trip, ordered by relevance.
     */
    data class FlightLegSelection(
        val flightInfo: FlightInfo,
        val candidates: List<TransportLeg>,
    ) : AnalyzeDocumentUiState()

    /**
     * ML Kit extracted flight information but no flight legs exist in the trip. The user
     * selects which non-terminal destination to add a new flight leg to (a transport is created
     * for the destination if one does not already exist), or skips.
     *
     * @param flightInfo The extracted flight details.
     * @param candidates Non-terminal destinations in the trip (all but the last, ordered by
     *   position). The terminal destination is excluded because it has no onward transport.
     */
    data class FlightTransportSelection(
        val flightInfo: FlightInfo,
        val candidates: List<Destination>,
    ) : AnalyzeDocumentUiState()

    /**
     * A transport has been selected to receive a new flight leg.
     * The user reviews what will be added and confirms or cancels.
     *
     * @param flightInfo The extracted flight details.
     * @param destination The destination whose transport will receive the new leg.
     */
    data class FlightAddLegConfirm(
        val flightInfo: FlightInfo,
        val destination: Destination,
    ) : AnalyzeDocumentUiState()

    /**
     * A confident match was found for the extracted hotel info.
     * The user reviews what will be updated and confirms or cancels.
     *
     * @param hotelInfo The extracted hotel details.
     * @param destination The destination whose hotel will be updated.
     * @param existingHotel The hotel record that will be updated, or `null` if a new one will be created.
     */
    data class HotelConfirm(
        val hotelInfo: HotelInfo,
        val destination: Destination,
        val existingHotel: Hotel?,
    ) : AnalyzeDocumentUiState()

    /**
     * ML Kit extracted hotel information but could not find a confident match in the trip's
     * itinerary. The user must select one of [candidates] to apply the data to, or skip.
     *
     * @param hotelInfo The extracted hotel details.
     * @param candidates Destinations available in the selected trip.
     */
    data class HotelDestinationSelection(
        val hotelInfo: HotelInfo,
        val candidates: List<Destination>,
    ) : AnalyzeDocumentUiState()

    /**
     * ML Kit extracted general trip-relevant information (no flight or hotel document detected).
     * The user reviews what will be saved as the trip's AI description and confirms or cancels.
     *
     * @param relevantTripInfo The extracted general trip info text that will be saved.
     */
    data class TripInfoConfirm(val relevantTripInfo: String) : AnalyzeDocumentUiState()
}

/**
 * Returns `true` when the analysis is in a progress-only state ([AnalyzeDocumentUiState.Loading]
 * or [AnalyzeDocumentUiState.Downloading]) that is shown inline rather than in a dialog.
 */
val AnalyzeDocumentUiState.isInProgress: Boolean
    get() = this is AnalyzeDocumentUiState.Loading || this is AnalyzeDocumentUiState.Downloading
