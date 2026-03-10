package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.Trip

/**
 * UI state for the share-to-WanderVault flow.
 *
 * The flow transitions through these states:
 * 1. [Loading] – retrieving the trip list.
 * 2. [TripSelection] – user picks which trip to attach the document to.
 * 3. [Processing] – document is being copied and analysed by ML Kit.
 * 4. (optional) [FlightLegSelection] or [HotelDestinationSelection] – ML Kit found structured info
 *    but cannot confidently determine which itinerary element to update; user must choose.
 * 5. (optional) [FlightConfirm] or [HotelConfirm] – user reviews the proposed changes before
 *    they are saved; reached either from a confident AI match or from a user selection above.
 * 6. [Done] – everything applied; the sheet can be dismissed.
 * 7. [Error] – a non-recoverable error occurred.
 */
sealed class ShareUiState {

    /** The list of trips is being loaded. */
    data object Loading : ShareUiState()

    /**
     * Trips are available; waiting for the user to select one.
     *
     * @param trips All trips available for selection.
     * @param sourceUri The URI of the shared document (as received from the intent).
     * @param mimeType MIME type of the shared document.
     * @param documentName Display name derived from the shared document.
     */
    data class TripSelection(
        val trips: List<Trip>,
        val sourceUri: String,
        val mimeType: String,
        val documentName: String,
    ) : ShareUiState()

    /** The document is being copied to internal storage and analysed. */
    data object Processing : ShareUiState()

    /**
     * ML Kit extracted flight information but could not find a confident match in the trip's
     * itinerary. The user must select one of [candidates] to apply the data to, or skip.
     *
     * @param flightInfo The extracted flight details.
     * @param candidates FLIGHT-type transport legs available in the selected trip.
     */
    data class FlightLegSelection(
        val flightInfo: FlightInfo,
        val candidates: List<TransportLeg>,
    ) : ShareUiState()

    /**
     * A flight leg has been identified (either by confident match or explicit user selection).
     * The user reviews the proposed changes and confirms or cancels.
     *
     * @param flightInfo The extracted flight details.
     * @param matchedLeg The flight leg that will be updated.
     */
    data class FlightConfirm(
        val flightInfo: FlightInfo,
        val matchedLeg: TransportLeg,
    ) : ShareUiState()

    /**
     * ML Kit extracted hotel information but could not find a confident match in the trip's
     * itinerary. The user must select one of [candidates] to apply the data to, or skip.
     *
     * @param hotelInfo The extracted hotel details.
     * @param candidates Destinations (with optional existing hotel) available in the selected trip.
     */
    data class HotelDestinationSelection(
        val hotelInfo: HotelInfo,
        val candidates: List<Destination>,
    ) : ShareUiState()

    /**
     * A destination has been identified (either by confident match or explicit user selection).
     * The user reviews the proposed changes and confirms or cancels.
     *
     * @param hotelInfo The extracted hotel details.
     * @param destination The destination whose hotel will be updated.
     * @param existingHotel The hotel record that will be updated, or `null` if a new one will be created.
     */
    data class HotelConfirm(
        val hotelInfo: HotelInfo,
        val destination: Destination,
        val existingHotel: Hotel?,
    ) : ShareUiState()

    /** The document has been attached and all extracted info applied successfully. */
    data object Done : ShareUiState()

    /** A non-recoverable error occurred during processing (copy or save failed). */
    data object Error : ShareUiState()
}
