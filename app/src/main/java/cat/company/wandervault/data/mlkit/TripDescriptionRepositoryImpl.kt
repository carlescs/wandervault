package cat.company.wandervault.data.mlkit

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.TransportType
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
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * ML Kit implementation of [TripDescriptionRepository] that uses the on-device Gemini Nano
 * Prompt API to generate a short, engaging trip summary.
 *
 * @param appPreferences Repository used to read the user-selected AI output language and to
 *   enforce the app-wide AI kill-switch.
 */
class TripDescriptionRepositoryImpl(
    private val appPreferences: AppPreferencesRepository,
) : TripDescriptionRepository {

    private val client by lazy { Generation.getClient() }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        appPreferences.getAiEnabled() && client.checkStatus() != FeatureStatus.UNAVAILABLE
    }

    override suspend fun isDeviceSupported(): Boolean = withContext(Dispatchers.IO) {
        client.checkStatus() != FeatureStatus.UNAVAILABLE
    }

    override suspend fun generateDescription(trip: Trip, destinations: List<Destination>): String? =
        withContext(Dispatchers.IO) {
            if (!appPreferences.getAiEnabled()) return@withContext null
            when (client.checkStatus()) {
                FeatureStatus.UNAVAILABLE -> return@withContext null
                FeatureStatus.DOWNLOADABLE -> awaitDownload()
                FeatureStatus.AVAILABLE -> Unit
            }
            val request = createRequest(buildPrompt(trip, destinations), MAX_DESCRIPTION_TOKENS)
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
        if (!appPreferences.getAiEnabled()) return@withContext null
        when (client.checkStatus()) {
            FeatureStatus.UNAVAILABLE -> return@withContext null
            FeatureStatus.DOWNLOADABLE -> awaitDownload()
            FeatureStatus.AVAILABLE -> Unit
        }
        val request = createRequest(buildWhatsNextPrompt(trip, destinations, now), MAX_WHATS_NEXT_TOKENS)
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
     * Creates a [generateContentRequest] with the given [prompt] text and [maxTokens] limit.
     */
    private fun createRequest(prompt: String, maxTokens: Int) =
        generateContentRequest(TextPart(prompt)) {
            maxOutputTokens = maxTokens
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
        return buildString {
            appendLine(
                "Write a short, engaging 2–3 sentence description for the following trip. " +
                    "Focus on the places visited and the overall experience. " +
                    "Only use the facts provided below. Do not invent or assume any destinations, " +
                    "activities, or details that are not explicitly listed. " +
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
                    val arrival = dest.arrivalDateTime?.format(PROMPT_DATE_TIME_FORMATTER)
                    val departure = dest.departureDateTime?.format(PROMPT_DATE_TIME_FORMATTER)
                    if (arrival != null) append(" – arrives $arrival")
                    if (departure != null) append(", departs $departure")
                    appendLine()
                    dest.transport?.legs?.forEach { leg ->
                        append("     → ${formatTransportTypeName(leg.type)}")
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
     * The prompt describes the current moment in time, the traveller's computed current position
     * (derived from the itinerary — see [describeTravellerPosition]), the single most immediate
     * upcoming event (see [findNextUpcomingEvent]), and the full timezone-aware itinerary
     * (including per-destination notes), then instructs the model to identify and describe the
     * traveller's next step.
     */
    private fun buildWhatsNextPrompt(
        trip: Trip,
        destinations: List<Destination>,
        now: ZonedDateTime,
    ): String {
        val nowFormatted = now.format(PROMPT_DATE_TIME_FORMATTER)
        val nowZone = now.zone.id
        val position = describeTravellerPosition(destinations, now)
        val nextEvent = findNextUpcomingEvent(destinations, now)
        return buildString {
            appendLine(
                "You are a helpful travel assistant. Based on the traveller's current position " +
                    "and itinerary, write a concise 1–2 sentence notice describing what their " +
                    "next step is. Focus on the single most immediate upcoming event (departure, " +
                    "arrival, or transport leg). Only use the facts provided below. Do not invent " +
                    "or assume any events, times, places, or details not explicitly present in " +
                    "the facts provided below. " +
                    "Respond in ${appPreferences.resolvedAiLanguageName()}.",
            )
            appendLine()
            appendLine("Current date and time: $nowFormatted (timezone: $nowZone)")
            appendLine("Traveller's current position: $position")
            if (nextEvent != null) {
                appendLine("Next upcoming event: $nextEvent")
            }
            appendLine("Trip: ${trip.title}")
            if (destinations.isNotEmpty()) {
                appendLine("Itinerary (${destinations.size} stop(s)):")
                destinations.forEachIndexed { index, dest ->
                    append("  ${index + 1}. ${dest.name}")
                    val arrival = dest.arrivalDateTime
                    val departure = dest.departureDateTime
                    if (arrival != null) {
                        append(" – arrives ${arrival.format(PROMPT_DATE_TIME_FORMATTER)} ${arrival.zone.id}")
                    }
                    if (departure != null) {
                        append(", departs ${departure.format(PROMPT_DATE_TIME_FORMATTER)} ${departure.zone.id}")
                    }
                    appendLine()
                    if (!dest.notes.isNullOrBlank()) {
                        appendLine("     Notes: ${dest.notes}")
                    }
                    dest.transport?.legs?.forEach { leg ->
                        append("     → ${formatTransportTypeName(leg.type)}")
                        if (!leg.company.isNullOrBlank()) append(" with ${leg.company}")
                        if (!leg.flightNumber.isNullOrBlank()) append(" (${leg.flightNumber})")
                        val legDep = leg.departureDateTime
                        val legArr = leg.arrivalDateTime
                        if (legDep != null) {
                            append(
                                ", departs ${legDep.format(PROMPT_DATE_TIME_FORMATTER)} ${legDep.zone.id}",
                            )
                        }
                        if (legArr != null) {
                            append(
                                ", arrives ${legArr.format(PROMPT_DATE_TIME_FORMATTER)} ${legArr.zone.id}",
                            )
                        }
                        appendLine()
                    }
                }
            }
        }
    }

    /**
     * Finds the single most immediate upcoming itinerary event after [now] and returns a
     * human-readable description of it, or `null` if there are no upcoming events.
     *
     * All candidate events — destination arrivals, destination departures, and individual
     * transport-leg departures/arrivals — are collected and the one with the earliest
     * [java.time.Instant] is returned.  This gives the model a clear, unambiguous anchor for
     * the "next step" rather than asking it to reason over the entire itinerary.
     */
    internal fun findNextUpcomingEvent(destinations: List<Destination>, now: ZonedDateTime): String? {
        val nowInstant = now.toInstant()
        // Sort by position so that event descriptions reference stops in itinerary order,
        // matching the ordering used by describeTravellerPosition. Destinations are not
        // guaranteed to arrive pre-sorted from callers.
        val sorted = destinations.sortedBy { it.position }

        val futureEvents = mutableListOf<ItineraryEvent>()
        for (dest in sorted) {
            dest.arrivalDateTime?.let { arrival ->
                if (arrival.toInstant().isAfter(nowInstant)) {
                    futureEvents.add(
                        ItineraryEvent(
                            arrival.toInstant(),
                            "arrive at ${dest.name} on ${arrival.format(PROMPT_DATE_TIME_FORMATTER)} ${arrival.zone.id}",
                        ),
                    )
                }
            }
            dest.departureDateTime?.let { departure ->
                if (departure.toInstant().isAfter(nowInstant)) {
                    futureEvents.add(
                        ItineraryEvent(
                            departure.toInstant(),
                            "depart from ${dest.name} on ${departure.format(PROMPT_DATE_TIME_FORMATTER)} ${departure.zone.id}",
                        ),
                    )
                }
            }
            dest.transport?.legs?.forEach { leg ->
                leg.departureDateTime?.let { legDep ->
                    if (legDep.toInstant().isAfter(nowInstant)) {
                        val description = buildString {
                            append(formatTransportTypeName(leg.type))
                            if (!leg.company.isNullOrBlank()) append(" with ${leg.company}")
                            if (!leg.flightNumber.isNullOrBlank()) append(" (${leg.flightNumber})")
                            append(
                                " departs on ${legDep.format(PROMPT_DATE_TIME_FORMATTER)} ${legDep.zone.id}",
                            )
                        }
                        futureEvents.add(ItineraryEvent(legDep.toInstant(), description))
                    }
                }
                leg.arrivalDateTime?.let { legArr ->
                    if (legArr.toInstant().isAfter(nowInstant)) {
                        val description = buildString {
                            append(formatTransportTypeName(leg.type))
                            if (!leg.company.isNullOrBlank()) append(" with ${leg.company}")
                            if (!leg.flightNumber.isNullOrBlank()) append(" (${leg.flightNumber})")
                            append(
                                " arrives on ${legArr.format(PROMPT_DATE_TIME_FORMATTER)} ${legArr.zone.id}",
                            )
                        }
                        futureEvents.add(ItineraryEvent(legArr.toInstant(), description))
                    }
                }
            }
        }

        return futureEvents.minByOrNull { it.instant }?.description
    }

    /**
     * Determines the traveller's current position in the trip based on [now] and the
     * destination itinerary.
     *
     * All temporal comparisons are performed on [java.time.Instant] values (UTC epoch seconds)
     * to ensure correct ordering regardless of the zones attached to individual datetimes.
     *
     * Rules (applied in order):
     * - **At home (before trip)** – [now] is before the earliest timed event in the itinerary.
     * - **At the first destination** – the first destination has no arrival; the traveller is
     *   considered to be there from the beginning until its departure.
     * - **At a destination** – [now] falls between the destination's arrival and departure times.
     * - **Traveling** – [now] falls between the departure of one stop and the arrival of the next.
     * - **At home (after trip)** – [now] is at or after the latest timed event in the itinerary.
     * - **Unknown** – no timed events are present, so the position cannot be determined.
     */
    private fun describeTravellerPosition(
        destinations: List<Destination>,
        now: ZonedDateTime,
    ): String {
        if (destinations.isEmpty()) return POSITION_UNKNOWN

        val sorted = destinations.sortedBy { it.position }

        val allInstants = sorted
            .flatMap { listOfNotNull(it.arrivalDateTime, it.departureDateTime) }
            .map { it.toInstant() }
        if (allInstants.isEmpty()) return POSITION_UNKNOWN

        val nowInstant = now.toInstant()
        // allInstants is guaranteed non-empty by the isEmpty() check above.
        val tripStartInstant = allInstants.min()
        val tripEndInstant = allInstants.max()

        if (nowInstant.isBefore(tripStartInstant)) return POSITION_AT_HOME_BEFORE_TRIP
        if (!nowInstant.isBefore(tripEndInstant)) return POSITION_AT_HOME_AFTER_TRIP

        for (i in sorted.indices) {
            val dest = sorted[i]

            if (isTravellerAtDestination(dest, i, nowInstant)) return "currently at ${dest.name}"

            // Check if the traveller is in transit between this stop and the next.
            if (i + 1 < sorted.size &&
                isTravellerInTransit(dest, sorted[i + 1], nowInstant)
            ) {
                return "currently traveling from ${dest.name} to ${sorted[i + 1].name}"
            }
        }

        return POSITION_UNKNOWN
    }

    /**
     * Returns `true` if the traveller is currently at [dest] given [nowInstant].
     *
     * For the first destination (index == 0) there is no recorded arrival time — the traveller
     * is considered to start there and remains until the departure.  For all other destinations
     * the traveller must have arrived ([Destination.arrivalDateTime] ≤ now) and not yet departed.
     */
    private fun isTravellerAtDestination(
        dest: Destination,
        index: Int,
        nowInstant: Instant,
    ): Boolean {
        val departureInstant = dest.departureDateTime?.toInstant()
        val notDeparted = departureInstant == null || nowInstant.isBefore(departureInstant)
        return if (index == 0) {
            notDeparted
        } else {
            val arrivalInstant = dest.arrivalDateTime?.toInstant() ?: return false
            !nowInstant.isBefore(arrivalInstant) && notDeparted
        }
    }

    /**
     * Returns `true` if the traveller is currently in transit from [from] to [to] given
     * [nowInstant] — i.e. [from]'s departure has passed but [to]'s arrival has not yet occurred.
     */
    private fun isTravellerInTransit(
        from: Destination,
        to: Destination,
        nowInstant: Instant,
    ): Boolean {
        val depInstant = from.departureDateTime?.toInstant() ?: return false
        val arrInstant = to.arrivalDateTime?.toInstant() ?: return false
        return !nowInstant.isBefore(depInstant) && nowInstant.isBefore(arrInstant)
    }

    companion object {
        /** Maximum number of tokens the model may generate for a trip description. */
        private const val MAX_DESCRIPTION_TOKENS = 200

        /** Maximum number of tokens the model may generate for a what's next notice. */
        private const val MAX_WHATS_NEXT_TOKENS = 150

        /** Position string used when the traveller's location cannot be determined. */
        private const val POSITION_UNKNOWN = "unknown"

        /** Position string used when now is before the first itinerary event. */
        private const val POSITION_AT_HOME_BEFORE_TRIP = "at home (the trip has not started yet)"

        /** Position string used when now is at or after the last itinerary event. */
        private const val POSITION_AT_HOME_AFTER_TRIP = "at home (the trip is over)"

        /**
         * Formatter for datetime values included in LLM prompts.
         *
         * Uses an explicit ISO-style pattern (`yyyy-MM-dd HH:mm`) to produce stable, locale-independent
         * output (e.g. `2026-04-02 10:30`) that is unambiguous for LLM reasoning.
         * The timezone zone ID is always appended separately in the prompt text.
         */
        private val PROMPT_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        /**
         * Converts a [TransportType] enum value to a human-readable title-case string
         * (e.g. `FLIGHT` → `"Flight"`).
         */
        internal fun formatTransportTypeName(type: TransportType): String =
            type.name.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }

        /**
         * A single itinerary event considered by [findNextUpcomingEvent].
         *
         * @param instant The UTC instant of the event, used for chronological ordering.
         * @param description A human-readable description included in the prompt.
         */
        internal data class ItineraryEvent(val instant: Instant, val description: String)
    }
}
