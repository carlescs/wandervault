package cat.company.wandervault.domain.model

/**
 * Result of extracting information from a travel document using ML Kit.
 *
 * @param summary A brief AI-generated summary of what the document contains.
 * @param relevantTripInfo Trip-relevant information extracted from the document (e.g. dates,
 *   destinations, booking references), or `null` if nothing trip-relevant was found.
 */
data class DocumentExtractionResult(
    val summary: String,
    val relevantTripInfo: String? = null,
)
