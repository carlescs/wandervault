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

class TripRepositoryImpl(
    private val tripDao: TripDao,
    private val destinationDao: DestinationDao,
    private val database: WanderVaultDatabase,
) : TripRepository {

    override fun getTrips(): Flow<List<Trip>> = combine(
        tripDao.getAll(),
        destinationDao.getAll(),
    ) { tripEntities, destinationProjections ->
        val destinationsByTripId = destinationProjections.groupBy { it.tripId }
        tripEntities
            .map { tripEntity ->
                val destinations = destinationsByTripId[tripEntity.id].orEmpty()
                tripEntity.toDomain(destinations)
            }
            .sortedWith(compareBy(nullsLast<LocalDate>()) { trip: Trip -> trip.startDate }.thenBy { it.id })
    }

    override fun getFavoriteTrips(): Flow<List<Trip>> = combine(
        tripDao.getFavorites(),
        destinationDao.getAll(),
    ) { tripEntities, destinationProjections ->
        val destinationsByTripId = destinationProjections.groupBy { it.tripId }
        tripEntities
            .map { tripEntity ->
                val destinations = destinationsByTripId[tripEntity.id].orEmpty()
                tripEntity.toDomain(destinations)
            }
            .sortedWith(compareBy(nullsLast<LocalDate>()) { trip: Trip -> trip.startDate }.thenBy { it.id })
    }

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

    override suspend fun deleteTrip(trip: Trip) {
        database.withTransaction {
            destinationDao.deleteByTripId(trip.id)
            tripDao.delete(trip.toEntity())
        }
    }
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
    )
}

private fun Trip.toEntity() = TripEntity(
    id = id,
    title = title,
    imageUri = imageUri,
    aiDescription = aiDescription,
    isFavorite = isFavorite,
)
