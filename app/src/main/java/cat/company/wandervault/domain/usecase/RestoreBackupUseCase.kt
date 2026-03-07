package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.BackupRepository

/**
 * Use-case that restores all app data from a zip backup at [inputUri].
 */
class RestoreBackupUseCase(private val repository: BackupRepository) {
    suspend operator fun invoke(inputUri: String): Result<Unit> =
        repository.restoreBackup(inputUri)
}
