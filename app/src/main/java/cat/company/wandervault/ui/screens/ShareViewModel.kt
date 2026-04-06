package cat.company.wandervault.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.model.ActivityInfo
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
import cat.company.wandervault.domain.usecase.SaveActivityUseCase
import cat.company.wandervault.domain.usecase.SaveDocumentUseCase
import cat.company.wandervault.domain.usecase.SaveHotelUseCase
import cat.company.wandervault.domain.usecase.SaveTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.SummarizeDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateDestinationUseCase
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
 * 3. (optional) [ShareUiState.FlightLegSelection], [ShareUiState.HotelDestinationSelection], or
 *    [ShareUiState.ActivityDestinationSelection]: no confident itinerary match; user selects the
 *    target element.
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
    private val saveActivity: SaveActivityUseCase,
    private val updateTransportLeg: UpdateTransportLegUseCase,
    private val updateDestination: UpdateDestinationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Loading)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    /** Pending flight info kept alive across the [ShareUiState.FlightLegSelection] dialog. */
    private var pendingFlightInfo: FlightInfo? = null

    /** Pending hotel info kept alive across the [ShareUiState.HotelDestinationSelection] dialog. */
    private var pendingHotelInfo: HotelInfo? = null

    /** Pending activity info kept alive across the [ShareUiState.ActivityDestinationSelection] dialog. */
    private var pendingActivityInfo: ActivityInfo? = null

    /**
     * General trip-relevant information extracted from the document.
     * Applied automatically after all flights, hotels, and activities are processed, if the trip
     * does not already have an AI description.
     */
    private var pendingRelevantTripInfo: String? = null

    /** The ID of the trip the user selected; set when entering [ShareUiState.Processing]. */
    private var selectedTripId: Int = NO_TRIP_SELECTED

    /**
     * The database ID of the document that was saved for this share session.
     * Set after the document is persisted so it can be linked to extracted itinerary items.
     */
    private var savedDocumentId: Int = 0

    /**
     * Queue of flight infos extracted from the shared document that have not yet been processed.
     * Populated when ML extraction completes and consumed one item at a time.
     */
    private val remainingFlightInfos = ArrayDeque<FlightInfo>()

    /**
     * Queue of hotel infos extracted from the shared document that have not yet been processed.
     * Populated when ML extraction completes and consumed one item at a time.
     */
    private val remainingHotelInfos = ArrayDeque<HotelInfo>()

    /**
     * Queue of activity infos extracted from the shared document that have not yet been processed.
     * Populated when ML extraction completes and consumed one item at a time.
     */
    private val remainingActivityInfos = ArrayDeque<ActivityInfo>()

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
                        savedDocumentId = savedDoc.id
                        updateDocument(savedDoc.copy(summary = result.summary))
                    }

                    remainingFlightInfos.clear()
                    remainingFlightInfos.addAll(result.flightInfoList)
                    remainingHotelInfos.clear()
                    remainingHotelInfos.addAll(result.hotelInfoList)
                    remainingActivityInfos.clear()
                    remainingActivityInfos.addAll(result.activityInfoList)
                    pendingRelevantTripInfo = result.relevantTripInfo

                    handleNextExtractedInfo()
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
            viewModelScope.launch { handleNextExtractedInfo() }
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
            viewModelScope.launch { handleNextExtractedInfo() }
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
                    handleNextExtractedInfo()
                }
            }
        }
    }

    /**
     * Skips the current disambiguation step and advances to the next pending item. If no more
     * items remain, transitions to [ShareUiState.Done].
     *
     * Only acts when in a selection state. Programmatic dismissal of the selection dialog
     * (e.g. when the state advances to FlightConfirm / HotelConfirm) fires onDismissRequest
     * asynchronously on the previous dialog — those spurious calls must be ignored.
     */
    fun onDisambiguationSkipped() {
        val current = _uiState.value
        if (current is ShareUiState.FlightLegSelection ||
            current is ShareUiState.HotelDestinationSelection ||
            current is ShareUiState.ActivityDestinationSelection
        ) {
            pendingFlightInfo = null
            pendingHotelInfo = null
            pendingActivityInfo = null
            viewModelScope.launch { handleNextExtractedInfo() }
        }
    }

    /**
     * Called when the user confirms the flight info change from the [ShareUiState.FlightConfirm]
     * dialog. Applies the flight info and leg stored in the confirm state, then advances to the
     * next pending item.
     */
    fun onFlightConfirmed() {
        val state = _uiState.value as? ShareUiState.FlightConfirm ?: run {
            viewModelScope.launch { handleNextExtractedInfo() }
            return
        }
        viewModelScope.launch {
            try {
                applyFlightInfoToLeg(state.flightInfo, state.matchedLeg)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply flight info to confirmed leg", e)
            }
            handleNextExtractedInfo()
        }
    }

    /**
     * Called when the user confirms the hotel info change from the [ShareUiState.HotelConfirm]
     * dialog. Applies the hotel info and destination stored in the confirm state, then advances
     * to the next pending item.
     */
    fun onHotelConfirmed() {
        val state = _uiState.value as? ShareUiState.HotelConfirm ?: run {
            viewModelScope.launch { handleNextExtractedInfo() }
            return
        }
        viewModelScope.launch {
            try {
                applyHotelInfoToDestination(state.hotelInfo, state.destination)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply hotel info to confirmed destination", e)
            }
            handleNextExtractedInfo()
        }
    }

    /**
     * Called when the user cancels from the [ShareUiState.FlightConfirm],
     * [ShareUiState.HotelConfirm], or [ShareUiState.ActivityConfirm] dialog. Skips the update
     * and advances to the next pending item.
     */
    fun onConfirmCancelled() {
        viewModelScope.launch { handleNextExtractedInfo() }
    }

    /**
     * Called when the user picks a [Destination] from the activity destination selection dialog.
     * Transitions to [ShareUiState.ActivityConfirm] so the user can review the new activity
     * before it is saved.
     */
    fun onActivityDestinationSelected(destination: Destination) {
        val activityInfo = pendingActivityInfo ?: run {
            viewModelScope.launch { handleNextExtractedInfo() }
            return
        }
        pendingActivityInfo = null
        _uiState.value = ShareUiState.ActivityConfirm(
            activityInfo = activityInfo,
            destination = destination,
        )
    }

    /**
     * Called when the user confirms the activity from the [ShareUiState.ActivityConfirm] dialog.
     * Creates and persists the new activity, then advances to the next pending item via
     * [handleNextExtractedInfo].
     */
    fun onActivityConfirmed() {
        val state = _uiState.value as? ShareUiState.ActivityConfirm ?: run {
            viewModelScope.launch { handleNextExtractedInfo() }
            return
        }
        viewModelScope.launch {
            try {
                val activityInfo = state.activityInfo
                val dateTime = activityInfo.toZonedDateTime()
                val title = activityInfo.title.orEmpty()
                val description = activityInfo.description.orEmpty()
                val confirmationNumber = activityInfo.confirmationNumber.orEmpty()
                if (title.isBlank() && description.isBlank() && confirmationNumber.isBlank() && dateTime == null) {
                    Log.w(TAG, "Skipping activity save: all extracted fields are empty")
                } else {
                    val docId = savedDocumentId.takeIf { it > 0 }
                    saveActivity(
                        Activity(
                            destinationId = state.destination.id,
                            title = title,
                            description = description,
                            dateTime = dateTime,
                            confirmationNumber = confirmationNumber,
                            sourceDocumentId = docId,
                        ),
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save activity for destination ${state.destination.id}", e)
            }
            handleNextExtractedInfo()
        }
    }

    // -----------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------

    /**
     * Processes the next pending item from the queues. Dequeues the first available
     * [FlightInfo] and calls [handleFlightInfo], or the first available [HotelInfo] and calls
     * [handleHotelInfo], or the first available [ActivityInfo] and calls [handleActivityInfo].
     * When all queues are exhausted, auto-applies the pending trip description (if any) and
     * transitions to [ShareUiState.Done].
     */
    private suspend fun handleNextExtractedInfo() {
        when {
            remainingFlightInfos.isNotEmpty() -> handleFlightInfo(remainingFlightInfos.removeFirst())
            remainingHotelInfos.isNotEmpty() -> handleHotelInfo(remainingHotelInfos.removeFirst())
            remainingActivityInfos.isNotEmpty() -> handleActivityInfo(remainingActivityInfos.removeFirst())
            else -> {
                // All items processed; auto-apply the trip description when not already set.
                val relevantInfo = pendingRelevantTripInfo
                pendingRelevantTripInfo = null
                if (relevantInfo != null) {
                    try {
                        val trip = getTrip(selectedTripId).first()
                        if (trip != null && trip.aiDescription == null) {
                            saveTripDescription(
                                trip,
                                relevantInfo,
                                sourceDocumentId = savedDocumentId.takeIf { it > 0 },
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to save trip description; continuing to Done", e)
                    }
                }
                _uiState.value = ShareUiState.Done
            }
        }
    }

    /**
     * Checks [flightInfo] against all FLIGHT-type legs in the selected trip.
     *
     * Legs already sourced from this document (i.e. [TransportLeg.sourceDocumentId] matches
     * [savedDocumentId]) are excluded from matching so that multiple flights in a single document
     * that share a booking reference do not all match the same leg.
     *
     * A *confident* match requires the flight number **or** booking reference to match exactly
     * (case-insensitive) among the available (non-excluded) legs.  When there is no confident
     * match but there are available candidates, the state transitions to
     * [ShareUiState.FlightLegSelection] with candidates sorted by relevance (closest partial
     * match first).  When there are no available legs the info is skipped.
     */
    private suspend fun handleFlightInfo(flightInfo: FlightInfo) {
        val allFlightLegs = getDestinationsForTrip(selectedTripId).first()
            .flatMap { dest -> dest.transport?.legs.orEmpty() }
            .filter { it.type == TransportType.FLIGHT }

        // Exclude legs already sourced from this document in the current session so that
        // multiple flights sharing a booking reference don't all re-match the same leg.
        val docId = savedDocumentId.takeIf { it > 0 }
        val availableFlightLegs = if (docId != null) {
            allFlightLegs.filter { it.sourceDocumentId != docId }
        } else {
            allFlightLegs
        }

        if (availableFlightLegs.isEmpty()) {
            Log.d(TAG, "No available flight legs in trip $selectedTripId; skipping extracted flight info")
            handleNextExtractedInfo()
            return
        }

        val confidentMatch = availableFlightLegs.firstOrNull { leg ->
            flightInfo.flightNumber != null &&
                leg.flightNumber?.equals(flightInfo.flightNumber, ignoreCase = true) == true
        } ?: availableFlightLegs.firstOrNull { leg ->
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
            // then airline substring matches, then the rest in their original (stable) order.
            val sortedCandidates = availableFlightLegs
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
     * Hotels already sourced from this document (i.e. [Hotel.sourceDocumentId] matches
     * [savedDocumentId]) are excluded from confident matching so that multiple hotels in a
     * single document that share a name or booking reference do not all match the same destination.
     *
     * A *confident* match requires that the destination already has a hotel record (not yet
     * linked to this document) and that the booking reference **or** hotel name matches that
     * existing hotel exactly (case-insensitive). The check-in date is intentionally excluded
     * from the confident-match criteria: an arrival date coincidence is not reliable enough to
     * auto-apply changes because two different hotels can share the same check-in date, which
     * would silently match the wrong destination and suppress the selection dialog. When there
     * is no confident match but there are destinations, the state transitions to
     * [ShareUiState.HotelDestinationSelection] with the candidates filtered to destinations
     * whose stay period overlaps with the extracted hotel dates (or all destinations when no
     * dates are available). When there are no destinations at all the info is silently skipped.
     */
    private suspend fun handleHotelInfo(hotelInfo: HotelInfo) {
        val destinations = getDestinationsForTrip(selectedTripId).first()

        if (destinations.isEmpty()) {
            Log.d(TAG, "No destinations in trip $selectedTripId; skipping extracted hotel info")
            handleNextExtractedInfo()
            return
        }

        val destinationHotels = destinations.map { dest ->
            dest to getHotelForDestination(dest.id).first()
        }

        // Exclude hotels already sourced from this document in the current session so that
        // multiple hotels sharing a name or booking reference don't all re-match the same destination.
        val docId = savedDocumentId.takeIf { it > 0 }
        val confidentMatch = destinationHotels.firstOrNull { (_, hotel) ->
            hotel != null && (docId == null || hotel.sourceDocumentId != docId) &&
                hotelInfo.bookingReference != null &&
                hotel.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)
        } ?: destinationHotels.firstOrNull { (_, hotel) ->
            hotel != null && (docId == null || hotel.sourceDocumentId != docId) &&
                hotelInfo.name != null &&
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

    /**
     * Checks [activityInfo] against destinations in the selected trip.
     *
     * When the activity has a date, only destinations whose stay period contains that date are
     * offered as candidates (falls back to all destinations when none match). When no date is
     * available all destinations are listed. If the trip has no destinations the activity is
     * silently skipped.
     */
    private suspend fun handleActivityInfo(activityInfo: ActivityInfo) {
        val destinations = getDestinationsForTrip(selectedTripId).first()
        if (destinations.isEmpty()) {
            Log.d(TAG, "No destinations in trip $selectedTripId; skipping extracted activity info")
            handleNextExtractedInfo()
            return
        }
        val candidates = if (activityInfo.date != null) {
            destinations.filter { dest -> dest.containsActivityDate(activityInfo) }
                .takeUnless { it.isEmpty() } ?: destinations
        } else {
            destinations
        }
        pendingActivityInfo = activityInfo
        _uiState.value = ShareUiState.ActivityDestinationSelection(
            activityInfo = activityInfo,
            candidates = candidates,
        )
    }

    private suspend fun applyFlightInfoToLeg(flightInfo: FlightInfo, leg: TransportLeg) {
        val applied = leg.applyFlightInfo(flightInfo)
        val docId = savedDocumentId.takeIf { it > 0 }
        val updatedLeg = if (docId != null) applied.copy(sourceDocumentId = docId) else applied
        if (updatedLeg == leg) return
        updateTransportLeg(updatedLeg)

        // Sync destination-level datetimes to keep the itinerary timeline consistent
        // (same convention as TransportDetailViewModel).
        if (selectedTripId == NO_TRIP_SELECTED) return
        val allDestinations = getDestinationsForTrip(selectedTripId).first()
            .sortedBy { it.position }
        val owningDestination = allDestinations.firstOrNull { dest ->
            dest.transport?.id == leg.transportId
        } ?: return
        val legsInTransport = owningDestination.transport?.legs.orEmpty()
        val legIndex = legsInTransport.indexOfFirst { it.id == leg.id }
        if (legIndex < 0) return

        if (legIndex == 0 && updatedLeg.departureDateTime != leg.departureDateTime) {
            updateDestination(owningDestination.copy(departureDateTime = updatedLeg.departureDateTime))
        }
        if (legIndex == legsInTransport.lastIndex && updatedLeg.arrivalDateTime != leg.arrivalDateTime) {
            val owningIndex = allDestinations.indexOf(owningDestination)
            val nextDestination = allDestinations.getOrNull(owningIndex + 1) ?: return
            updateDestination(nextDestination.copy(arrivalDateTime = updatedLeg.arrivalDateTime))
        }
    }

    private suspend fun applyHotelInfoToDestination(hotelInfo: HotelInfo, destination: Destination) {
        val existingHotel = getHotelForDestination(destination.id).first()
        val docId = savedDocumentId.takeIf { it > 0 }
        if (existingHotel != null) {
            val updatedHotel = existingHotel.copy(
                name = existingHotel.name.ifBlank { null } ?: hotelInfo.name.orEmpty(),
                address = existingHotel.address.ifBlank { null } ?: hotelInfo.address.orEmpty(),
                reservationNumber = existingHotel.reservationNumber.ifBlank { null }
                    ?: hotelInfo.bookingReference.orEmpty(),
            ).let { hotel ->
                if (docId != null) hotel.copy(sourceDocumentId = docId) else hotel
            }
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
                    sourceDocumentId = docId,
                ),
            )
        }
    }

    companion object {
        private const val TAG = "ShareViewModel"
        private const val NO_TRIP_SELECTED = -1
    }
}
