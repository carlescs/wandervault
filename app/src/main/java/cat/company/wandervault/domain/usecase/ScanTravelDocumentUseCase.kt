package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.ScannedDocumentInfo
import cat.company.wandervault.domain.parser.TravelDocumentTextParser
import cat.company.wandervault.domain.repository.TextRecognitionRepository

/**
 * Use-case that scans an image of a travel document (boarding pass, hotel reservation, …) and
 * extracts structured fields such as the flight number and booking reference.
 *
 * Text is recognised on-device with ML Kit and parsed by [TravelDocumentTextParser]. Nothing is
 * persisted: callers must show the result to the user for confirmation before saving it.
 */
class ScanTravelDocumentUseCase(private val repository: TextRecognitionRepository) {

    /**
     * @param imageUri URI of the image to scan.
     * @return The extracted [ScannedDocumentInfo], or `null` when no text was recognised.
     * @throws Exception when the image cannot be read or text recognition fails.
     */
    suspend operator fun invoke(imageUri: String): ScannedDocumentInfo? {
        val text = repository.recognizeText(imageUri)?.takeIf { it.isNotBlank() } ?: return null
        return TravelDocumentTextParser.parse(text)
    }
}
