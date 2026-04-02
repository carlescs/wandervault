package cat.company.wandervault.data.mlkit

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.AppPreferencesRepository
import cat.company.wandervault.domain.repository.TripDescriptionRepository
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * ML Kit implementation of [TripDescriptionRepository] that uses the on-device Gemini Nano
 * Prompt API to generate a short, engaging trip summary.
 *
 * @param appPreferences Repository used to read the user-selected AI output language.
 */
class TripDescriptionRepositoryImpl(
    private val appPreferences: AppPreferencesRepository,
) : TripDescriptionRepository {

    private val client by lazy { Generation.getClient() }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        client.checkStatus() != FeatureStatus.UNAVAILABLE
    }

    override suspend fun generateDescription(trip: Trip, destinations: List<Destination>): String? =
        withContext(Dispatchers.IO) {
            when (client.checkStatus()) {
                FeatureStatus.UNAVAILABLE -> return@withContext null
                FeatureStatus.DOWNLOADABLE -> awaitDownload()
                FeatureStatus.AVAILABLE -> Unit
            }
            val request = generateContentRequest(TextPart(buildPrompt(trip, destinations))) {
                maxOutputTokens = MAX_DESCRIPTION_TOKENS
            }
            val response = client.generateContent(request)
            val text = response.candidates.firstOrNull()?.text
            if (text.isNullOrBlank()) {
                throw IllegalStateException(
                    "Gemini Nano returned no description candidates for an available model.",
                )
            }
            text
        }

    override suspend fun generateWhatsNext(
        trip: Trip,
        destinations: List<Destination>,
        now: ZonedDateTime,
    ): String? = withContext(Dispatchers.IO) {
        when (client.checkStatus()) {
            FeatureStatus.UNAVAILABLE -> return@withContext null
            FeatureStatus.DOWNLOADABLE -> awaitDownload()
            FeatureStatus.AVAILABLE -> Unit
        }
        val request = generateContentRequest(TextPart(buildWhatsNextPrompt(trip, destinations, now))) {
            maxOutputTokens = MAX_WHATS_NEXT_TOKENS
        }
        val response = client.generateContent(request)
        val text = response.candidates.firstOrNull()?.text
        if (text.isNullOrBlank()) {
            throw IllegalStateException(
                "Gemini Nano returned no candidates for what's next on an available model.",
            )
        }
        text
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
                    "Focus on the places visited and the overall experience. " +
                    "Respond in ${appPreferences.resolvedAiLanguageName()}.",
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
                            .lowercase(Locale.ROOT)
                            .replaceFirstChar { it.titlecase(Locale.ROOT) }
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

    /**
     * Builds the prompt for the "what's next" notice generation.
     *
     * The prompt describes the current moment in time and the full timezone-aware itinerary,
     * then instructs the model to identify and describe the traveller's next step concisely.
     */
    private fun buildWhatsNextPrompt(
        trip: Trip,
        destinations: List<Destination>,
        now: ZonedDateTime,
    ): String {
        val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        val nowFormatted = now.format(dateTimeFormatter)
        val nowZone = now.zone.id
        return buildString {
            appendLine(
                "You are a helpful travel assistant. Based on the traveller's itinerary and the " +
                    "current date and time, write a concise 1–2 sentence notice describing what " +
                    "their next step in the trip is. Be specific and friendly. " +
                    "If the trip has not started yet, say so and mention when it begins. " +
                    "If the trip is over, say so briefly. " +
                    "Respond in ${appPreferences.resolvedAiLanguageName()}.",
            )
            appendLine()
            appendLine("Current date and time: $nowFormatted (timezone: $nowZone)")
            appendLine("Trip: ${trip.title}")
            if (destinations.isNotEmpty()) {
                appendLine("Itinerary (${destinations.size} stop(s)):")
                destinations.forEachIndexed { index, dest ->
                    append("  ${index + 1}. ${dest.name}")
                    val arrival = dest.arrivalDateTime
                    val departure = dest.departureDateTime
                    if (arrival != null) {
                        append(" – arrives ${arrival.format(dateTimeFormatter)} ${arrival.zone.id}")
                    }
                    if (departure != null) {
                        append(", departs ${departure.format(dateTimeFormatter)} ${departure.zone.id}")
                    }
                    appendLine()
                    dest.transport?.legs?.forEach { leg ->
                        val typeName = leg.type.name
                            .lowercase(Locale.ROOT)
                            .replaceFirstChar { it.titlecase(Locale.ROOT) }
                        append("     → $typeName")
                        if (!leg.company.isNullOrBlank()) append(" with ${leg.company}")
                        if (!leg.flightNumber.isNullOrBlank()) append(" (${leg.flightNumber})")
                        val legDep = leg.departureDateTime
                        val legArr = leg.arrivalDateTime
                        if (legDep != null) {
                            append(
                                ", departs ${legDep.format(dateTimeFormatter)} ${legDep.zone.id}",
                            )
                        }
                        if (legArr != null) {
                            append(
                                ", arrives ${legArr.format(dateTimeFormatter)} ${legArr.zone.id}",
                            )
                        }
                        appendLine()
                    }
                }
            }
        }
    }

    companion object {
        /** Maximum number of tokens the model may generate for a trip description. */
        private const val MAX_DESCRIPTION_TOKENS = 200

        /** Maximum number of tokens the model may generate for a what's next notice. */
        private const val MAX_WHATS_NEXT_TOKENS = 150
    }
}
