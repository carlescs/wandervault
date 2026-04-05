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
import cat.company.wandervault.domain.usecase.GetHotelsForDestinationsUseCase
import cat.company.wandervault.domain.usecase.GetOrCreateTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.SaveHotelUseCase
import cat.company.wandervault.domain.usecase.SaveTransportLegUseCase
import cat.company.wandervault.domain.usecase.SaveTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.SummarizeDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateDestinationUseCase
import cat.company.wandervault.domain.usecase.UpdateTransportLegUseCase
import kotlinx.coroutines.CancellationException
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
 * @param getHotelForDestination Use-case that streams the hotel for a single destination.
 * @param getHotelsForDestinations Use-case that fetches hotels for multiple destinations in one query.
 * @param saveHotel Use-case that saves a hotel record.
 * @param updateTransportLeg Use-case that persists an updated transport leg.
 * @param updateDestination Use-case that persists an updated destination record (used to keep
 *   [Destination.departureDateTime] and [Destination.arrivalDateTime] in sync with the first/last
 *   leg's datetime when flight info is applied from a document).
 * @param getOrCreateTransport Use-case that returns the transport ID for a destination, creating one if needed.
 * @param saveTransportLeg Use-case that persists a new transport leg.
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
    private val getHotelsForDestinations: GetHotelsForDestinationsUseCase,
    private val saveHotel: SaveHotelUseCase,
    private val updateTransportLeg: UpdateTransportLegUseCase,
    private val updateDestination: UpdateDestinationUseCase,
    private val getOrCreateTransport: GetOrCreateTransportForDestinationUseCase,
    private val saveTransportLeg: SaveTransportLegUseCase,
) : ViewModel() {

    /** Current document analysis state; updated independently from the DB-driven document flow. */
    private val _analyzeState = MutableStateFlow<AnalyzeDocumentUiState?>(null)

    /**
     * Tracks on-device AI availability.
     * Initialised to `false` (fail-closed) and updated once in [init] after checking the model.
     */
    private val _isAiAvailable = MutableStateFlow(false)

    /** The coroutine running the current document analysis, kept so it can be cancelled. */
    private var analyzeJob: Job? = null

    /** Pending flight info kept alive across the [AnalyzeDocumentUiState.FlightLegSelection] dialog. */
    private var pendingFlightInfo: FlightInfo? = null

    /** Pending hotel info kept alive across the [AnalyzeDocumentUiState.HotelDestinationSelection] dialog. */
    private var pendingHotelInfo: HotelInfo? = null

    /**
     * Queue of flight infos extracted from the current document that have not yet been processed.
     * Populated by [analyzeDocument] and consumed one item at a time by
     * [processNextExtractedInfo].
     */
    private val remainingFlightInfos = ArrayDeque<FlightInfo>()

    /**
     * Queue of hotel infos extracted from the current document that have not yet been processed.
     * Populated by [analyzeDocument] and consumed one item at a time by
     * [processNextExtractedInfo].
     */
    private val remainingHotelInfos = ArrayDeque<HotelInfo>()

    /** Trip ID for the document currently being analysed; stored for use in [processNextExtractedInfo]. */
    private var pendingAnalysisTripId: Int = NO_TRIP_PENDING

    /** Document name for the document currently being analysed; stored for logging in [processNextExtractedInfo]. */
    private var pendingAnalysisDocumentName: String = ""

    /** General trip info to show after all flight/hotel items are processed, if any. */
    private var pendingRelevantTripInfo: String? = null

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
        _isAiAvailable,
    ) { docState, analyzeState, aiAvailable ->
        if (docState is DocumentInfoUiState.Success) {
            docState.copy(analyzeState = analyzeState, isAiAvailable = aiAvailable)
        } else {
            docState
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DocumentInfoUiState.Loading,
        )

    init {
        // Check AI availability upfront so the Analyze button is hidden proactively
        // on devices that do not support Gemini Nano.
        viewModelScope.launch {
            val available = try {
                summarizeDocument.isAvailable()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "AI availability check failed; assuming unavailable", e)
                false
            }
            _isAiAvailable.value = available
        }
    }

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
     * On success persists the summary to the DB (the bottom sheet already shows it) and
     * directly begins processing proposed trip changes without showing an intermediate result
     * dialog.
     * On permanent unavailability sets [AnalyzeDocumentUiState.Unavailable].
     * On transient failure sets [AnalyzeDocumentUiState.Error].
     *
     * Can be called even when the document already has an AI description, allowing the user
     * to re-analyze the document and overwrite the existing summary.
     *
     * No-op when the document is not yet loaded.
     */
    fun analyzeDocument() {
        startAnalysis(updateSummary = true)
    }

    /**
     * Runs ML Kit analysis on the current document to extract trip-relevant information
     * (flights, hotels, etc.) and applies it to the trip, **without** overwriting the
     * existing AI description (summary).
     *
     * Sets [AnalyzeDocumentUiState.Loading] immediately while the document is being read.
     * If the Gemini Nano model needs to be downloaded first, transitions to
     * [AnalyzeDocumentUiState.Downloading] with live byte-count updates.
     * On success, directly begins processing proposed trip changes.
     * On permanent unavailability sets [AnalyzeDocumentUiState.Unavailable].
     * On transient failure sets [AnalyzeDocumentUiState.Error].
     *
     * No-op when the document is not yet loaded.
     */
    fun analyzeDocumentForTripUpdates() {
        startAnalysis(updateSummary = false)
    }

    /**
     * Shared implementation for [analyzeDocument] and [analyzeDocumentForTripUpdates].
     *
     * @param updateSummary When `true`, the extracted summary is persisted to the document record.
     *   When `false`, the existing summary is preserved and only trip elements are processed.
     */
    private fun startAnalysis(updateSummary: Boolean) {
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
                analysisResult
            } catch (e: Exception) {
                Log.w(TAG, "Document analysis failed for ${document.name}", e)
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
                return@launch
            }

            if (updateSummary) {
                // Reload the latest document before updating to avoid clobbering concurrent changes
                // (e.g. rename/move) that may have happened while analysis was running (best-effort).
                try {
                    val latestDocument = getDocumentById(documentId).first()
                    if (latestDocument != null) {
                        updateDocument(latestDocument.copy(summary = result.summary))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist refreshed summary for ${document.name}", e)
                    _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
                    return@launch
                }
            }

            // Directly process any proposed trip changes; the summary is already visible in the
            // bottom sheet so there is no need for an intermediate result dialog.
            pendingAnalysisTripId = tripId
            pendingAnalysisDocumentName = document.name
            pendingRelevantTripInfo = result.relevantTripInfo
            remainingFlightInfos.clear()
            remainingFlightInfos.addAll(result.flightInfoList)
            remainingHotelInfos.clear()
            remainingHotelInfos.addAll(result.hotelInfoList)
            processNextExtractedInfo()
        }
    }

    /**
     * Deletes the AI description (summary) of the current document.
     *
     * No-op when the document is not yet loaded.
     */
    fun deleteAiDescription() {
        viewModelScope.launch {
            try {
                val latestDocument = getDocumentById(documentId).first() ?: return@launch
                updateDocument(latestDocument.copy(summary = null))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete AI description for document $documentId", e)
            }
        }
    }

    /**
     * Processes the next pending extracted item (flight or hotel) from the queues.
     *
     * Dequeues the first available [FlightInfo] and runs flight matching/disambiguation,
     * or the first available [HotelInfo] and runs hotel matching/disambiguation.
     * When both queues are empty, shows the general trip-info confirmation (if any) or
     * dismisses the dialog.
     */
    private fun processNextExtractedInfo() {
        val tripId = pendingAnalysisTripId
        val documentName = pendingAnalysisDocumentName
        when {
            remainingFlightInfos.isNotEmpty() -> {
                val flightInfo = remainingFlightInfos.removeFirst()
                viewModelScope.launch {
                    try {
                        applyOrDisambiguateFlightInfo(flightInfo, documentName, tripId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to apply flight info for trip $tripId", e)
                        _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
                    }
                }
            }
            remainingHotelInfos.isNotEmpty() -> {
                val hotelInfo = remainingHotelInfos.removeFirst()
                viewModelScope.launch {
                    try {
                        applyOrDisambiguateHotelInfo(hotelInfo, documentName, tripId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to apply hotel info for trip $tripId", e)
                        _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
                    }
                }
            }
            else -> {
                val relevantInfo = pendingRelevantTripInfo
                pendingRelevantTripInfo = null
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
     * dialog. Applies the flight info and leg stored in the confirm state, then advances to the
     * next pending item via [processNextExtractedInfo].
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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply flight info to confirmed leg", e)
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
                return@launch
            }
            processNextExtractedInfo()
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
     * dialog. Applies the hotel info and destination stored in the confirm state, then advances to
     * the next pending item via [processNextExtractedInfo].
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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply hotel info to confirmed destination", e)
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
                return@launch
            }
            processNextExtractedInfo()
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
                    saveTripDescription(trip, state.relevantTripInfo, sourceDocumentId = documentId)
                }
                dismissAnalyze()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save trip description for trip $tripId", e)
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
            }
        }
    }

    /**
     * Cancels any in-flight analysis and dismisses the document analysis dialog entirely,
     * clearing all pending items.
     */
    fun dismissAnalyze() {
        analyzeJob?.cancel()
        analyzeJob = null
        pendingFlightInfo = null
        pendingHotelInfo = null
        remainingFlightInfos.clear()
        remainingHotelInfos.clear()
        pendingRelevantTripInfo = null
        _analyzeState.value = null
    }

    /**
     * Skips the current per-item step (flight or hotel disambiguation/confirm) and advances to
     * the next pending item without closing the dialog. Called when the user taps "Skip" or
     * "Cancel" during a per-item state.
     *
     * If there are no more pending items, dismisses the dialog entirely via [dismissAnalyze].
     */
    fun skipCurrentAnalysisItem() {
        pendingFlightInfo = null
        pendingHotelInfo = null
        processNextExtractedInfo()
    }

    /**
     * Checks [flightInfo] against all FLIGHT-type legs in the given trip.
     *
     * A *confident* match requires the flight number **or** booking reference to match exactly
     * (case-insensitive). When there is a confident match the state transitions to
     * [AnalyzeDocumentUiState.FlightConfirm] so the user can review and confirm the change.
     * When there is no confident match but there are flight legs, the state transitions to
     * [AnalyzeDocumentUiState.FlightLegSelection] (sorted by relevance) so the user can pick.
     * When there are no flight legs but there are eligible non-terminal destinations in the trip,
     * the state transitions to [AnalyzeDocumentUiState.FlightTransportSelection] so the user can
     * pick a destination to add a new leg to (creating the transport record if it does not yet
     * exist). If the trip has fewer than two destinations, so there is no eligible non-terminal
     * destination, the info is silently skipped.
     */
    private suspend fun applyOrDisambiguateFlightInfo(
        flightInfo: FlightInfo,
        documentName: String,
        tripId: Int,
    ) {
        val allDestinations = getDestinationsForTrip(tripId).first()
        val allFlightLegs = allDestinations
            .flatMap { dest -> dest.transport?.legs.orEmpty() }
            .filter { it.type == TransportType.FLIGHT }

        if (allFlightLegs.isEmpty()) {
            // The last destination (highest position) has no onward transport, so exclude it.
            val maxPosition = allDestinations.maxOfOrNull { it.position }
            val nonTerminalDestinations = allDestinations.filter { it.position != maxPosition }
            if (nonTerminalDestinations.isEmpty()) {
                Log.d(TAG, "No non-terminal destinations in trip $tripId; skipping flight info from $documentName")
                processNextExtractedInfo()
                return
            }
            pendingFlightInfo = flightInfo
            _analyzeState.value = AnalyzeDocumentUiState.FlightTransportSelection(
                flightInfo = flightInfo,
                candidates = nonTerminalDestinations,
            )
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
     * Called when the user picks a [Destination] from the transport selection dialog.
     * Transitions to [AnalyzeDocumentUiState.FlightAddLegConfirm] so the user can review
     * the proposed new leg before it is saved.
     */
    fun onFlightTransportSelected(destination: Destination) {
        val flightInfo = pendingFlightInfo ?: run {
            Log.w(TAG, "onFlightTransportSelected called with no pending flight info; dismissing")
            dismissAnalyze()
            return
        }
        pendingFlightInfo = null
        _analyzeState.value = AnalyzeDocumentUiState.FlightAddLegConfirm(
            flightInfo = flightInfo,
            destination = destination,
        )
    }

    /**
     * Called when the user confirms adding a new flight leg from the
     * [AnalyzeDocumentUiState.FlightAddLegConfirm] dialog. Creates and persists the new leg,
     * then advances to the next pending item via [processNextExtractedInfo].
     */
    fun onFlightAddLegConfirmed() {
        val state = _analyzeState.value as? AnalyzeDocumentUiState.FlightAddLegConfirm ?: run {
            Log.w(TAG, "onFlightAddLegConfirmed called when not in FlightAddLegConfirm state; dismissing")
            dismissAnalyze()
            return
        }
        viewModelScope.launch {
            try {
                addFlightLegToTransport(state.flightInfo, state.destination)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add flight leg to transport", e)
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: e.toString())
                return@launch
            }
            processNextExtractedInfo()
        }
    }

    /**
     * Creates a new FLIGHT-type [TransportLeg] populated from [flightInfo] and appends it to
     * the transport belonging to [destination], creating the transport record if needed.
     *
     * When the new leg is the first leg (position 0) and a departure datetime can be derived from
     * the extracted document, [Destination.departureDateTime] is also updated to keep the
     * itinerary timeline in sync (matching the convention in [TransportDetailViewModel]).
     */
    private suspend fun addFlightLegToTransport(flightInfo: FlightInfo, destination: Destination) {
        val transportId = getOrCreateTransport(destination.id)
        // Re-fetch the destination to get the current leg count so that the new leg's position
        // is accurate even if the destination snapshot stored in the UI state is stale.
        val currentDestination = getDestinationsForTrip(destination.tripId).first()
            .firstOrNull { it.id == destination.id }
        val position = currentDestination?.transport?.legs?.size ?: 0
        val departureDateTime = flightInfo.toZonedDeparture()
        saveTransportLeg(
            TransportLeg(
                id = 0,
                transportId = transportId,
                type = TransportType.FLIGHT,
                position = position,
                company = flightInfo.airline,
                flightNumber = flightInfo.flightNumber,
                reservationConfirmationNumber = flightInfo.bookingReference,
                stopName = flightInfo.arrivalPlace,
                departureDateTime = departureDateTime,
            ),
        )
        // Sync the destination departure time when this is the first leg and a time was extracted.
        if (position == 0 && departureDateTime != null) {
            val destToUpdate = currentDestination ?: destination
            updateDestination(destToUpdate.copy(departureDateTime = departureDateTime))
        }
    }

    /**
     * Applies [flightInfo] to [leg] via [TransportLeg.applyFlightInfo], then persists the
     * updated leg when it actually changed.
     *
     * Also syncs destination-level datetimes following the same convention as
     * [TransportDetailViewModel]:
     * - If the updated leg is the **first leg** (position 0) of its transport and its
     *   `departureDateTime` changed, the owning destination's [Destination.departureDateTime] is
     *   updated to match.
     * - If the updated leg is the **last leg** of its transport and its `arrivalDateTime`
     *   changed, the *next* destination's [Destination.arrivalDateTime] is updated to match.
     */
    private suspend fun applyFlightInfoToLeg(flightInfo: FlightInfo, leg: TransportLeg) {
        val updatedLeg = leg.applyFlightInfo(flightInfo).copy(sourceDocumentId = documentId)
        if (updatedLeg == leg) return
        updateTransportLeg(updatedLeg)

        // Re-fetch destinations to find owning and next destination for syncing.
        val tripId = pendingAnalysisTripId.takeIf { it != NO_TRIP_PENDING } ?: return
        val allDestinations = getDestinationsForTrip(tripId).first()
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
            processNextExtractedInfo()
            return
        }

        val destinationHotels = run {
            val hotelsByDestId = getHotelsForDestinations(destinations.map { it.id })
            destinations.map { dest -> dest to hotelsByDestId[dest.id] }
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
                sourceDocumentId = documentId,
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
                    sourceDocumentId = documentId,
                ),
            )
        }
    }

    companion object {
        private const val TAG = "DocumentInfoViewModel"

        /** Sentinel value for [pendingAnalysisTripId] when no analysis is in progress. */
        private const val NO_TRIP_PENDING = -1
    }
}
