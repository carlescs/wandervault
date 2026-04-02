package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.User
import cat.company.wandervault.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/** Use-case that returns a [Flow] emitting the currently signed-in [User], or `null`. */
class GetCurrentUserUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Flow<User?> = repository.currentUser()
}
