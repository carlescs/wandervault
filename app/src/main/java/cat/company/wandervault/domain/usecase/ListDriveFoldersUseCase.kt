package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.DriveFolder
import cat.company.wandervault.domain.repository.GoogleDriveRepository

/** Use-case that lists the top-level Drive folders accessible to the signed-in user. */
class ListDriveFoldersUseCase(private val repository: GoogleDriveRepository) {
    suspend operator fun invoke(): Result<List<DriveFolder>> = repository.listFolders()
}
