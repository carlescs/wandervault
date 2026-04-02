package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.usecase.CreateTripInviteUseCase
import cat.company.wandervault.domain.usecase.GetCurrentUserUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.PushTripChangesUseCase
import cat.company.wandervault.domain.usecase.RemoveCollaboratorUseCase
import cat.company.wandervault.domain.usecase.ShareTripUseCase
import cat.company.wandervault.domain.usecase.UnshareTripUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Share Trip screen.
 *
 * @param tripId The ID of the trip to manage sharing for.
 */
class ShareTripViewModel(
    private val tripId: Int,
    private val getTrip: GetTripUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val shareTrip: ShareTripUseCase,
    private val unshareTrip: UnshareTripUseCase,
    private val createInvite: CreateTripInviteUseCase,
    private val removeCollaborator: RemoveCollaboratorUseCase,
    private val pushChanges: PushTripChangesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareTripUiState>(ShareTripUiState.Loading)
    val uiState: StateFlow<ShareTripUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(getTrip(tripId), getCurrentUser()) { trip, user ->
                trip to user
            }.collect { (trip, user) ->
                if (trip == null) {
                    _uiState.value = ShareTripUiState.Error("Trip not found.")
                    return@collect
                }
                val current = _uiState.value
                val inviteCode = (current as? ShareTripUiState.Success)?.inviteCode
                val isGeneratingInvite = (current as? ShareTripUiState.Success)?.isGeneratingInvite ?: false
                val isSyncing = (current as? ShareTripUiState.Success)?.isSyncing ?: false
                _uiState.value = ShareTripUiState.Success(
                    tripTitle = trip.title,
                    shareId = trip.shareId,
                    ownerId = trip.ownerId,
                    currentUserUid = user?.uid,
                    collaboratorIds = trip.collaboratorIds,
                    inviteCode = inviteCode,
                    isGeneratingInvite = isGeneratingInvite,
                    isSyncing = isSyncing,
                )
            }
        }
    }

    fun onShareToggle() {
        val state = _uiState.value as? ShareTripUiState.Success ?: return
        if (state.isShared) {
            onUnshareClick()
        } else {
            onShareClick()
        }
    }

    fun onShareClick() {
        viewModelScope.launch {
            setSuccessFlag { copy(isSyncing = true, error = null) }
            try {
                shareTrip(tripId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setSuccessFlag { copy(isSyncing = false, error = e.localizedMessage) }
                return@launch
            }
            setSuccessFlag { copy(isSyncing = false) }
        }
    }

    fun onUnshareClick() {
        viewModelScope.launch {
            setSuccessFlag { copy(isSyncing = true, error = null) }
            try {
                unshareTrip(tripId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setSuccessFlag { copy(isSyncing = false, error = e.localizedMessage) }
                return@launch
            }
            setSuccessFlag { copy(isSyncing = false, inviteCode = null) }
        }
    }

    fun onGenerateInviteClick() {
        val state = _uiState.value as? ShareTripUiState.Success ?: return
        val shareId = state.shareId ?: return
        viewModelScope.launch {
            setSuccessFlag { copy(isGeneratingInvite = true, error = null) }
            val code = try {
                createInvite(shareId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setSuccessFlag { copy(isGeneratingInvite = false, error = e.localizedMessage) }
                return@launch
            }
            setSuccessFlag { copy(isGeneratingInvite = false, inviteCode = code) }
        }
    }

    fun onRemoveCollaboratorClick(collaboratorUid: String) {
        val state = _uiState.value as? ShareTripUiState.Success ?: return
        val shareId = state.shareId ?: return
        viewModelScope.launch {
            runCatching { removeCollaborator(shareId, collaboratorUid) }
        }
    }

    fun onSyncClick() {
        viewModelScope.launch {
            setSuccessFlag { copy(isSyncing = true, error = null) }
            try {
                pushChanges(tripId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setSuccessFlag { copy(isSyncing = false, error = e.localizedMessage) }
                return@launch
            }
            setSuccessFlag { copy(isSyncing = false) }
        }
    }

    private inline fun setSuccessFlag(update: ShareTripUiState.Success.() -> ShareTripUiState.Success) {
        _uiState.update { state ->
            (state as? ShareTripUiState.Success)?.update() ?: state
        }
    }
}
