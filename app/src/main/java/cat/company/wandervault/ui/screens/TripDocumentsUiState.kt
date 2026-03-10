package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder

/**
 * Transient error type for failed write operations in the Documents screen.
 * The Screen maps each variant to the appropriate string resource.
 */
enum class DocumentsWriteError {
    /** A folder or document with the requested name already exists in this scope. */
    DuplicateName,
    /** A generic, unclassified write failure. */
    Generic,
}

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
     * Analysis completed successfully.
     *
     * @param document The document that was analyzed.
     * @param extractionResult The full extraction result from ML Kit.
     */
    data class Result(
        val document: TripDocument,
        val extractionResult: DocumentExtractionResult,
    ) : AnalyzeDocumentUiState()

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
 * UI state for the Trip Documents screen.
 *
 * Represents the content of the currently visible folder (either the root level or a sub-folder).
 */
sealed class TripDocumentsUiState {
    /** Data is being loaded. */
    data object Loading : TripDocumentsUiState()

    /**
     * Data loaded successfully.
     *
     * @param folders Sub-folders of the current level (root folders or sub-folders of [currentFolder]).
     * @param documents Documents in the current folder (including root-level documents when [currentFolder] is `null`).
     * @param currentFolder The folder currently being viewed, or `null` when at the root level.
     * @param folderStack Navigation path of folders from the root to the current folder (last element = current folder).
     * @param allFolders All folders in the trip regardless of depth, used for the move-document picker.
     * @param writeError A one-off error from a failed write operation (create/rename/delete),
     *   or `null` when there is no pending error. Call [TripDocumentsViewModel.clearError] to dismiss.
     * @param selectedDocumentIds IDs of documents currently selected in multi-select mode.
     *   Empty when no selection is active. Call [TripDocumentsViewModel.clearSelection] to exit selection mode.
     */
    data class Success(
        val folders: List<TripDocumentFolder> = emptyList(),
        val documents: List<TripDocument> = emptyList(),
        val currentFolder: TripDocumentFolder? = null,
        val folderStack: List<TripDocumentFolder> = emptyList(),
        val allFolders: List<TripDocumentFolder> = emptyList(),
        val writeError: DocumentsWriteError? = null,
        val selectedDocumentIds: Set<Int> = emptySet(),
    ) : TripDocumentsUiState()
}
