package cat.company.wandervault.ui.screens

data class JoinTripUiState(
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val joinedTripId: Int? = null,
) {
    val isCodeValid: Boolean get() = inviteCode.length == 6
}
