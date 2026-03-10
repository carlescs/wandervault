package cat.company.wandervault.ui.screens

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.usecase.GetAllFoldersForTripUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetDocumentByIdUseCase
import cat.company.wandervault.domain.usecase.GetHotelForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.SaveHotelUseCase
import cat.company.wandervault.domain.usecase.SaveTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.SummarizeDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateTransportLegUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for the Document Info screen.
 *
 * Loads the [DocumentInfoUiState] for a single document identified by [documentId], including
 * the document model, the file size read from internal storage, and the containing folder name.
 * Also exposes document analysis capabilities via [analyzeDocument].
 *
 * @param documentId The ID of the document to display.
 * @param getDocumentById Use-case that streams the document entity.
 * @param getAllFoldersForTrip Use-case that streams all folders in the document's trip.
 * @param summarizeDocument Use-case that runs ML Kit analysis on a document.
 * @param updateDocument Use-case that persists an updated document record.
 * @param getTrip Use-case that retrieves the parent trip.
 * @param saveTripDescription Use-case that saves the trip AI description.
 * @param getDestinationsForTrip Use-case that streams destinations for the trip.
 * @param getHotelForDestination Use-case that streams the hotel for a destination.
 * @param saveHotel Use-case that saves a hotel record.
 * @param updateTransportLeg Use-case that persists an updated transport leg.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DocumentInfoViewModel(
    private val documentId: Int,
    private val getDocumentById: GetDocumentByIdUseCase,
    private val getAllFoldersForTrip: GetAllFoldersForTripUseCase,
    private val summarizeDocument: SummarizeDocumentUseCase,
    private val updateDocument: UpdateDocumentUseCase,
    private val getTrip: GetTripUseCase,
    private val saveTripDescription: SaveTripDescriptionUseCase,
    private val getDestinationsForTrip: GetDestinationsForTripUseCase,
    private val getHotelForDestination: GetHotelForDestinationUseCase,
    private val saveHotel: SaveHotelUseCase,
    private val updateTransportLeg: UpdateTransportLegUseCase,
) : ViewModel() {

    /** Current document analysis state; updated independently from the DB-driven document flow. */
    private val _analyzeState = MutableStateFlow<AnalyzeDocumentUiState?>(null)

    /** The coroutine running the current document analysis, kept so it can be cancelled. */
    private var analyzeJob: Job? = null

    /** Pending flight info kept alive across the [AnalyzeDocumentUiState.FlightLegSelection] dialog. */
    private var pendingFlightInfo: FlightInfo? = null

    /** Pending hotel info kept alive across the [AnalyzeDocumentUiState.HotelDestinationSelection] dialog. */
    private var pendingHotelInfo: HotelInfo? = null

    val uiState: StateFlow<DocumentInfoUiState> = combine(
        getDocumentById(documentId)
            .flatMapLatest { document ->
                if (document == null) {
                    flowOf(DocumentInfoUiState.NotFound)
                } else {
                    flow<DocumentInfoUiState> {
                        val fileSize = resolveFileSize(document.uri)
                        getAllFoldersForTrip(document.tripId).collect { folders ->
                            val folderName = document.folderId?.let { fid ->
                                folders.find { it.id == fid }?.name
                            }
                            emit(
                                DocumentInfoUiState.Success(
                                    document = document,
                                    fileSizeBytes = fileSize,
                                    folderName = folderName,
                                )
                            )
                        }
                    }
                }
            },
        _analyzeState,
    ) { docState, analyzeState ->
        if (docState is DocumentInfoUiState.Success) {
            docState.copy(analyzeState = analyzeState)
        } else {
            docState
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DocumentInfoUiState.Loading,
        )

    private suspend fun resolveFileSize(uri: String): Long? = withContext(Dispatchers.IO) {
        try {
            val path = Uri.parse(uri).path ?: return@withContext null
            val file = File(path)
            if (file.exists()) file.length() else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Runs ML Kit analysis on the current document and updates the UI with the results.
     *
     * Sets [AnalyzeDocumentUiState.Loading] immediately while the document is being read.
     * If the Gemini Nano model needs to be downloaded first, transitions to
     * [AnalyzeDocumentUiState.Downloading] with live byte-count updates.
     * On success sets [AnalyzeDocumentUiState.Result] with the extraction result.
     * On permanent unavailability sets [AnalyzeDocumentUiState.Unavailable].
     * On transient failure sets [AnalyzeDocumentUiState.Error].
     *
     * Also updates the document's stored summary if a new one is extracted (best-effort:
     * a DB failure persisting the summary does not affect the displayed result).
     *
     * No-op when the document is not yet loaded.
     */
    fun analyzeDocument() {
        val document = (uiState.value as? DocumentInfoUiState.Success)?.document ?: return
        val tripId = document.tripId
        analyzeJob?.cancel()
        _analyzeState.value = AnalyzeDocumentUiState.Loading
        analyzeJob = viewModelScope.launch {
            val result = try {
                val tripYear = getTrip(tripId).first()?.startDate?.year
                val analysisResult = summarizeDocument(document.uri, document.mimeType, tripYear) { bytesDownloaded ->
                    _analyzeState.value = AnalyzeDocumentUiState.Downloading(bytesDownloaded)
                }
                if (analysisResult == null) {
                    Log.w(TAG, "summarizeDocument returned null for ${document.name}; AI unavailable or unsupported type")
                    _analyzeState.value = AnalyzeDocumentUiState.Unavailable
                    return@launch
                }
                // Set the result immediately so the UI shows the analysis without waiting for DB.
                _analyzeState.value = AnalyzeDocumentUiState.Result(document, analysisResult)
                analysisResult
            } catch (e: Exception) {
                Log.w(TAG, "Document analysis failed for ${document.name}", e)
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
                return@launch
            }

            // Persist the refreshed summary on the document record (best-effort).
            try {
                updateDocument(document.copy(summary = result.summary))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist refreshed summary for ${document.name}", e)
            }
        }
    }

    /**
     * Processes the trip changes proposed in the current [AnalyzeDocumentUiState.Result].
     *
     * For flight info, checks for a confident match first. If found, transitions to
     * [AnalyzeDocumentUiState.FlightConfirm] so the user can review the matched leg before
     * confirming. If no confident match, transitions to [AnalyzeDocumentUiState.FlightLegSelection]
     * (candidates sorted by relevance) so the user can pick the right leg.
     *
     * For hotel info, a confident match (by booking reference or hotel name) is checked first.
     * If found, transitions to [AnalyzeDocumentUiState.HotelConfirm] so the user can review the
     * matched destination before confirming. If no confident match, transitions to
     * [AnalyzeDocumentUiState.HotelDestinationSelection] so the user can pick the right destination.
     *
     * For general trip info, transitions to [AnalyzeDocumentUiState.TripInfoConfirm] so the user
     * can review the extracted text before it is saved as the trip description. Dismisses
     * immediately when no relevant info was extracted.
     *
     * No-op when the analyze state is not [AnalyzeDocumentUiState.Result].
     */
    fun applyAnalysisChanges() {
        val analyzeResult = _analyzeState.value as? AnalyzeDocumentUiState.Result ?: return
        val result = analyzeResult.extractionResult
        val documentName = analyzeResult.document.name
        val tripId = analyzeResult.document.tripId
        when {
            result.flightInfo != null -> {
                viewModelScope.launch {
                    try {
                        applyOrDisambiguateFlightInfo(result.flightInfo, documentName, tripId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to apply flight info for trip $tripId", e)
                        _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
                    }
                }
            }
            result.hotelInfo != null -> {
                viewModelScope.launch {
                    try {
                        applyOrDisambiguateHotelInfo(result.hotelInfo, documentName, tripId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to apply hotel info for trip $tripId", e)
                        _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
                    }
                }
            }
            else -> {
                val relevantInfo = result.relevantTripInfo
                if (relevantInfo == null) {
                    dismissAnalyze()
                } else {
                    _analyzeState.value = AnalyzeDocumentUiState.TripInfoConfirm(relevantInfo)
                }
            }
        }
    }

    /**
     * Called when the user picks a [TransportLeg] from the flight disambiguation dialog.
     * Transitions to [AnalyzeDocumentUiState.FlightConfirm] so the user can review the changes
     * before they are saved.
     */
    fun onFlightLegSelected(leg: TransportLeg) {
        val flightInfo = pendingFlightInfo ?: run {
            Log.w(TAG, "onFlightLegSelected called with no pending flight info; dismissing")
            dismissAnalyze()
            return
        }
        pendingFlightInfo = null
        _analyzeState.value = AnalyzeDocumentUiState.FlightConfirm(
            flightInfo = flightInfo,
            matchedLeg = leg,
        )
    }

    /**
     * Called when the user confirms the flight info change from the [AnalyzeDocumentUiState.FlightConfirm]
     * dialog. Applies the flight info and leg stored in the confirm state and dismisses.
     */
    fun onFlightConfirmed() {
        val state = _analyzeState.value as? AnalyzeDocumentUiState.FlightConfirm ?: run {
            Log.w(TAG, "onFlightConfirmed called when not in FlightConfirm state; dismissing")
            dismissAnalyze()
            return
        }
        viewModelScope.launch {
            try {
                applyFlightInfoToLeg(state.flightInfo, state.matchedLeg)
                dismissAnalyze()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply flight info to confirmed leg", e)
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
            }
        }
    }

    /**
     * Called when the user picks a [Destination] from the hotel disambiguation dialog.
     * Loads the existing hotel for the destination and transitions to
     * [AnalyzeDocumentUiState.HotelConfirm] so the user can review the changes before they
     * are saved.
     */
    fun onHotelDestinationSelected(destination: Destination) {
        val hotelInfo = pendingHotelInfo ?: run {
            Log.w(TAG, "onHotelDestinationSelected called with no pending hotel info; dismissing")
            dismissAnalyze()
            return
        }
        pendingHotelInfo = null
        viewModelScope.launch {
            try {
                val existingHotel = getHotelForDestination(destination.id).first()
                val selectionState = _analyzeState.value
                if (selectionState is AnalyzeDocumentUiState.HotelDestinationSelection) {
                    _analyzeState.compareAndSet(
                        expect = selectionState,
                        update = AnalyzeDocumentUiState.HotelConfirm(
                            hotelInfo = hotelInfo,
                            destination = destination,
                            existingHotel = existingHotel,
                        ),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load hotel for selected destination", e)
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
            }
        }
    }

    /**
     * Called when the user confirms the hotel info change from the [AnalyzeDocumentUiState.HotelConfirm]
     * dialog. Applies the hotel info and destination stored in the confirm state and dismisses.
     */
    fun onHotelConfirmed() {
        val state = _analyzeState.value as? AnalyzeDocumentUiState.HotelConfirm ?: run {
            Log.w(TAG, "onHotelConfirmed called when not in HotelConfirm state; dismissing")
            dismissAnalyze()
            return
        }
        viewModelScope.launch {
            try {
                applyHotelInfoToDestination(state.hotelInfo, state.destination)
                dismissAnalyze()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply hotel info to confirmed destination", e)
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
            }
        }
    }

    /**
     * Called when the user confirms the general trip-info change from the
     * [AnalyzeDocumentUiState.TripInfoConfirm] dialog. Saves the extracted trip description
     * (only when the trip does not already have one) and dismisses.
     */
    fun onTripInfoConfirmed() {
        val state = _analyzeState.value as? AnalyzeDocumentUiState.TripInfoConfirm ?: run {
            Log.w(TAG, "onTripInfoConfirmed called when not in TripInfoConfirm state; dismissing")
            dismissAnalyze()
            return
        }
        val tripId = (uiState.value as? DocumentInfoUiState.Success)?.document?.tripId ?: run {
            dismissAnalyze()
            return
        }
        viewModelScope.launch {
            try {
                val trip = getTrip(tripId).first()
                if (trip == null) {
                    Log.w(TAG, "Trip $tripId not found after user confirmation; skipping description update")
                } else if (trip.aiDescription == null) {
                    saveTripDescription(trip, state.relevantTripInfo)
                }
                dismissAnalyze()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save trip description for trip $tripId", e)
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
            }
        }
    }

    /**
     * Cancels any in-flight analysis and dismisses the document analysis dialog.
     */
    fun dismissAnalyze() {
        analyzeJob?.cancel()
        analyzeJob = null
        pendingFlightInfo = null
        pendingHotelInfo = null
        _analyzeState.value = null
    }

    /**
     * Checks [flightInfo] against all FLIGHT-type legs in the given trip.
     *
     * A *confident* match requires the flight number **or** booking reference to match exactly
     * (case-insensitive). When there is a confident match the state transitions to
     * [AnalyzeDocumentUiState.FlightConfirm] so the user can review and confirm the change.
     * When there is no confident match but there are flight legs, the state transitions to
     * [AnalyzeDocumentUiState.FlightLegSelection] (sorted by relevance) so the user can pick.
     * When there are no flight legs at all the info is silently skipped.
     */
    private suspend fun applyOrDisambiguateFlightInfo(
        flightInfo: FlightInfo,
        documentName: String,
        tripId: Int,
    ) {
        val allFlightLegs = getDestinationsForTrip(tripId).first()
            .flatMap { dest -> dest.transport?.legs.orEmpty() }
            .filter { it.type == TransportType.FLIGHT }

        if (allFlightLegs.isEmpty()) {
            Log.d(TAG, "No flight legs in trip $tripId; skipping flight info from $documentName")
            dismissAnalyze()
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
            _analyzeState.value = AnalyzeDocumentUiState.FlightConfirm(
                flightInfo = flightInfo,
                matchedLeg = confidentMatch,
            )
        } else {
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
                        indexed.index
                    },
                )
                .map { it.value }
            pendingFlightInfo = flightInfo
            _analyzeState.value = AnalyzeDocumentUiState.FlightLegSelection(
                flightInfo = flightInfo,
                candidates = sortedCandidates,
            )
        }
    }

    /**
     * Applies [flightInfo] to [leg], filling only blank fields and preserving existing non-blank
     * values. Persists the change via [updateTransportLeg] only when the leg actually changes.
     */
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

    /**
     * Checks [hotelInfo] against all destinations in the given trip.
     *
     * A *confident* match requires that there is already an existing [Hotel] record for a
     * destination in this trip, and that the booking reference **or** hotel name matches that
     * record exactly (case-insensitive). When there is such a confident match the state transitions
     * to [AnalyzeDocumentUiState.HotelConfirm] so the user can review and confirm the change.
     * When there is no existing hotel record or no confident match, the state transitions to
     * [AnalyzeDocumentUiState.HotelDestinationSelection] with candidates filtered to destinations
     * whose stay period overlaps the hotel dates (or all destinations when no dates are available),
     * so the user can pick the destination.
     */
    private suspend fun applyOrDisambiguateHotelInfo(
        hotelInfo: HotelInfo,
        documentName: String,
        tripId: Int,
    ) {
        val destinations = getDestinationsForTrip(tripId).first()
        if (destinations.isEmpty()) {
            Log.d(TAG, "No destinations in trip $tripId; skipping hotel info from $documentName")
            dismissAnalyze()
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
            val (matchedDest, matchedHotel) = confidentMatch
            _analyzeState.value = AnalyzeDocumentUiState.HotelConfirm(
                hotelInfo = hotelInfo,
                destination = matchedDest,
                existingHotel = matchedHotel,
            )
        } else {
            val candidates = if (hotelInfo.checkInDate != null || hotelInfo.checkOutDate != null) {
                destinations.filter { dest ->
                    dest.overlapsHotelDates(hotelInfo)
                }.takeUnless { it.isEmpty() } ?: destinations
            } else {
                destinations
            }
            pendingHotelInfo = hotelInfo
            _analyzeState.value = AnalyzeDocumentUiState.HotelDestinationSelection(
                hotelInfo = hotelInfo,
                candidates = candidates,
            )
        }
    }

    /**
     * Applies [hotelInfo] to the given [destination], updating its existing hotel record
     * (preserving non-blank fields) or creating a new one if none exists yet.
     */
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
        private const val TAG = "DocumentInfoViewModel"
    }
}
