package cat.company.wandervault.ui.screens

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.usecase.CopyDocumentToInternalStorageUseCase
import cat.company.wandervault.domain.usecase.DeleteDocumentUseCase
import cat.company.wandervault.domain.usecase.DeleteFolderUseCase
import cat.company.wandervault.domain.usecase.GetAllFoldersForTripUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetDocumentsInFolderUseCase
import cat.company.wandervault.domain.usecase.GetHotelForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetRootDocumentsUseCase
import cat.company.wandervault.domain.usecase.GetRootFoldersUseCase
import cat.company.wandervault.domain.usecase.GetSubFoldersUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.SaveDocumentUseCase
import cat.company.wandervault.domain.usecase.SaveFolderUseCase
import cat.company.wandervault.domain.usecase.SaveHotelUseCase
import cat.company.wandervault.domain.usecase.SaveTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.SummarizeDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateFolderUseCase
import cat.company.wandervault.domain.usecase.UpdateTransportLegUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the Trip Documents tab.
 *
 * Manages folder navigation (root → sub-folder → sub-sub-folder, etc.) and
 * exposes the current level's folders and documents as a [StateFlow].
 *
 * Write operations (create/rename/delete) catch constraint and validation failures and surface
 * them as a transient [TripDocumentsUiState.Success.writeError]. Call [clearError] to dismiss.
 *
 * @param tripId The ID of the trip whose documents are shown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripDocumentsViewModel(
    private val tripId: Int,
    private val getRootFolders: GetRootFoldersUseCase,
    private val getSubFolders: GetSubFoldersUseCase,
    private val getDocumentsInFolder: GetDocumentsInFolderUseCase,
    private val getRootDocuments: GetRootDocumentsUseCase,
    private val saveFolder: SaveFolderUseCase,
    private val updateFolder: UpdateFolderUseCase,
    private val deleteFolder: DeleteFolderUseCase,
    private val saveDocument: SaveDocumentUseCase,
    private val updateDocument: UpdateDocumentUseCase,
    private val deleteDocument: DeleteDocumentUseCase,
    private val copyDocumentToInternalStorage: CopyDocumentToInternalStorageUseCase,
    private val summarizeDocument: SummarizeDocumentUseCase,
    private val getAllFoldersForTrip: GetAllFoldersForTripUseCase,
    private val getTrip: GetTripUseCase,
    private val saveTripDescription: SaveTripDescriptionUseCase,
    private val getDestinationsForTrip: GetDestinationsForTripUseCase,
    private val getHotelForDestination: GetHotelForDestinationUseCase,
    private val saveHotel: SaveHotelUseCase,
    private val updateTransportLeg: UpdateTransportLegUseCase,
) : ViewModel() {

    /** Stack of folders the user has navigated into; empty = at root. */
    private val _folderStack = MutableStateFlow<List<TripDocumentFolder>>(emptyList())

    private val _uiState = MutableStateFlow<TripDocumentsUiState>(TripDocumentsUiState.Loading)
    val uiState: StateFlow<TripDocumentsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _folderStack.flatMapLatest { stack ->
                val currentFolder = stack.lastOrNull()
                val foldersFlow = if (currentFolder == null) {
                    getRootFolders(tripId)
                } else {
                    getSubFolders(currentFolder.id)
                }
                val documentsFlow = if (currentFolder == null) {
                    getRootDocuments(tripId)
                } else {
                    getDocumentsInFolder(currentFolder.id)
                }
                val allFoldersFlow = getAllFoldersForTrip(tripId)
                combine(foldersFlow, documentsFlow, allFoldersFlow) { folders, documents, allFolders ->
                    TripDocumentsUiState.Success(
                        folders = folders,
                        documents = documents,
                        currentFolder = currentFolder,
                        folderStack = stack,
                        allFolders = allFolders,
                        // writeError is not preserved across data refreshes: a successful write
                        // triggers a new DB emission which clears any prior error naturally.
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /** Navigates into [folder], pushing it onto the folder stack. */
    fun openFolder(folder: TripDocumentFolder) {
        _folderStack.value = _folderStack.value + folder
    }

    /**
     * Navigates up one level in the folder hierarchy.
     * No-op when already at the root level.
     */
    fun navigateUp() {
        val stack = _folderStack.value
        if (stack.isNotEmpty()) {
            _folderStack.value = stack.dropLast(1)
        }
    }

    /** Returns `true` if the user can navigate up (i.e. not at the root level). */
    fun canNavigateUp(): Boolean = _folderStack.value.isNotEmpty()

    /** Dismisses the current error message (if any). */
    fun clearError() {
        val current = _uiState.value as? TripDocumentsUiState.Success ?: return
        _uiState.value = current.copy(writeError = null)
    }

    /** Creates a new folder with [name] at the current navigation level. */
    fun createFolder(name: String) {
        val parentFolderId = _folderStack.value.lastOrNull()?.id
        launchWrite {
            saveFolder(
                TripDocumentFolder(
                    tripId = tripId,
                    name = name.trim(),
                    parentFolderId = parentFolderId,
                ),
            )
        }
    }

    /** Renames [folder] to [newName]. */
    fun renameFolder(folder: TripDocumentFolder, newName: String) {
        launchWrite { updateFolder(folder.copy(name = newName.trim())) }
    }

    /** Permanently removes [folder] and all its contents. */
    fun removeFolder(folder: TripDocumentFolder) {
        launchWrite { deleteFolder(folder) }
    }

    /**
     * Copies the file at [sourceUri] to internal storage and attaches the resulting document to
     * the current folder, or to the trip root when no folder is open.
     *
     * [name] will be used as the document display name. [mimeType] should be the MIME type of
     * the file (e.g. "application/pdf"). After saving the document, ML Kit is used to extract a
     * summary and any relevant trip information. The summary is stored in the document record, and
     * any trip information found is saved as the trip's AI description if not already set.
     *
     * If the file copy fails the error is surfaced via [DocumentsWriteError.Generic].
     */
    fun addDocument(name: String, sourceUri: String, mimeType: String) {
        val currentFolder = _folderStack.value.lastOrNull()
        launchWrite {
            val internalUri = copyDocumentToInternalStorage(sourceUri)
                ?: throw IllegalStateException("Failed to copy document to internal storage")
            saveDocument(
                TripDocument(
                    tripId = tripId,
                    folderId = currentFolder?.id,
                    name = name.trim(),
                    uri = internalUri,
                    mimeType = mimeType,
                ),
            )
            extractAndApplyDocumentInfo(internalUri, mimeType, name.trim(), currentFolder?.id)
        }
    }

    /**
     * Runs ML Kit document extraction on the file at [internalUri], then:
     * 1. Updates the document record with the extracted summary.
     * 2. If the document contains flight info, updates the matching [TransportLeg] in the trip.
     * 3. If the document contains hotel info, updates or creates the [Hotel] for the matching
     *    destination.
     * 4. Otherwise, updates the trip's AI description with any extracted general trip-relevant
     *    info if the trip currently has no description.
     *
     * [folderId] is the ID of the folder the document was saved into (null = trip root). It is
     * captured at upload time so that navigation changes during the (potentially long) ML Kit
     * extraction do not affect which document list is queried.
     *
     * Extraction failures are logged and silently ignored so that the document upload always
     * succeeds even when on-device AI is unavailable.
     */
    private suspend fun extractAndApplyDocumentInfo(
        internalUri: String,
        mimeType: String,
        documentName: String,
        folderId: Int?,
    ) {
        try {
            val result = summarizeDocument(internalUri, mimeType) ?: return
            // Update the saved document with the extracted summary.
            // Lookup by URI is safe because internalUri is a UUID-based filename.
            val currentDocuments = if (folderId == null) {
                getRootDocuments(tripId).first()
            } else {
                getDocumentsInFolder(folderId).first()
            }
            val savedDoc = currentDocuments.find { it.uri == internalUri }
            if (savedDoc != null) {
                updateDocument(savedDoc.copy(summary = result.summary))
            }
            when {
                result.flightInfo != null -> applyFlightInfo(result.flightInfo, documentName)
                result.hotelInfo != null -> applyHotelInfo(result.hotelInfo, documentName)
                else -> {
                    // Fallback: update the trip's AI description with general trip-relevant info.
                    val relevantInfo = result.relevantTripInfo ?: return
                    val trip = getTrip(tripId).first()
                    if (trip == null) {
                        Log.w(TAG, "Trip $tripId not found; skipping description update for $documentName")
                        return
                    }
                    if (trip.aiDescription == null) {
                        saveTripDescription(trip, relevantInfo)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Document info extraction failed for $documentName", e)
        }
    }

    /**
     * Applies extracted [flightInfo] to the most relevant [TransportLeg] with
     * [TransportType.FLIGHT] in this trip.
     *
     * Matching priority:
     * 1. Leg whose [TransportLeg.flightNumber] matches the extracted flight number.
     * 2. Leg whose [TransportLeg.reservationConfirmationNumber] matches the extracted booking ref.
     * 3. First FLIGHT leg with a blank [TransportLeg.flightNumber] (clearly incomplete data).
     * 4. First FLIGHT leg found (last resort).
     *
     * Only blank fields on the matched leg are updated; existing non-blank values are preserved.
     */
    private suspend fun applyFlightInfo(
        flightInfo: FlightInfo,
        documentName: String,
    ) {
        val allFlightLegs = getDestinationsForTrip(tripId).first()
            .flatMap { dest -> dest.transport?.legs.orEmpty() }
            .filter { it.type == TransportType.FLIGHT }
        if (allFlightLegs.isEmpty()) {
            Log.d(TAG, "No flight legs in trip $tripId; skipping flight info from $documentName")
            return
        }
        val matchedLeg = allFlightLegs.firstOrNull { leg ->
            flightInfo.flightNumber != null &&
                leg.flightNumber.equals(flightInfo.flightNumber, ignoreCase = true)
        } ?: allFlightLegs.firstOrNull { leg ->
            flightInfo.bookingReference != null &&
                leg.reservationConfirmationNumber.equals(
                    flightInfo.bookingReference,
                    ignoreCase = true,
                )
        } ?: allFlightLegs.firstOrNull { leg ->
            leg.flightNumber.isNullOrBlank()
        } ?: allFlightLegs.first()

        // Only fill in fields that are currently blank; do not overwrite user-entered data.
        // FlightInfo.arrivalPlace maps to TransportLeg.stopName (the endpoint of the leg).
        // FlightInfo.departurePlace is intentionally not stored: the departure is implicitly the
        // name of the Destination that owns this Transport, which is already in the itinerary.
        val updatedLeg = matchedLeg.copy(
            company = matchedLeg.company?.ifBlank { null } ?: flightInfo.airline,
            flightNumber = matchedLeg.flightNumber?.ifBlank { null } ?: flightInfo.flightNumber,
            reservationConfirmationNumber = matchedLeg.reservationConfirmationNumber
                ?.ifBlank { null } ?: flightInfo.bookingReference,
            stopName = matchedLeg.stopName?.ifBlank { null } ?: flightInfo.arrivalPlace,
        )
        if (updatedLeg != matchedLeg) {
            updateTransportLeg(updatedLeg)
        }
    }

    /**
     * Applies extracted [hotelInfo] to the most relevant [Hotel] in this trip.
     *
     * Matching priority:
     * 1. Existing hotel whose [Hotel.reservationNumber] matches the extracted booking reference.
     * 2. Existing hotel whose [Hotel.name] matches the extracted hotel name.
     * 3. First destination that has no hotel yet (a new [Hotel] is created for it).
     * 4. First destination's hotel (last resort).
     *
     * Only blank fields on the matched hotel are updated; existing non-blank values are preserved.
     */
    private suspend fun applyHotelInfo(
        hotelInfo: HotelInfo,
        documentName: String,
    ) {
        val destinations = getDestinationsForTrip(tripId).first()
        if (destinations.isEmpty()) {
            Log.d(TAG, "No destinations in trip $tripId; skipping hotel info from $documentName")
            return
        }
        // Build a list of (destination, hotel?) pairs for the trip.
        val destinationHotels = destinations.map { dest ->
            dest to getHotelForDestination(dest.id).first()
        }
        val (matchedDest, existingHotel) = destinationHotels.firstOrNull { (_, hotel) ->
            hotel != null && hotelInfo.bookingReference != null &&
                hotel.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)
        } ?: destinationHotels.firstOrNull { (_, hotel) ->
            hotel != null && hotelInfo.name != null &&
                hotel.name.equals(hotelInfo.name, ignoreCase = true)
        } ?: destinationHotels.firstOrNull { (_, hotel) ->
            hotel == null
        } ?: destinationHotels.first()

        if (existingHotel != null) {
            // Only fill in blank fields; preserve existing non-blank values.
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
            // No hotel exists for this destination yet — create one with the extracted data.
            if (hotelInfo.name.isNullOrBlank() && hotelInfo.address.isNullOrBlank() &&
                hotelInfo.bookingReference.isNullOrBlank()
            ) {
                Log.d(TAG, "All hotel fields are blank from $documentName; skipping hotel creation")
                return
            }
            saveHotel(
                Hotel(
                    destinationId = matchedDest.id,
                    name = hotelInfo.name ?: "",
                    address = hotelInfo.address ?: "",
                    reservationNumber = hotelInfo.bookingReference ?: "",
                ),
            )
        }
    }

    /** Renames [document] to [newName]. */
    fun renameDocument(document: TripDocument, newName: String) {
        launchWrite { updateDocument(document.copy(name = newName.trim())) }
    }

    /** Moves [document] to the folder with [targetFolderId], or to root level when `null`. */
    fun moveDocument(document: TripDocument, targetFolderId: Int?) {
        launchWrite { updateDocument(document.copy(folderId = targetFolderId)) }
    }

    /** Permanently removes [document]. */
    fun removeDocument(document: TripDocument) {
        launchWrite { deleteDocument(document) }
    }

    /**
     * Launches a write coroutine that catches:
     * - [IllegalArgumentException] from repository-level root-folder uniqueness checks → [DocumentsWriteError.DuplicateName]
     * - [SQLiteConstraintException] from Room unique-index violations on sub-folders or documents → [DocumentsWriteError.DuplicateName]
     * - All other [Exception] types → [DocumentsWriteError.Generic]
     *
     * Errors are surfaced as a transient [DocumentsWriteError] on the UI state rather than
     * crashing the app or cancelling the ViewModel scope.
     */
    private fun launchWrite(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Write operation rejected (duplicate name): ${e.message}")
                setWriteError(DocumentsWriteError.DuplicateName)
            } catch (e: SQLiteConstraintException) {
                Log.w(TAG, "Write operation rejected (constraint violation): ${e.message}")
                setWriteError(DocumentsWriteError.DuplicateName)
            } catch (e: Exception) {
                Log.e(TAG, "Write operation failed", e)
                setWriteError(DocumentsWriteError.Generic)
            }
        }
    }

    private fun setWriteError(error: DocumentsWriteError) {
        val current = _uiState.value as? TripDocumentsUiState.Success ?: return
        _uiState.value = current.copy(writeError = error)
    }

    companion object {
        private const val TAG = "TripDocumentsViewModel"
    }
}
