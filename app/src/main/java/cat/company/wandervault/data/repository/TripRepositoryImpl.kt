package cat.company.wandervault.data.repository

import androidx.room.withTransaction
import cat.company.wandervault.data.local.DestinationDao
import cat.company.wandervault.data.local.DestinationDateProjection
import cat.company.wandervault.data.local.TripDao
import cat.company.wandervault.data.local.TripEntity
import cat.company.wandervault.data.local.WanderVaultDatabase
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZonedDateTime

class TripRepositoryImpl(
    private val tripDao: TripDao,
    private val destinationDao: DestinationDao,
    private val database: WanderVaultDatabase,
) : TripRepository {

    override fun getTrips(): Flow<List<Trip>> = combine(
        tripDao.getAll(),
        destinationDao.getAll(),
        ::mapToSortedTripList,
    )

    override fun getFavoriteTrips(): Flow<List<Trip>> = combine(
        tripDao.getFavorites(),
        destinationDao.getAll(),
        ::mapToSortedTripList,
    )

    override fun getArchivedTrips(): Flow<List<Trip>> = combine(
        tripDao.getArchived(),
        destinationDao.getAll(),
        ::mapToSortedTripList,
    )

    override fun getTripById(id: Int): Flow<Trip?> = combine(
        tripDao.getById(id),
        destinationDao.getDateProjectionsForTrip(id),
    ) { tripEntity, destinations ->
        tripEntity?.toDomain(destinations)
    }

    override suspend fun saveTrip(trip: Trip) {
        tripDao.insert(trip.toEntity())
    }

    override suspend fun updateTrip(trip: Trip) {
        tripDao.update(trip.toEntity())
    }

    override suspend fun toggleFavoriteTrip(tripId: Int) {
        tripDao.toggleFavorite(tripId)
    }

    override suspend fun archiveTrip(tripId: Int) {
        tripDao.setArchived(tripId, true)
    }

    override suspend fun unarchiveTrip(tripId: Int) {
        tripDao.setArchived(tripId, false)
    }

    override suspend fun deleteTrip(trip: Trip) {
        database.withTransaction {
            destinationDao.deleteByTripId(trip.id)
            tripDao.delete(trip.toEntity())
        }
    }

    override suspend fun updateTripWhatsNext(
        tripId: Int,
        nextStep: String?,
        nextStepDeadline: ZonedDateTime?,
    ) {
        // Serialise ZonedDateTime → String using the same format as DateConverters.
        tripDao.updateWhatsNext(
            tripId = tripId,
            nextStep = nextStep,
            nextStepDeadline = nextStepDeadline?.toString(),
        )
    }
}

private fun mapToSortedTripList(
    tripEntities: List<TripEntity>,
    destinationProjections: List<DestinationDateProjection>,
): List<Trip> {
    val destinationsByTripId = destinationProjections.groupBy { it.tripId }
    return tripEntities
        .map { tripEntity ->
            val destinations = destinationsByTripId[tripEntity.id].orEmpty()
            tripEntity.toDomain(destinations)
        }
        .sortedWith(compareBy(nullsLast<LocalDate>()) { trip: Trip -> trip.startDate }.thenBy { it.id })
}

/** Derives [Trip.startDate] and [Trip.endDate] from the given destination date projections. */
private fun TripEntity.toDomain(destinations: List<DestinationDateProjection>): Trip {
    val allDates: List<LocalDate> = destinations.flatMap { dest ->
        listOfNotNull(
            dest.arrivalDateTime?.toLocalDate(),
            dest.departureDateTime?.toLocalDate(),
        )
    }
    return Trip(
        id = id,
        title = title,
        imageUri = imageUri,
        startDate = allDates.minOrNull(),
        endDate = allDates.maxOrNull(),
        aiDescription = aiDescription,
        isFavorite = isFavorite,
        defaultTimezone = defaultTimezone,
        nextStep = nextStep,
        nextStepDeadline = nextStepDeadline,
        isArchived = isArchived,
    )
}

private fun Trip.toEntity() = TripEntity(
    id = id,
    title = title,
    imageUri = imageUri,
    aiDescription = aiDescription,
    isFavorite = isFavorite,
    defaultTimezone = defaultTimezone,
    nextStep = nextStep,
    nextStepDeadline = nextStepDeadline,
    isArchived = isArchived,
)
