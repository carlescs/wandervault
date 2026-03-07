package cat.company.wandervault.domain.repository

/**
 * Repository for creating and restoring data backups.
 */
interface BackupRepository {
    /**
     * Creates a zip backup of all app data (database + images) and writes it
     * to the URI given by [outputUri].
     */
    suspend fun createBackup(outputUri: String): Result<Unit>

    /**
     * Restores app data from the zip backup at [inputUri], overwriting the
     * current database and image files.
     *
     * The caller is responsible for restarting the app after a successful
     * restore so that Room re-opens the replaced database.
     */
    suspend fun restoreBackup(inputUri: String): Result<Unit>
}
