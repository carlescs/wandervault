package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.usecase.AcceptTripInviteUseCase
import cat.company.wandervault.domain.usecase.JoinTripUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JoinTripViewModel(
    private val acceptInvite: AcceptTripInviteUseCase,
    private val joinTrip: JoinTripUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinTripUiState())
    val uiState: StateFlow<JoinTripUiState> = _uiState.asStateFlow()

    fun onInviteCodeChanged(code: String) {
        _uiState.update { it.copy(inviteCode = code.uppercase().take(6), error = null) }
    }

    fun onJoinClick() {
        val code = _uiState.value.inviteCode
        if (!_uiState.value.isCodeValid) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val shareId = acceptInvite(code)
                val trip = joinTrip(shareId)
                _uiState.update { it.copy(isLoading = false, joinedTripId = trip.id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}
