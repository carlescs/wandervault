package cat.company.wandervault.data.repository

import android.content.Context
import android.net.Uri
import cat.company.wandervault.data.local.WanderVaultDatabase
import cat.company.wandervault.data.local.WanderVaultDatabase.Companion.checkpoint
import cat.company.wandervault.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backs up and restores all app data (Room database + internal images) as a zip file.
 *
 * Backup: the WAL is checkpointed, then the main SQLite file and every file inside
 * `filesDir/images/` are written into a single zip archive at the caller-supplied URI.
 *
 * Restore: the Room database is closed, the zip is extracted over the existing database
 * and image files, and the caller is expected to restart the process so Room
 * re-initialises with the restored data.
 *
 * **Note:** `restoreBackup` closes the Room database instance so that the underlying
 * SQLite file can be replaced safely.  Callers must not start any new database
 * operations after calling `restoreBackup`, and should restart the application
 * process immediately after a successful restore.
 */
class BackupRepositoryImpl(
    private val context: Context,
    private val database: WanderVaultDatabase,
) : BackupRepository {

    override suspend fun createBackup(outputUri: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Flush WAL so the main database file is fully up-to-date.
            database.checkpoint()

            val databaseFile = context.getDatabasePath(WanderVaultDatabase.DATABASE_NAME)
            val imagesDir = File(context.filesDir, "images")
            val uri = Uri.parse(outputUri)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zip ->
                    if (databaseFile.exists()) {
                        zip.putNextEntry(ZipEntry(WanderVaultDatabase.DATABASE_NAME))
                        databaseFile.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                    if (imagesDir.exists()) {
                        imagesDir.walkTopDown().filter { it.isFile }.forEach { file ->
                            zip.putNextEntry(ZipEntry("images/${file.name}"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            } ?: return@withContext Result.failure(IOException("Cannot open output stream for $outputUri"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreBackup(inputUri: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val databaseFile = context.getDatabasePath(WanderVaultDatabase.DATABASE_NAME)
            val databaseWalFile = File("${databaseFile.path}-wal")
            val databaseShmFile = File("${databaseFile.path}-shm")
            val imagesDir = File(context.filesDir, "images")

            // Close Room so the database files are not locked.
            database.close()

            val uri = Uri.parse(inputUri)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        // Guard against zip-slip: reject any path containing traversal.
                        if (!name.contains("..") && !name.startsWith("/")) {
                            when {
                                name == WanderVaultDatabase.DATABASE_NAME -> {
                                    databaseFile.parentFile?.mkdirs()
                                    databaseWalFile.delete()
                                    databaseShmFile.delete()
                                    databaseFile.outputStream().use { zip.copyTo(it) }
                                }
                                name.startsWith("images/") -> {
                                    val fileName = name.removePrefix("images/")
                                    if (fileName.isNotEmpty() && !fileName.contains('/')) {
                                        imagesDir.mkdirs()
                                        File(imagesDir, fileName).outputStream()
                                            .use { zip.copyTo(it) }
                                    }
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return@withContext Result.failure(IOException("Cannot open input stream for $inputUri"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
