package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.ImageRepository

class CopyImageToInternalStorageUseCase(private val repository: ImageRepository) {
    suspend operator fun invoke(sourceUri: String): String? = repository.copyToInternalStorage(sourceUri)
}
