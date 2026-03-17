package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.DriveFolder
import cat.company.wandervault.domain.repository.GoogleDriveRepository

/** Use-case that persists the user's chosen Drive upload folder. */
class SetSelectedDriveFolderUseCase(private val repository: GoogleDriveRepository) {
    operator fun invoke(folder: DriveFolder?) =
        repository.setSelectedFolder(folder?.id, folder?.name)
}
