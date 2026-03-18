package cat.company.wandervault.ui.screens

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.DriveFolder
import cat.company.wandervault.domain.usecase.BuildSignInIntentUseCase
import cat.company.wandervault.domain.usecase.GetDriveSignInStatusUseCase
import cat.company.wandervault.domain.usecase.GetSelectedDriveFolderUseCase
import cat.company.wandervault.domain.usecase.HandleSignInResultUseCase
import cat.company.wandervault.domain.usecase.ListDriveFoldersUseCase
import cat.company.wandervault.domain.usecase.SetSelectedDriveFolderUseCase
import cat.company.wandervault.domain.usecase.SignInToDriveUseCase
import cat.company.wandervault.domain.usecase.SignOutFromDriveUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Profile screen.
 *
 * Manages Google Drive sign-in status, folder selection, and exposes a
 * [ProfileUiState] for the UI.
 *
 * The sign-in flow works in two phases:
 * 1. [onSignIn] attempts a silent (no-UI) sign-in.  On success the state is updated
 *    immediately.  On failure a sign-in [Intent] is emitted via [signInIntentEvent]
 *    so the screen can launch the Google Sign-In activity.
 * 2. When the activity result arrives the screen calls [onSignInResult] which
 *    processes the returned [Intent] and updates the state.
 */
class ProfileViewModel(
    private val getDriveSignInStatus: GetDriveSignInStatusUseCase,
    private val signInToDrive: SignInToDriveUseCase,
    private val buildSignInIntent: BuildSignInIntentUseCase,
    private val handleSignInResult: HandleSignInResultUseCase,
    private val signOutFromDrive: SignOutFromDriveUseCase,
    private val getSelectedDriveFolder: GetSelectedDriveFolderUseCase,
    private val setSelectedDriveFolder: SetSelectedDriveFolderUseCase,
    private val listDriveFolders: ListDriveFoldersUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            isSignedInToDrive = getDriveSignInStatus(),
            selectedDriveFolder = getSelectedDriveFolder(),
        ),
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Emits an [Intent] when an interactive Google Sign-In activity needs to be launched.
     * The screen should collect this flow and start the activity with
     * [ActivityResultLauncher.launch], then pass the result data to [onSignInResult].
     */
    private val _signInIntentEvent = Channel<Intent>(Channel.BUFFERED)
    val signInIntentEvent: Flow<Intent> = _signInIntentEvent.receiveAsFlow()

    /**
     * Initiates the Google Drive sign-in flow.
     *
     * First attempts a silent (no-UI) sign-in.  If that fails the sign-in [Intent]
     * is sent via [signInIntentEvent] for the screen to launch the interactive flow.
     */
    fun onSignIn() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true, driveError = null) }
            signInToDrive()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            isSignedInToDrive = true,
                            selectedDriveFolder = getSelectedDriveFolder(),
                        )
                    }
                }
                .onFailure {
                    // Silent sign-in failed – launch the interactive Google Sign-In activity.
                    _signInIntentEvent.send(buildSignInIntent())
                    // Keep isSigningIn = true while waiting for the activity result.
                }
        }
    }

    /**
     * Handles the [Intent] returned from the Google Sign-In activity.
     *
     * @param data The result intent, or `null` if the user cancelled.
     */
    fun onSignInResult(data: Intent?) {
        viewModelScope.launch {
            handleSignInResult(data)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            isSignedInToDrive = true,
                            selectedDriveFolder = getSelectedDriveFolder(),
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            driveError = e.message ?: "Sign-in failed",
                        )
                    }
                }
        }
    }

    /** Signs the user out of Google Drive and clears the selected folder. */
    fun onSignOut() {
        viewModelScope.launch {
            signOutFromDrive()
            _uiState.update {
                it.copy(
                    isSignedInToDrive = false,
                    selectedDriveFolder = null,
                    availableDriveFolders = emptyList(),
                    driveError = null,
                )
            }
        }
    }

    /**
     * Loads available Drive folders so the user can pick one.
     * Sets [ProfileUiState.isFolderPickerOpen] to `true` so the dialog is shown even
     * when Drive returns an empty folder list.
     */
    fun onOpenFolderPicker() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFolderPickerOpen = true, isLoadingFolders = true, driveError = null) }
            listDriveFolders()
                .onSuccess { folders ->
                    _uiState.update {
                        it.copy(isLoadingFolders = false, availableDriveFolders = folders)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingFolders = false,
                            // Keep isFolderPickerOpen = true so the dialog stays visible;
                            // the error is shown via driveError on top and the user can
                            // dismiss it and then cancel the picker themselves.
                            driveError = e.message ?: "Failed to load folders",
                        )
                    }
                }
        }
    }

    /** Persists [folder] as the Drive upload destination. */
    fun onFolderSelected(folder: DriveFolder) {
        setSelectedDriveFolder(folder)
        _uiState.update {
            it.copy(
                selectedDriveFolder = folder,
                isFolderPickerOpen = false,
                availableDriveFolders = emptyList(),
            )
        }
    }

    /** Clears the Drive folder selection. */
    fun onClearFolderSelection() {
        setSelectedDriveFolder(null)
        _uiState.update { it.copy(selectedDriveFolder = null) }
    }

    /** Dismisses any current error message. */
    fun onDismissError() {
        _uiState.update { it.copy(driveError = null) }
    }

    /** Dismisses the folder picker dialog without selecting a folder. */
    fun onDismissFolderPicker() {
        _uiState.update {
            it.copy(
                isFolderPickerOpen = false,
                availableDriveFolders = emptyList(),
            )
        }
    }
}
