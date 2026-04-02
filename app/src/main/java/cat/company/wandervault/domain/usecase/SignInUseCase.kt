package cat.company.wandervault.domain.usecase

import android.content.Intent
import cat.company.wandervault.domain.model.User
import cat.company.wandervault.domain.repository.AuthRepository

/** Use-case that returns the Google Sign-In [Intent] to be launched by the UI. */
class GetSignInIntentUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Intent = repository.buildSignInIntent()
}

/** Use-case that completes sign-in after the Google Sign-In activity returns. */
class HandleSignInResultUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(data: Intent?): Result<User> = repository.handleSignInResult(data)
}
