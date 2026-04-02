package cat.company.wandervault.data.remote.firebase

import android.content.Context
import cat.company.wandervault.data.local.TripDocumentDao
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.RemoteDocumentRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Firebase Storage implementation of [RemoteDocumentRepository].
 *
 * Documents are stored at `trips/{shareId}/documents/{documentId}`.
 */
class FirebaseStorageDocumentRepositoryImpl(
    private val context: Context,
    private val storage: FirebaseStorage,
    private val tripDocumentDao: TripDocumentDao,
) : RemoteDocumentRepository {

    override suspend fun uploadDocument(shareId: String, document: TripDocument): String {
        val storageRef = storage.reference
            .child("trips/$shareId/documents/${document.id}")

        val localUri = android.net.Uri.parse(document.uri)
        val uploadTask = storageRef.putFile(localUri).await()
        val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

        // Persist the remote URL locally.
        tripDocumentDao.updateRemoteUrl(document.id, downloadUrl)

        return downloadUrl
    }

    override suspend fun downloadDocument(remoteUrl: String, destinationName: String): String =
        withContext(Dispatchers.IO) {
            val storageRef = storage.getReferenceFromUrl(remoteUrl)
            val localFile = File(context.filesDir, destinationName)
            storageRef.getFile(localFile).await()
            localFile.absolutePath
        }

    override suspend fun deleteRemoteDocument(document: TripDocument) {
        val remoteUrl = document.remoteUrl ?: return
        storage.getReferenceFromUrl(remoteUrl).delete().await()
        tripDocumentDao.updateRemoteUrl(document.id, null)
    }
}
