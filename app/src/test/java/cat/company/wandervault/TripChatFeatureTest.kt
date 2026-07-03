package cat.company.wandervault

import cat.company.wandervault.data.mlkit.TripDescriptionRepositoryImpl
import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.AppPreferencesRepository
import cat.company.wandervault.domain.repository.TripDescriptionRepository
import cat.company.wandervault.domain.usecase.AskTripQuestionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TripChatFeatureTest {

    private val fakePreferences = object : AppPreferencesRepository {
        override fun getDefaultTimezone(): String? = null
        override fun setDefaultTimezone(zoneId: String?) = Unit
        override fun getAiLanguage(): String? = null
        override fun setAiLanguage(languageTag: String?) = Unit
        override fun getAiEnabled(): Boolean = true
        override fun setAiEnabled(enabled: Boolean) = Unit
        override fun getNotificationsEnabled(): Boolean = true
        override fun setNotificationsEnabled(enabled: Boolean) = Unit
    }

    private val repository = TripDescriptionRepositoryImpl(fakePreferences)

    @Test
    fun `trip chat prompt includes itinerary hotels activities and document summaries`() {
        val trip = Trip(
            id = 7,
            title = "Japan Spring Trip",
            startDate = LocalDate.of(2026, 4, 3),
            endDate = LocalDate.of(2026, 4, 12),
            aiDescription = "A spring journey through Tokyo and Kyoto.",
            nextStep = "Take the train to Kyoto tomorrow morning.",
        )
        val destination = Destination(
            id = 11,
            tripId = trip.id,
            name = "Kyoto",
            position = 1,
            arrivalDateTime = ZonedDateTime.of(2026, 4, 7, 14, 0, 0, 0, ZoneOffset.UTC),
            departureDateTime = ZonedDateTime.of(2026, 4, 10, 9, 30, 0, 0, ZoneOffset.UTC),
            notes = "Visit the bamboo forest and arrive early at temples.",
        )
        val activity = Activity(
            id = 3,
            destinationId = destination.id,
            title = "Tea ceremony",
            description = "Booked for the afternoon.",
            dateTime = ZonedDateTime.of(2026, 4, 8, 15, 0, 0, 0, ZoneOffset.UTC),
            confirmationNumber = "ACT-42",
        )
        val hotel = Hotel(
            id = 5,
            destinationId = destination.id,
            name = "Sakura Inn Kyoto",
            address = "12 Gion Street",
            reservationNumber = "HOTEL-99",
        )
        val document = TripDocument(
            id = 9,
            tripId = trip.id,
            name = "kyoto-hotel.pdf",
            uri = "file:///kyoto-hotel.pdf",
            mimeType = "application/pdf",
            summary = "Hotel booking for Sakura Inn Kyoto from April 7 to April 10.",
        )

        val prompt = repository.buildTripQuestionPrompt(
            trip = trip,
            destinations = listOf(destination),
            activities = listOf(activity),
            hotelsByDestination = mapOf(destination.id to hotel),
            documents = listOf(document),
            question = "Where am I staying in Kyoto?",
        )

        assertTrue(prompt.contains("Question: Where am I staying in Kyoto?"))
        assertTrue(prompt.contains("Trip summary: A spring journey through Tokyo and Kyoto."))
        assertTrue(prompt.contains("Current what's next note: Take the train to Kyoto tomorrow morning."))
        assertTrue(prompt.contains("Notes: Visit the bamboo forest and arrive early at temples."))
        assertTrue(prompt.contains("Hotel: Sakura Inn Kyoto"))
        assertTrue(prompt.contains("Tea ceremony"))
        assertTrue(prompt.contains("Summary: Hotel booking for Sakura Inn Kyoto from April 7 to April 10."))
    }

    @Test
    fun `trip chat prompt mentions when a document lacks summary`() {
        val prompt = repository.buildTripQuestionPrompt(
            trip = Trip(id = 1, title = "Weekend Trip"),
            destinations = emptyList(),
            activities = emptyList(),
            hotelsByDestination = emptyMap(),
            documents = listOf(
                TripDocument(
                    id = 1,
                    tripId = 1,
                    name = "boarding-pass.png",
                    uri = "file:///boarding-pass.png",
                    mimeType = "image/png",
                    summary = null,
                ),
            ),
            question = "Do I have any uploaded tickets?",
        )

        assertTrue(prompt.contains("No AI summary available for this document."))
    }

    @Test
    fun `AskTripQuestionUseCase forwards arguments to repository`() = runTest {
        val fakeRepository = FakeTripDescriptionRepository()
        val useCase = AskTripQuestionUseCase(fakeRepository)
        val trip = Trip(id = 4, title = "City Break")
        val destinations = listOf(Destination(id = 10, tripId = 4, name = "Lisbon", position = 0))
        val activities = listOf(Activity(id = 1, destinationId = 10, title = "Walk"))
        val hotels = mapOf(10 to Hotel(destinationId = 10, name = "Central Hotel"))
        val documents = listOf(
            TripDocument(
                id = 2,
                tripId = 4,
                name = "reservation.txt",
                uri = "file:///reservation.txt",
                mimeType = "text/plain",
                summary = "Reservation details.",
            ),
        )

        val result = useCase(
            trip = trip,
            destinations = destinations,
            activities = activities,
            hotelsByDestination = hotels,
            documents = documents,
            question = "What have I booked?",
        )

        assertEquals("Trip answer", result)
        assertEquals("What have I booked?", fakeRepository.lastQuestion)
        assertEquals(documents, fakeRepository.lastDocuments)
        assertEquals(hotels, fakeRepository.lastHotelsByDestination)
    }
}

private class FakeTripDescriptionRepository : TripDescriptionRepository {
    var lastQuestion: String? = null
    var lastDocuments: List<TripDocument>? = null
    var lastHotelsByDestination: Map<Int, Hotel>? = null

    override suspend fun isAvailable(): Boolean = true

    override suspend fun isDeviceSupported(): Boolean = true

    override suspend fun generateDescription(trip: Trip, destinations: List<Destination>): String? = null

    override suspend fun generateWhatsNext(
        trip: Trip,
        destinations: List<Destination>,
        now: ZonedDateTime,
        activities: List<Activity>,
    ): String? = null

    override suspend fun askTripQuestion(
        trip: Trip,
        destinations: List<Destination>,
        activities: List<Activity>,
        hotelsByDestination: Map<Int, Hotel>,
        documents: List<TripDocument>,
        question: String,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)?,
    ): String? {
        lastQuestion = question
        lastDocuments = documents
        lastHotelsByDestination = hotelsByDestination
        return "Trip answer"
    }
}
