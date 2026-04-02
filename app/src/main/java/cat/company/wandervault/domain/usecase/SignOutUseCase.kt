package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.AuthRepository

/** Use-case that signs the current user out of Firebase. */
class SignOutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signOut()
}
