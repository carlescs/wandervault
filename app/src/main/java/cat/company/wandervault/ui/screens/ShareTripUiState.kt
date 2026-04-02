package cat.company.wandervault.ui.screens

sealed class ShareTripUiState {
    data object Loading : ShareTripUiState()
    data class Success(
        val tripTitle: String,
        val shareId: String?,
        val ownerId: String?,
        val currentUserUid: String?,
        val collaboratorIds: List<String>,
        val inviteCode: String? = null,
        val isGeneratingInvite: Boolean = false,
        val isSyncing: Boolean = false,
        val error: String? = null,
    ) : ShareTripUiState() {
        /** True if the trip is currently shared. */
        val isShared: Boolean get() = shareId != null
        /** True if the current user is the owner of the shared trip. */
        val isOwner: Boolean get() = currentUserUid != null && currentUserUid == ownerId
    }
    data class Error(val message: String) : ShareTripUiState()
}
