package cat.company.wandervault.domain.usecase

import android.content.Intent
import cat.company.wandervault.domain.repository.GoogleDriveRepository

/** Use-case that returns the [Intent] needed to start the interactive Google Sign-In flow. */
class BuildSignInIntentUseCase(private val repository: GoogleDriveRepository) {
    operator fun invoke(): Intent = repository.buildSignInIntent()
}
