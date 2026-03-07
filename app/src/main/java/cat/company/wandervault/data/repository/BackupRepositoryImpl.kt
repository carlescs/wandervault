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
 * Backup: the WAL is checkpointed, then the main SQLite file, the WAL file (if non-empty
 * after the checkpoint, i.e. the checkpoint was partial due to active readers), and every
 * file inside `filesDir/images/` are written into a single zip archive at the
 * caller-supplied URI.  Including the WAL ensures that committed transactions that have
 * not yet been flushed to the main file are not silently dropped from the backup.
 *
 * Restore: the zip is fully extracted to a temporary staging directory first. Only once
 * extraction succeeds (and the DB entry is confirmed present) is the Room database closed
 * and the staged files moved into place. This ensures the database is never closed for a
 * bad or partial archive. The caller must restart the application immediately after a
 * successful restore so Room re-initialises with the replaced database.
 */
class BackupRepositoryImpl(
    private val context: Context,
    private val database: WanderVaultDatabase,
) : BackupRepository {

    override suspend fun createBackup(outputUri: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Flush WAL so the main database file is as up-to-date as possible.
            // wal_checkpoint(TRUNCATE) only truncates the WAL when no readers are active;
            // if the checkpoint is partial, the WAL file is kept and included in the
            // archive below so that no committed data is silently dropped.
            database.checkpoint()

            val databaseFile = context.getDatabasePath(WanderVaultDatabase.DATABASE_NAME)

            if (!databaseFile.exists()) {
                return@withContext Result.failure(IOException("Database file not found."))
            }

            val databaseWalFile = File("${databaseFile.path}-wal")
            val imagesDir = File(context.filesDir, "images")
            val uri = Uri.parse(outputUri)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zip ->
                    zip.putNextEntry(ZipEntry(WanderVaultDatabase.DATABASE_NAME))
                    databaseFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()

                    // Include the WAL file when it is non-empty (partial checkpoint).
                    if (databaseWalFile.exists() && databaseWalFile.length() > 0) {
                        zip.putNextEntry(ZipEntry("${WanderVaultDatabase.DATABASE_NAME}-wal"))
                        databaseWalFile.inputStream().use { it.copyTo(zip) }
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
        // Stage directory used for extraction – cleaned up on any failure.
        val stageDir = File(context.cacheDir, "restore_stage")
        try {
            stageDir.deleteRecursively()
            stageDir.mkdirs()

            val uri = Uri.parse(inputUri)
            var dbStagedFile: File? = null

            // Phase 1: extract the entire archive to the staging directory.
            // The database is NOT closed yet so the app remains usable if validation fails.
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        // Guard against zip-slip: reject any path containing traversal.
                        if (!name.contains("..") && !name.startsWith("/")) {
                            when {
                                name == WanderVaultDatabase.DATABASE_NAME -> {
                                    val dest = File(stageDir, WanderVaultDatabase.DATABASE_NAME)
                                    dest.outputStream().use { zip.copyTo(it) }
                                    dbStagedFile = dest
                                }
                                name == "${WanderVaultDatabase.DATABASE_NAME}-wal" -> {
                                    val dest = File(stageDir, "${WanderVaultDatabase.DATABASE_NAME}-wal")
                                    dest.outputStream().use { zip.copyTo(it) }
                                }
                                name.startsWith("images/") -> {
                                    val fileName = name.removePrefix("images/")
                                    if (fileName.isNotEmpty() && !fileName.contains('/')) {
                                        val imagesStage = File(stageDir, "images")
                                        imagesStage.mkdirs()
                                        File(imagesStage, fileName).outputStream()
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

            // Phase 2: validate that the archive contained the database file.
            val validatedDbFile = dbStagedFile?.takeIf { it.exists() }
                ?: return@withContext Result.failure(
                    IOException("Backup archive does not contain a valid database file."),
                )

            // Phase 3: all validation passed – now close Room and swap files into place.
            val databaseFile = context.getDatabasePath(WanderVaultDatabase.DATABASE_NAME)
            val databaseWalFile = File("${databaseFile.path}-wal")
            val databaseShmFile = File("${databaseFile.path}-shm")
            val imagesDir = File(context.filesDir, "images")

            database.close()

            // Replace database (delete WAL/SHM so Room starts clean, then lay down the
            // restored main file and, if the backup included a WAL, the restored WAL too).
            databaseFile.parentFile?.mkdirs()
            databaseWalFile.delete()
            databaseShmFile.delete()
            validatedDbFile.copyTo(databaseFile, overwrite = true)
            val walStageFile = File(stageDir, "${WanderVaultDatabase.DATABASE_NAME}-wal")
            if (walStageFile.exists()) {
                walStageFile.copyTo(databaseWalFile, overwrite = true)
            }

            // Replace images: clear existing files first to remove any orphaned images,
            // then copy in only the files that are part of the backup snapshot.
            imagesDir.deleteRecursively()
            val imagesStage = File(stageDir, "images")
            if (imagesStage.exists()) {
                imagesStage.copyRecursively(imagesDir, overwrite = true)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            stageDir.deleteRecursively()
        }
    }
}
