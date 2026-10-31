package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.ScannedDocumentInfo

/**
 * State of an on-device document scan (boarding pass, reservation, …) started from the
 * Transport Detail or Location Detail screen.
 */
sealed class DocumentScanUiState {
    /** No scan is in progress. */
    data object Idle : DocumentScanUiState()

    /** Text is being recognised in the selected image. */
    data object Scanning : DocumentScanUiState()

    /**
     * Fields were extracted and are waiting for the user to review and confirm them.
     * Nothing has been saved yet.
     *
     * @param info The extracted fields.
     * @param suggestedLegIndex For transport scans, the index of the leg the fields most likely
     *   belong to, or `null` to suggest creating a new leg. Ignored for hotel scans.
     */
    data class Review(
        val info: ScannedDocumentInfo,
        val suggestedLegIndex: Int? = null,
    ) : DocumentScanUiState()

    /** The image was read but no relevant information could be found in it. */
    data object NothingFound : DocumentScanUiState()

    /** The image could not be read or text recognition failed. */
    data object Error : DocumentScanUiState()
}
