package cat.company.wandervault.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import cat.company.wandervault.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

class ImageRepositoryImpl(private val context: Context) : ImageRepository {
    override suspend fun copyToInternalStorage(sourceUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(sourceUri)
            val mimeType = context.contentResolver.getType(uri)
            val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "jpg"
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext null
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists() && !imagesDir.mkdirs()) return@withContext null
            val file = File(imagesDir, "${UUID.randomUUID()}.$extension")
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

    override suspend fun deleteFromInternalStorage(fileUri: String) = withContext(Dispatchers.IO) {
        try {
            val path = Uri.parse(fileUri).path ?: return@withContext
            val file = File(path)
            val internalImagesDir = File(context.filesDir, "images").canonicalPath
            if (file.canonicalPath.startsWith(internalImagesDir)) {
                file.delete()
            }
        } catch (e: IOException) {
            // Ignore deletion failures
        }
    }
}
