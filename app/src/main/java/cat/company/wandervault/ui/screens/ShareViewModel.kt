package cat.company.wandervault.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.usecase.CopyDocumentToInternalStorageUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetHotelForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetRootDocumentsUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.GetTripsUseCase
import cat.company.wandervault.domain.usecase.SaveDocumentUseCase
import cat.company.wandervault.domain.usecase.SaveHotelUseCase
import cat.company.wandervault.domain.usecase.SaveTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.SummarizeDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateTransportLegUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

/**
 * ViewModel that drives the share-to-WanderVault flow.
 *
 * Receives the shared document details ([sourceUri], [mimeType], [documentName]) at construction
 * time (via Koin parameters) and proceeds through the following states:
 * 1. [ShareUiState.Loading] → [ShareUiState.TripSelection]: trips are loaded, user picks one.
 * 2. [ShareUiState.Processing]: document is copied and analysed by ML Kit.
 * 3. (optional) [ShareUiState.FlightLegSelection] or [ShareUiState.HotelDestinationSelection]:
 *    no confident itinerary match; user selects the target element.
 * 4. [ShareUiState.Done]: all data applied; the caller should dismiss the share UI.
 * 5. [ShareUiState.Error]: a non-recoverable error occurred.
 *
 * @param sourceUri URI of the shared document (as-received from the intent).
 * @param mimeType MIME type of the shared document.
 * @param documentName Display name for the shared document.
 */
