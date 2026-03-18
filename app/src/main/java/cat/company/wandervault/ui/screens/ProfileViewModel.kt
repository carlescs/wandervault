package cat.company.wandervault.ui.screens

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.data.remote.google.DriveSignInClient
import cat.company.wandervault.data.remote.google.SignInCancelledException
import cat.company.wandervault.domain.model.DriveFolder
import cat.company.wandervault.domain.usecase.GetDriveSignInStatusUseCase
import cat.company.wandervault.domain.usecase.GetSelectedDriveFolderUseCase
import cat.company.wandervault.domain.usecase.ListDriveFoldersUseCase
import cat.company.wandervault.domain.usecase.SetSelectedDriveFolderUseCase
import cat.company.wandervault.domain.usecase.SignInToDriveUseCase
import cat.company.wandervault.domain.usecase.SignOutFromDriveUseCase
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Profile screen.
 *
 * Manages Google Drive sign-in status, folder selection, and exposes a
 * [ProfileUiState] for the UI.
 *
 * The sign-in flow works in two phases:
 * 1. [onSignIn] attempts a silent (no-UI) sign-in via [SignInToDriveUseCase].  On success
 *    the state is updated immediately.  On failure a sign-in [Intent] is emitted via
 *    [signInIntentEvent] so the screen can launch the Google Sign-In activity.
 * 2. When the activity result arrives the screen calls [onSignInResult] which processes
 *    the returned [Intent] via [DriveSignInClient] and updates the state.
 */
class ProfileViewModel(
    private val getDriveSignInStatus: GetDriveSignInStatusUseCase,
    private val signInToDrive: SignInToDriveUseCase,
    private val driveSignInClient: DriveSignInClient,
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
     *
     * The screen should collect this flow and start the activity with
     * [ActivityResultLauncher.launch], then pass the result data to [onSignInResult].
     * With `extraBufferCapacity = 1` and [BufferOverflow.DROP_OLDEST], [tryEmit] is
     * guaranteed to succeed without suspending.
     */
    private val _signInIntentEvent = MutableSharedFlow<Intent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val signInIntentEvent: Flow<Intent> = _signInIntentEvent.asSharedFlow()

    /**
     * Initiates the Google Drive sign-in flow.
     *
     * First attempts a silent (no-UI) sign-in.  If that fails the sign-in [Intent]
     * is emitted via [signInIntentEvent] for the screen to launch the interactive flow.
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
                    // Silent sign-in failed – emit the intent so the screen can launch
                    // the interactive Google Sign-In activity.
                    _signInIntentEvent.tryEmit(driveSignInClient.buildSignInIntent())
                    // Keep isSigningIn = true while waiting for the activity result.
                }
        }
    }

    /**
     * Handles the [Intent] returned from the Google Sign-In activity.
     *
     * Passing `null` for [data] is treated as a cancellation (identical to
     * [onSignInCancelled]). A [SignInCancelledException] returned by
     * [DriveSignInClient.handleSignInResult] is also treated as a silent cancellation
     * so no error dialog is shown to the user.
     *
     * @param data The result intent from the sign-in activity, or `null` if cancelled.
     */
    fun onSignInResult(data: Intent?) {
        viewModelScope.launch {
            driveSignInClient.handleSignInResult(data)
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
                    if (e is SignInCancelledException) {
                        // User cancelled – reset loading state without showing an error.
                        _uiState.update { it.copy(isSigningIn = false) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSigningIn = false,
                                driveError = e.message ?: "Sign-in failed",
                            )
                        }
                    }
                }
        }
    }

    /**
     * Called when the user dismisses the Google Sign-In chooser without selecting an
     * account (e.g. presses Back).  Clears the signing-in state without showing an error.
     */
    fun onSignInCancelled() {
        _uiState.update { it.copy(isSigningIn = false) }
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
