package cat.company.wandervault.domain.model

import java.time.LocalDate

data class Trip(
    val id: Int,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val imageUri: String? = null,
)
