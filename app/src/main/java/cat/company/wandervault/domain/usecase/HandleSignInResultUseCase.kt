package cat.company.wandervault.domain.usecase

import android.content.Intent
import cat.company.wandervault.domain.repository.GoogleDriveRepository

/**
 * Use-case that processes the [Intent] returned from the Google Sign-In activity.
 *
 * @return [Result.success] on successful sign-in, [Result.failure] otherwise.
 */
class HandleSignInResultUseCase(private val repository: GoogleDriveRepository) {
    suspend operator fun invoke(data: Intent?): Result<Unit> = repository.handleSignInResult(data)
}
