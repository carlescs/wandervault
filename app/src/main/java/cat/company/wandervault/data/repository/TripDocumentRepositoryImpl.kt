package cat.company.wandervault.data.repository

import android.net.Uri
import cat.company.wandervault.data.local.TripDocumentDao
import cat.company.wandervault.data.local.TripDocumentEntity
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TripDocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class TripDocumentRepositoryImpl(
    private val tripDocumentDao: TripDocumentDao,
) : TripDocumentRepository {

    override fun getDocumentsForTrip(tripId: Int): Flow<List<TripDocument>> =
        tripDocumentDao.getByTripId(tripId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveDocument(document: TripDocument): Long =
        tripDocumentDao.insert(document.toEntity())

    override suspend fun deleteDocument(document: TripDocument) {
        tripDocumentDao.delete(document.toEntity())
        deleteFileIfInternal(document.localUri)
    }

    private suspend fun deleteFileIfInternal(fileUri: String) = withContext(Dispatchers.IO) {
        try {
            val path = Uri.parse(fileUri).path ?: return@withContext
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
            // Ignore deletion failures
        }
    }
}

private fun TripDocumentEntity.toDomain() = TripDocument(
    id = id,
    tripId = tripId,
    name = name,
    localUri = localUri,
    mimeType = mimeType,
    folder = folder,
    extractedText = extractedText,
    createdAt = createdAt,
)

private fun TripDocument.toEntity() = TripDocumentEntity(
    id = id,
    tripId = tripId,
    name = name,
    localUri = localUri,
    mimeType = mimeType,
    folder = folder,
    extractedText = extractedText,
    createdAt = createdAt,
)
