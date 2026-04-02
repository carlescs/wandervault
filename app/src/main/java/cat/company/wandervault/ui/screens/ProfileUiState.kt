package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.User

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
