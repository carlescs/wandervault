package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.GoogleDriveRepository

/** Use-case that returns whether the user is currently signed in to Google Drive. */
class GetDriveSignInStatusUseCase(private val repository: GoogleDriveRepository) {
    operator fun invoke(): Boolean = repository.isSignedIn()
}
