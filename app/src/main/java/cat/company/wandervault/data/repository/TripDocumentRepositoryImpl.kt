package cat.company.wandervault.data.repository

import cat.company.wandervault.data.local.TripDocumentDao
import cat.company.wandervault.data.local.TripDocumentEntity
import cat.company.wandervault.data.local.TripDocumentFolderDao
import cat.company.wandervault.data.local.TripDocumentFolderEntity
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.repository.TripDocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TripDocumentRepositoryImpl(
    private val folderDao: TripDocumentFolderDao,
    private val documentDao: TripDocumentDao,
) : TripDocumentRepository {

    override fun getRootFolders(tripId: Int): Flow<List<TripDocumentFolder>> =
        folderDao.getRootFolders(tripId).map { list -> list.map { it.toDomain() } }

    override fun getSubFolders(parentFolderId: Int): Flow<List<TripDocumentFolder>> =
        folderDao.getSubFolders(parentFolderId).map { list -> list.map { it.toDomain() } }

    override fun getDocumentsInFolder(folderId: Int): Flow<List<TripDocument>> =
        documentDao.getByFolderId(folderId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveFolder(folder: TripDocumentFolder) {
        requireUniqueFolderName(folder)
        folderDao.insert(folder.toEntity())
    }

    override suspend fun updateFolder(folder: TripDocumentFolder) {
        requireUniqueFolderName(folder, excludeId = folder.id)
        folderDao.update(folder.toEntity())
    }

    override suspend fun deleteFolder(folder: TripDocumentFolder) {
        folderDao.delete(folder.toEntity())
    }

    override suspend fun saveDocument(document: TripDocument) {
        documentDao.insert(document.toEntity())
    }

    override suspend fun updateDocument(document: TripDocument) {
        documentDao.update(document.toEntity())
    }

    override suspend fun deleteDocument(document: TripDocument) {
        documentDao.delete(document.toEntity())
    }

    /**
     * Enforces uniqueness for root-level folders where parentFolderId is null, since SQLite's
     * unique index treats NULL values as distinct and cannot enforce this constraint natively.
     * Sub-folder uniqueness is handled by the DB unique index on (tripId, parentFolderId, name).
     *
     * @throws IllegalArgumentException if a root-level folder with the same name already exists.
     */
    private suspend fun requireUniqueFolderName(folder: TripDocumentFolder, excludeId: Int = 0) {
        if (folder.parentFolderId == null) {
            val count = folderDao.countRootFoldersByName(folder.tripId, folder.name, excludeId)
            require(count == 0) {
                "A root-level folder named '${folder.name}' already exists in trip ${folder.tripId}."
            }
        }
    }
}

private fun TripDocumentFolderEntity.toDomain() = TripDocumentFolder(
    id = id,
    tripId = tripId,
    name = name,
    parentFolderId = parentFolderId,
)

private fun TripDocumentFolder.toEntity() = TripDocumentFolderEntity(
    id = id,
    tripId = tripId,
    name = name,
    parentFolderId = parentFolderId,
)

private fun TripDocumentEntity.toDomain() = TripDocument(
    id = id,
    folderId = folderId,
    name = name,
    uri = uri,
    mimeType = mimeType,
)

private fun TripDocument.toEntity() = TripDocumentEntity(
    id = id,
    folderId = folderId,
    name = name,
    uri = uri,
    mimeType = mimeType,
)
