package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.GoogleDriveRepository

/**
 * Use-case that uploads a local file to the user's selected Google Drive folder.
 *
 * The [remotePath] list mirrors the local trip/folder hierarchy so the Drive structure
 * matches the on-device layout (e.g. `["My Trip 2025", "Flights"]`).
 *
 * @return [Result.success] with the Drive file ID, or [Result.failure] if the upload
 *   failed or the user is not signed in / has no folder selected.
 */
class UploadDocumentToDriveUseCase(private val repository: GoogleDriveRepository) {
    suspend operator fun invoke(
        localUri: String,
        mimeType: String,
        fileName: String,
        remotePath: List<String>,
    ): Result<String> {
        if (!repository.isSignedIn()) {
            return Result.failure(IllegalStateException("Not signed in to Google Drive"))
        }
        if (repository.getSelectedFolderId() == null) {
            return Result.failure(IllegalStateException("No Drive folder selected"))
        }
        return repository.uploadFile(
            localUri = localUri,
            mimeType = mimeType,
            fileName = fileName,
            remotePath = remotePath,
        )
    }
}
