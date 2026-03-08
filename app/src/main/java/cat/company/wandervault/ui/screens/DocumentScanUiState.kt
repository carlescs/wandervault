package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.DocumentExtractionResult

/**
 * State of an in-progress or completed document scan for inline field auto-fill.
 *
 * Used by [TransportDetailViewModel] (boarding-pass scan) and
 * [LocationDetailViewModel] (hotel-reservation scan).
 */
sealed class DocumentScanUiState {
    /** ML Kit analysis is running. */
    data object Loading : DocumentScanUiState()

    /**
     * The on-device AI model is being downloaded before analysis can begin.
     *
     * @param bytesDownloaded Total bytes of model data downloaded so far.
     */
    data class Downloading(val bytesDownloaded: Long) : DocumentScanUiState()

    /**
     * Analysis completed successfully.
     *
     * @param extractionResult The full extraction result from ML Kit.
     */
    data class Result(val extractionResult: DocumentExtractionResult) : DocumentScanUiState()

    /**
     * AI analysis is permanently unavailable on this device, or the document content
     * could not be read. Retrying will not help.
     */
    data object Unavailable : DocumentScanUiState()

    /**
     * Analysis failed with a transient error. The user may try again.
     *
     * @param message Optional error detail that can be shown to help diagnose the problem.
     */
    data class Error(val message: String? = null) : DocumentScanUiState()
}

/**
 * Formats [bytes] as a human-readable size string (B / KB / MB).
 * Used by scan-dialog composables in this package.
 */
internal fun formatScanBytes(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_048_576 -> "${bytes / 1_024} KB"
    else -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
}
