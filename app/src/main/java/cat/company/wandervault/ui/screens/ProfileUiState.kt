package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.DriveFolder

/**
 * UI state for the Profile screen.
 *
 * @param isSignedInToDrive Whether the user is currently signed in to Google Drive.
 * @param selectedDriveFolder The Drive folder chosen as the upload destination, or `null`.
 * @param isFolderPickerOpen Whether the Drive folder picker dialog is currently visible.
 * @param availableDriveFolders Folders loaded from Drive for the picker dialog.
 * @param isLoadingFolders Whether a folder-list request is in progress.
 * @param driveError A user-visible error message from the last Drive operation, or `null`.
 * @param isSigningIn Whether a sign-in operation is currently in progress.
 */
data class ProfileUiState(
    val isSignedInToDrive: Boolean = false,
    val selectedDriveFolder: DriveFolder? = null,
    val isFolderPickerOpen: Boolean = false,
    val availableDriveFolders: List<DriveFolder> = emptyList(),
    val isLoadingFolders: Boolean = false,
    val driveError: String? = null,
    val isSigningIn: Boolean = false,
)