class ShareViewModel(
    private val sourceUri: String,
    private val mimeType: String,
    private val documentName: String,
    private val getTrips: GetTripsUseCase,
    private val copyDocumentToInternalStorage: CopyDocumentToInternalStorageUseCase,
    private val saveDocument: SaveDocumentUseCase,
    private val updateDocument: UpdateDocumentUseCase,
    private val getRootDocuments: GetRootDocumentsUseCase,
    private val summarizeDocument: SummarizeDocumentUseCase,
    private val getTrip: GetTripUseCase,
    private val saveTripDescription: SaveTripDescriptionUseCase,
    private val getDestinationsForTrip: GetDestinationsForTripUseCase,
    private val getHotelForDestination: GetHotelForDestinationUseCase,
    private val saveHotel: SaveHotelUseCase,
    private val updateTransportLeg: UpdateTransportLegUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Loading)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    /** Pending flight info kept alive across the [ShareUiState.FlightLegSelection] dialog. */
    private var pendingFlightInfo: FlightInfo? = null

    /** Pending hotel info kept alive across the [ShareUiState.HotelDestinationSelection] dialog. */
    private var pendingHotelInfo: HotelInfo? = null

    /** The ID of the trip the user selected; set when entering [ShareUiState.Processing]. */
    private var selectedTripId: Int = NO_TRIP_SELECTED

    init {
        viewModelScope.launch {
            getTrips()
                // Stop collecting once the user has moved past trip selection.
                .takeWhile {
                    val state = _uiState.value
                    state is ShareUiState.Loading || state is ShareUiState.TripSelection
                }
                .collect { trips ->
                    _uiState.value = ShareUiState.TripSelection(
                        trips = trips,
                        sourceUri = sourceUri,
                        mimeType = mimeType,
                        documentName = documentName,
                    )
                }
        }
    }

    /**
     * Called when the user confirms a trip. Begins copying the document, persisting it, and
     * running ML Kit extraction asynchronously.
     */
    fun onTripSelected(tripId: Int) {
        selectedTripId = tripId
        _uiState.value = ShareUiState.Processing

        viewModelScope.launch {
            // Copy and save the document – these failures are fatal.
            val internalUri = copyDocumentToInternalStorage(sourceUri) ?: run {
                Log.e(TAG, "Failed to copy document to internal storage")
                _uiState.value = ShareUiState.Error
                return@launch
            }

            try {
                saveDocument(
                    TripDocument(
                        tripId = tripId,
                        folderId = null,
                        name = documentName.trim(),
                        uri = internalUri,
                        mimeType = mimeType,
                    ),
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save document", e)
                _uiState.value = ShareUiState.Error
                return@launch
            }

            // ML extraction + itinerary updates – failure here is non-fatal; the document has
            // already been saved successfully so we still transition to Done.
            try {
                val tripYear = getTrip(tripId).first()?.startDate?.year
                val result = summarizeDocument(internalUri, mimeType, tripYear)
                if (result != null) {
                    // Persist the extracted summary on the saved document record.
                    val savedDoc = getRootDocuments(tripId).first().find { it.uri == internalUri }
                    if (savedDoc != null) {
                        updateDocument(savedDoc.copy(summary = result.summary))
                    }

                    when {
                        result.flightInfo != null ->
                            handleFlightInfo(result.flightInfo)

                        result.hotelInfo != null ->
                            handleHotelInfo(result.hotelInfo)

                        else -> {
                            val relevantInfo = result.relevantTripInfo
                            if (relevantInfo != null) {
                                val trip = getTrip(tripId).first()
                                if (trip != null && trip.aiDescription == null) {
                                    saveTripDescription(trip, relevantInfo)
                                }
                            }
                            _uiState.value = ShareUiState.Done
                        }
                    }
                } else {
                    _uiState.value = ShareUiState.Done
                }
            } catch (e: Exception) {
                Log.w(TAG, "ML extraction failed; document was saved successfully", e)
                _uiState.value = ShareUiState.Done
            }
        }
    }

    /**
     * Called when the user picks a specific [TransportLeg] from the flight disambiguation dialog.
     * Transitions to [ShareUiState.FlightConfirm] so the user can review the changes before
     * they are saved.
     */
    fun onFlightLegSelected(leg: TransportLeg) {
        val flightInfo = pendingFlightInfo ?: run {
            _uiState.value = ShareUiState.Done
            return
        }
        pendingFlightInfo = null
        _uiState.value = ShareUiState.FlightConfirm(
            flightInfo = flightInfo,
            matchedLeg = leg,
        )
    }

    /**
     * Called when the user picks a [Destination] from the hotel disambiguation dialog.
     * Loads the existing hotel for the destination and transitions to [ShareUiState.HotelConfirm]
     * so the user can review the changes before they are saved.
     */
    fun onHotelDestinationSelected(destination: Destination) {
        val hotelInfo = pendingHotelInfo ?: run {
            _uiState.value = ShareUiState.Done
            return
        }
        pendingHotelInfo = null
        viewModelScope.launch {
            try {
                val existingHotel = getHotelForDestination(destination.id).first()
                // Guard against the user skipping/dismissing while the DB query was in-flight.
                // compareAndSet ensures we only advance to HotelConfirm if the state has not
                // been modified since we captured it (e.g. by onDisambiguationSkipped()).
                val selectionState = _uiState.value
                if (selectionState is ShareUiState.HotelDestinationSelection) {
                    _uiState.compareAndSet(
                        expect = selectionState,
                        update = ShareUiState.HotelConfirm(
                            hotelInfo = hotelInfo,
                            destination = destination,
                            existingHotel = existingHotel,
                        ),
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load hotel for selected destination", e)
                val selectionState = _uiState.value
                if (selectionState is ShareUiState.HotelDestinationSelection) {
                    _uiState.compareAndSet(expect = selectionState, update = ShareUiState.Done)
                }
            }
        }
    }

    /** Skips the current disambiguation step and moves directly to [ShareUiState.Done]. */
    fun onDisambiguationSkipped() {
        // Only act when in a selection state. Programmatic dismissal of the selection dialog
        // (e.g. when the state advances to FlightConfirm / HotelConfirm) fires onDismissRequest
        // asynchronously on the previous dialog — those spurious calls must be ignored.
        val current = _uiState.value
        if (current is ShareUiState.FlightLegSelection || current is ShareUiState.HotelDestinationSelection) {
            pendingFlightInfo = null
            pendingHotelInfo = null
            _uiState.value = ShareUiState.Done
        }
    }

    /**
     * Called when the user confirms the flight info change from the [ShareUiState.FlightConfirm]
     * dialog. Applies the flight info and leg stored in the confirm state and transitions to
     * [ShareUiState.Done].
     */
    fun onFlightConfirmed() {
        val state = _uiState.value as? ShareUiState.FlightConfirm ?: run {
            _uiState.value = ShareUiState.Done
            return
        }
        viewModelScope.launch {
            try {
                applyFlightInfoToLeg(state.flightInfo, state.matchedLeg)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply flight info to confirmed leg", e)
            } finally {
                _uiState.value = ShareUiState.Done
            }
        }
    }

    /**
     * Called when the user confirms the hotel info change from the [ShareUiState.HotelConfirm]
     * dialog. Applies the hotel info and destination stored in the confirm state and transitions
     * to [ShareUiState.Done].
     */
    fun onHotelConfirmed() {
        val state = _uiState.value as? ShareUiState.HotelConfirm ?: run {
            _uiState.value = ShareUiState.Done
            return
        }
        viewModelScope.launch {
            try {
                applyHotelInfoToDestination(state.hotelInfo, state.destination)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply hotel info to confirmed destination", e)
            } finally {
                _uiState.value = ShareUiState.Done
            }
        }
    }

    /**
     * Called when the user cancels from the [ShareUiState.FlightConfirm] or
     * [ShareUiState.HotelConfirm] dialog. Skips the update and transitions to [ShareUiState.Done].
     */
    fun onConfirmCancelled() {
        _uiState.value = ShareUiState.Done
    }

    // -----------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------

    /**
     * Checks [flightInfo] against all FLIGHT-type legs in the selected trip.
     *
     * A *confident* match requires the flight number **or** booking reference to match exactly
     * (case-insensitive).  When there is no confident match but there are candidates, the state
     * transitions to [ShareUiState.FlightLegSelection] with candidates sorted by relevance
     * (closest partial match first).  When there are no flight legs at all the info is skipped.
     */
    private suspend fun handleFlightInfo(flightInfo: FlightInfo) {
        val allFlightLegs = getDestinationsForTrip(selectedTripId).first()
            .flatMap { dest -> dest.transport?.legs.orEmpty() }
            .filter { it.type == TransportType.FLIGHT }

        if (allFlightLegs.isEmpty()) {
            Log.d(TAG, "No flight legs in trip $selectedTripId; skipping extracted flight info")
            _uiState.value = ShareUiState.Done
            return
        }

        val confidentMatch = allFlightLegs.firstOrNull { leg ->
            flightInfo.flightNumber != null &&
                leg.flightNumber?.equals(flightInfo.flightNumber, ignoreCase = true) == true
        } ?: allFlightLegs.firstOrNull { leg ->
            flightInfo.bookingReference != null &&
                leg.reservationConfirmationNumber?.equals(
                    flightInfo.bookingReference,
                    ignoreCase = true,
                ) == true
        }

        if (confidentMatch != null) {
            _uiState.value = ShareUiState.FlightConfirm(
                flightInfo = flightInfo,
                matchedLeg = confidentMatch,
            )
        } else {
            // Sort candidates by relevance: loose flight-number/booking-ref matches go first,
            // then airline prefix matches, then the rest in their original (stable) order.
            val sortedCandidates = allFlightLegs
                .withIndex()
                .sortedWith(
                    compareByDescending<IndexedValue<TransportLeg>> { indexed ->
                        !flightInfo.flightNumber.isNullOrBlank() &&
                            indexed.value.flightNumber?.contains(
                                flightInfo.flightNumber,
                                ignoreCase = true,
                            ) == true
                    }.thenByDescending { indexed ->
                        !flightInfo.bookingReference.isNullOrBlank() &&
                            indexed.value.reservationConfirmationNumber?.contains(
                                flightInfo.bookingReference,
                                ignoreCase = true,
                            ) == true
                    }.thenByDescending { indexed ->
                        !flightInfo.airline.isNullOrBlank() &&
                            indexed.value.company?.contains(
                                flightInfo.airline,
                                ignoreCase = true,
                            ) == true
                    }.thenBy { indexed ->
                        // Preserve original order among legs with equal relevance.
                        indexed.index
                    },
                )
                .map { it.value }
            pendingFlightInfo = flightInfo
            _uiState.value = ShareUiState.FlightLegSelection(
                flightInfo = flightInfo,
                candidates = sortedCandidates,
            )
        }
    }

    /**
     * Checks [hotelInfo] against all destinations in the selected trip.
     *
     * A *confident* match requires that the destination already has a hotel record and that
     * the booking reference **or** hotel name matches that existing hotel exactly
     * (case-insensitive). The check-in date is intentionally excluded from the confident-match
     * criteria: an arrival date coincidence is not reliable enough to auto-apply changes because
     * two different hotels can share the same check-in date, which would silently match the wrong
     * destination and suppress the selection dialog. When there is no confident match but there
     * are destinations, the state transitions to [ShareUiState.HotelDestinationSelection] with
     * the candidates filtered to destinations whose stay period overlaps with the extracted hotel
     * dates (or all destinations when no dates are available). When there are no destinations at
     * all the info is silently skipped.
     */
    private suspend fun handleHotelInfo(hotelInfo: HotelInfo) {
        val destinations = getDestinationsForTrip(selectedTripId).first()

        if (destinations.isEmpty()) {
            Log.d(TAG, "No destinations in trip $selectedTripId; skipping extracted hotel info")
            _uiState.value = ShareUiState.Done
            return
        }

        val destinationHotels = destinations.map { dest ->
            dest to getHotelForDestination(dest.id).first()
        }

        val confidentMatch = destinationHotels.firstOrNull { (_, hotel) ->
            hotel != null && hotelInfo.bookingReference != null &&
                hotel.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)
        } ?: destinationHotels.firstOrNull { (_, hotel) ->
            hotel != null && hotelInfo.name != null &&
                hotel.name.equals(hotelInfo.name, ignoreCase = true)
        }

        if (confidentMatch != null) {
            _uiState.value = ShareUiState.HotelConfirm(
                hotelInfo = hotelInfo,
                destination = confidentMatch.first,
                existingHotel = confidentMatch.second,
            )
        } else {
            // Filter candidates to those whose stay period overlaps the hotel dates when
            // dates are available, so the user is presented with the most relevant options.
            val candidates = if (hotelInfo.checkInDate != null || hotelInfo.checkOutDate != null) {
                destinations.filter { dest -> dest.overlapsHotelDates(hotelInfo) }
                    .takeUnless { it.isEmpty() } ?: destinations
            } else {
                destinations
            }
            pendingHotelInfo = hotelInfo
            _uiState.value = ShareUiState.HotelDestinationSelection(
                hotelInfo = hotelInfo,
                candidates = candidates,
            )
        }
    }

    private suspend fun applyFlightInfoToLeg(flightInfo: FlightInfo, leg: TransportLeg) {
        val updatedLeg = leg.copy(
            company = leg.company?.ifBlank { null } ?: flightInfo.airline,
            flightNumber = leg.flightNumber?.ifBlank { null } ?: flightInfo.flightNumber,
            reservationConfirmationNumber = leg.reservationConfirmationNumber
                ?.ifBlank { null } ?: flightInfo.bookingReference,
            stopName = leg.stopName?.ifBlank { null } ?: flightInfo.arrivalPlace,
        )
        if (updatedLeg != leg) {
            updateTransportLeg(updatedLeg)
        }
    }

    private suspend fun applyHotelInfoToDestination(hotelInfo: HotelInfo, destination: Destination) {
        val existingHotel = getHotelForDestination(destination.id).first()
        if (existingHotel != null) {
            val updatedHotel = existingHotel.copy(
                name = existingHotel.name.ifBlank { null } ?: hotelInfo.name.orEmpty(),
                address = existingHotel.address.ifBlank { null } ?: hotelInfo.address.orEmpty(),
                reservationNumber = existingHotel.reservationNumber.ifBlank { null }
                    ?: hotelInfo.bookingReference.orEmpty(),
            )
            if (updatedHotel != existingHotel) {
                saveHotel(updatedHotel)
            }
        } else {
            if (hotelInfo.name.isNullOrBlank() && hotelInfo.address.isNullOrBlank() &&
                hotelInfo.bookingReference.isNullOrBlank()
            ) {
                return
            }
            saveHotel(
                Hotel(
                    destinationId = destination.id,
                    name = hotelInfo.name ?: "",
                    address = hotelInfo.address ?: "",
                    reservationNumber = hotelInfo.bookingReference ?: "",
                ),
            )
        }
    }

    companion object {
        private const val TAG = "ShareViewModel"
        private const val NO_TRIP_SELECTED = -1
    }
}
