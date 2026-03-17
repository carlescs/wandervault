package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.DriveFolder
import cat.company.wandervault.domain.repository.GoogleDriveRepository

/**
 * Use-case that returns the Drive folder currently selected as the upload destination,
 * or `null` if none has been chosen.
 */
class GetSelectedDriveFolderUseCase(private val repository: GoogleDriveRepository) {
    operator fun invoke(): DriveFolder? {
        val id = repository.getSelectedFolderId() ?: return null
        val name = repository.getSelectedFolderName() ?: return null
        return DriveFolder(id = id, name = name)
    }
}
