package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.BackupRepository

/**
 * Use-case that creates a zip backup of all app data and writes it to [outputUri].
 */
class CreateBackupUseCase(private val repository: BackupRepository) {
    suspend operator fun invoke(outputUri: String): Result<Unit> =
        repository.createBackup(outputUri)
}
