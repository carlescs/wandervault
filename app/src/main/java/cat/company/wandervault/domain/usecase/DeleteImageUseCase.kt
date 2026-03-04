package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.ImageRepository

class DeleteImageUseCase(private val repository: ImageRepository) {
    suspend operator fun invoke(fileUri: String) = repository.deleteFromInternalStorage(fileUri)
}
