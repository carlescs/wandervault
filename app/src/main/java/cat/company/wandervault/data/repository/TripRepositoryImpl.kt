package cat.company.wandervault.data.repository

import cat.company.wandervault.data.local.DestinationDao
import cat.company.wandervault.data.local.DestinationEntity
import cat.company.wandervault.data.local.TripDao
import cat.company.wandervault.data.local.TripEntity
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class TripRepositoryImpl(
    private val tripDao: TripDao,
    private val destinationDao: DestinationDao,
) : TripRepository {

    override fun getTrips(): Flow<List<Trip>> = combine(
        tripDao.getAll(),
        destinationDao.getAll(),
    ) { tripEntities, destinationEntities ->
        tripEntities.map { tripEntity ->
            val destinations = destinationEntities.filter { it.tripId == tripEntity.id }
            tripEntity.toDomain(destinations)
        }
    }

    override fun getTripById(id: Int): Flow<Trip?> = combine(
        tripDao.getById(id),
        destinationDao.getByTripId(id),
    ) { tripEntity, destinations ->
        tripEntity?.toDomain(destinations)
    }

    override suspend fun saveTrip(trip: Trip) {
        tripDao.insert(trip.toEntity())
    }

    override suspend fun updateTrip(trip: Trip) {
        tripDao.update(trip.toEntity())
    }
}

/** Derives [Trip.startDate] and [Trip.endDate] from the given destination rows. */
private fun TripEntity.toDomain(destinations: List<DestinationEntity>): Trip {
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
    )
}

private fun Trip.toEntity() = TripEntity(
    id = id,
    title = title,
    imageUri = imageUri,
)
