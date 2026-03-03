package cat.company.wandervault.domain.model

data class Trip(
    val id: Int,
    val title: String,
    val imageUri: String? = null,
)
