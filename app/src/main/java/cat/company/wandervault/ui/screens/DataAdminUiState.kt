package cat.company.wandervault.ui.screens

/**
 * Represents the UI state for the Data Administration screen.
 */
sealed class DataAdminUiState {
    /** No operation is in progress. */
    data object Idle : DataAdminUiState()

    /** A backup is being written. */
    data object BackupInProgress : DataAdminUiState()

    /** The backup completed successfully. */
    data object BackupSuccess : DataAdminUiState()

    /** The backup failed with an optional error [message]. */
    data class BackupError(val message: String?) : DataAdminUiState()

    /** A restore operation is in progress. */
    data object RestoreInProgress : DataAdminUiState()

    /**
     * The restore completed successfully.
     *
     * The app must be restarted for Room to pick up the replaced database.
     */
    data object RestoreSuccess : DataAdminUiState()

    /** The restore failed with an optional error [message]. */
    data class RestoreError(val message: String?) : DataAdminUiState()
}
