package cat.company.wandervault.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import cat.company.wandervault.data.local.TripDocumentDao
import cat.company.wandervault.data.local.TripDocumentEntity
import cat.company.wandervault.data.local.TripDocumentFolderDao
import cat.company.wandervault.data.local.TripDocumentFolderEntity
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.repository.TripDocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

class TripDocumentRepositoryImpl(
    private val context: Context,
    private val folderDao: TripDocumentFolderDao,
    private val documentDao: TripDocumentDao,
) : TripDocumentRepository {

    /**
     * Resolved canonical path of the internal documents directory.
     * Computed once to avoid repeated file-system calls on every deletion.
     */
    private val internalDocsDirPath by lazy {
        File(context.filesDir, "documents").canonicalFile.toPath()
    }

    override fun getDocumentById(id: Int): Flow<TripDocument?> =
        documentDao.getById(id).map { it?.toDomain() }

    override fun getRootFolders(tripId: Int): Flow<List<TripDocumentFolder>> =
        folderDao.getRootFolders(tripId).map { list -> list.map { it.toDomain() } }

    override fun getSubFolders(parentFolderId: Int): Flow<List<TripDocumentFolder>> =
        folderDao.getSubFolders(parentFolderId).map { list -> list.map { it.toDomain() } }

    override fun getDocumentsInFolder(folderId: Int): Flow<List<TripDocument>> =
        documentDao.getByFolderId(folderId).map { list -> list.map { it.toDomain() } }

    override fun getRootDocuments(tripId: Int): Flow<List<TripDocument>> =
        documentDao.getRootDocuments(tripId).map { list -> list.map { it.toDomain() } }

    override fun getAllFoldersForTrip(tripId: Int): Flow<List<TripDocumentFolder>> =
        folderDao.getAllByTripId(tripId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveFolder(folder: TripDocumentFolder) {
        requireUniqueFolderName(folder)
        folderDao.insert(folder.toEntity())
    }

    override suspend fun updateFolder(folder: TripDocumentFolder) {
        requireUniqueFolderName(folder, excludeId = folder.id)
        folderDao.update(folder.toEntity())
    }

    override suspend fun deleteFolder(folder: TripDocumentFolder) {
        // Collect all document URIs in the folder tree before the cascade-delete wipes them from DB.
        val urisToDelete = documentDao.getDocumentUrisInFolderTree(folder.id)
        folderDao.delete(folder.toEntity())
        urisToDelete.forEach { deleteFileFromInternalStorage(it) }
    }

    override suspend fun saveDocument(document: TripDocument) {
        requireUniqueRootDocumentName(document)
        documentDao.insert(document.toEntity())
    }

    override suspend fun updateDocument(document: TripDocument) {
        requireUniqueRootDocumentName(document, excludeId = document.id)
        documentDao.update(document.toEntity())
    }

    override suspend fun deleteDocument(document: TripDocument) {
        documentDao.delete(document.toEntity())
        deleteFileFromInternalStorage(document.uri)
    }

    override suspend fun copyDocumentToInternalStorage(sourceUri: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(sourceUri)
                val mimeType = context.contentResolver.getType(uri)
                val extension = mimeType?.let {
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
                } ?: "bin"
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext null
                val documentsDir = File(context.filesDir, "documents")
                if (!(documentsDir.exists() || documentsDir.mkdirs())) return@withContext null
                val file = File(documentsDir, "${UUID.randomUUID()}.$extension")
                inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(file).toString()
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            }
        }

    override suspend fun getAllDocumentUrisForTrip(tripId: Int): List<String> =
        documentDao.getAllDocumentUrisByTripId(tripId)

    override suspend fun deleteDocumentFileByUri(fileUri: String) =
        deleteFileFromInternalStorage(fileUri)

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

    /**
     * Enforces uniqueness for root-level documents (folderId IS NULL) within a trip, since
     * SQLite's unique index treats NULL values as distinct.
     * In-folder document uniqueness is handled by the DB unique index on (folderId, name).
     *
     * @throws IllegalArgumentException if a root-level document with the same name already exists.
     */
    private suspend fun requireUniqueRootDocumentName(document: TripDocument, excludeId: Int = 0) {
        if (document.folderId == null) {
            val count = documentDao.countRootDocumentsByName(document.tripId, document.name, excludeId)
            require(count == 0) {
                "A root-level document named '${document.name}' already exists in trip ${document.tripId}."
            }
        }
    }

    /**
     * Deletes the physical file at [fileUri] from internal documents storage.
     * Only files residing under `filesDir/documents/` are deleted; external URIs are ignored.
     * Deletion failures are silently ignored — a missing or inaccessible file must never prevent
     * the DB cleanup that already completed.
     *
     * Note: [file.canonicalFile] resolves symlinks via a filesystem call. This is intentional for
     * security (path-traversal prevention) and is safe since all calls run on [Dispatchers.IO].
     */
    private suspend fun deleteFileFromInternalStorage(fileUri: String) =
        withContext(Dispatchers.IO) {
            try {
                val path = Uri.parse(fileUri).path ?: return@withContext
                val file = File(path)
                if (file.canonicalFile.toPath().startsWith(internalDocsDirPath)) {
                    file.delete()
                }
            } catch (e: IOException) {
                // Ignore: file may already be deleted or inaccessible; DB cleanup must proceed.
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
    tripId = tripId,
    folderId = folderId,
    name = name,
    uri = uri,
    mimeType = mimeType,
    summary = summary,
    description = description,
)

private fun TripDocument.toEntity() = TripDocumentEntity(
    id = id,
    tripId = tripId,
    folderId = folderId,
    name = name,
    uri = uri,
    mimeType = mimeType,
    summary = summary,
    description = description,
)
