package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.DriveFolder
import cat.company.wandervault.domain.usecase.GetDriveSignInStatusUseCase
import cat.company.wandervault.domain.usecase.GetSelectedDriveFolderUseCase
import cat.company.wandervault.domain.usecase.ListDriveFoldersUseCase
import cat.company.wandervault.domain.usecase.SetSelectedDriveFolderUseCase
import cat.company.wandervault.domain.usecase.SignInToDriveUseCase
import cat.company.wandervault.domain.usecase.SignOutFromDriveUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Profile screen.
 *
 * Manages Google Drive sign-in status, folder selection, and exposes a
 * [ProfileUiState] for the UI.
 */
class ProfileViewModel(
    private val getDriveSignInStatus: GetDriveSignInStatusUseCase,
    private val signInToDrive: SignInToDriveUseCase,
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

    /** Initiates the Google Drive sign-in flow. */
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
