package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.GoogleDriveRepository

/** Use-case that signs the user out of Google Drive and clears stored credentials. */
class SignOutFromDriveUseCase(private val repository: GoogleDriveRepository) {
    suspend operator fun invoke() = repository.signOut()
}
