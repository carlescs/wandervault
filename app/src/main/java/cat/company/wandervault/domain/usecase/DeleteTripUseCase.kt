package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.ImageRepository
import cat.company.wandervault.domain.repository.TripDocumentRepository
import cat.company.wandervault.domain.repository.TripRepository

/**
 * Use-case that permanently removes a [Trip] and all its associated data from the repository,
 * including physical document files and the trip cover image from internal storage.
 *
 * Ordering:
 * 1. Collect document file URIs while the DB rows still exist.
 * 2. Delete the trip from the DB — CASCADE removes destination, folder, and document rows.
 * 3. Best-effort: delete each physical document file.
 * 4. Best-effort: delete the trip cover image file.
 *
 * Steps 3 and 4 never throw; a failure there leaves orphaned files but preserves DB consistency.
 */
class DeleteTripUseCase(
    private val tripRepository: TripRepository,
    private val documentRepository: TripDocumentRepository,
    private val imageRepository: ImageRepository,
) {
    suspend operator fun invoke(trip: Trip) {
        // Step 1: Collect document URIs before the CASCADE removes their DB rows.
        val documentUris = try {
            documentRepository.getAllDocumentUrisForTrip(trip.id)
        } catch (_: Exception) {
            emptyList()
        }

        // Step 2: Delete the trip from DB; CASCADE removes destinations, folders, and document rows.
        tripRepository.deleteTrip(trip)

        // Step 3: Best-effort cleanup of physical document files (never throws).
        documentUris.forEach { uri ->
            try { documentRepository.deleteDocumentFileByUri(uri) } catch (_: Exception) { }
        }

        // Step 4: Best-effort cleanup of the trip cover image (never throws).
        trip.imageUri?.takeIf { it.startsWith("file://") }?.let { uri ->
            try { imageRepository.deleteFromInternalStorage(uri) } catch (_: Exception) { }
        }
    }
}
