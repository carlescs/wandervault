package cat.company.wandervault.ui.screens

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.usecase.GetCurrentUserUseCase
import cat.company.wandervault.domain.usecase.GetSignInIntentUseCase
import cat.company.wandervault.domain.usecase.HandleSignInResultUseCase
import cat.company.wandervault.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val getSignInIntent: GetSignInIntentUseCase,
    private val handleSignInResult: HandleSignInResultUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * One-shot event that emits the Google Sign-In [Intent] to be launched by the UI via
     * `rememberLauncherForActivityResult`.
     */
    private val _signInIntentEvent = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val signInIntentEvent: SharedFlow<Intent> = _signInIntentEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            getCurrentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun onSignInClick() {
        _signInIntentEvent.tryEmit(getSignInIntent())
    }

    fun onSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = handleSignInResult(data)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.localizedMessage,
                )
            }
        }
    }

    fun onSignOutClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { signOut() }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
