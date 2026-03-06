package cat.company.wandervault.data.mlkit

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripDescriptionRepository
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * ML Kit implementation of [TripDescriptionRepository] that uses the on-device Gemini Nano
 * Prompt API to generate a short, engaging trip summary.
 */
class TripDescriptionRepositoryImpl : TripDescriptionRepository {

    private val client by lazy { Generation.getClient() }

    override suspend fun generateDescription(trip: Trip, destinations: List<Destination>): String? {
        when (client.checkStatus()) {
            FeatureStatus.UNAVAILABLE -> return null
            FeatureStatus.DOWNLOADABLE -> awaitDownload()
            FeatureStatus.AVAILABLE -> Unit
        }
        val request = generateContentRequest(TextPart(buildPrompt(trip, destinations))) {
            maxOutputTokens = MAX_DESCRIPTION_TOKENS
        }
        return client.generateContent(request).candidates.firstOrNull()?.text
    }

    /**
     * Waits for the Gemini Nano model to finish downloading by collecting the download Flow
     * until a terminal status (completed or failed) is emitted.
     */
    private suspend fun awaitDownload() {
        val terminal = client.download()
            .first { it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed }
        if (terminal is DownloadStatus.DownloadFailed) throw terminal.e
    }

    private fun buildPrompt(trip: Trip, destinations: List<Destination>): String {
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

        return buildString {
            appendLine(
                "Write a short, engaging 2–3 sentence description for the following trip. " +
                    "Focus on the places visited and the overall experience.",
            )
            appendLine()
            appendLine("Trip name: ${trip.title}")
            if (trip.startDate != null && trip.endDate != null) {
                appendLine(
                    "Dates: ${trip.startDate.format(dateFormatter)} – ${trip.endDate.format(dateFormatter)}",
                )
            }
            if (destinations.isNotEmpty()) {
                appendLine("Itinerary (${destinations.size} stop(s)):")
                destinations.forEachIndexed { index, dest ->
                    append("  ${index + 1}. ${dest.name}")
                    val arrival = dest.arrivalDateTime?.format(dateTimeFormatter)
                    val departure = dest.departureDateTime?.format(dateTimeFormatter)
                    if (arrival != null) append(" – arrives $arrival")
                    if (departure != null) append(", departs $departure")
                    appendLine()
                    dest.transport?.legs?.forEach { leg ->
                        val typeName = leg.type.name
                            .lowercase()
                            .replaceFirstChar { it.uppercase() }
                        append("     → $typeName")
                        if (!leg.company.isNullOrBlank()) append(" with ${leg.company}")
                        if (!leg.flightNumber.isNullOrBlank()) append(" (${leg.flightNumber})")
                        if (!leg.stopName.isNullOrBlank()) append(" via ${leg.stopName}")
                        appendLine()
                    }
                }
            }
        }
    }

    companion object {
        /** Maximum number of tokens the model may generate for a trip description. */
        private const val MAX_DESCRIPTION_TOKENS = 200
    }
}
