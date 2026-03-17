package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.GoogleDriveRepository

/** Use-case that initiates the Google Sign-In / OAuth flow for Drive access. */
class SignInToDriveUseCase(private val repository: GoogleDriveRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.signIn()
}
