package cat.company.wandervault.ui.screens

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.usecase.CreateBackupUseCase
import cat.company.wandervault.domain.usecase.RestoreBackupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

/**
 * ViewModel for the Data Administration screen.
 *
 * Drives backup and restore operations and exposes a [DataAdminUiState] for the UI.
 *
 * @param application Required to trigger an app restart after a successful restore.
 * @param createBackup Use-case that writes a zip backup to the given URI.
 * @param restoreBackup Use-case that restores app data from a zip backup URI.
 */
class DataAdminViewModel(
    private val application: Application,
    private val createBackup: CreateBackupUseCase,
    private val restoreBackup: RestoreBackupUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DataAdminUiState>(DataAdminUiState.Idle)
    val uiState: StateFlow<DataAdminUiState> = _uiState.asStateFlow()

    /** Called once the user has chosen an output URI for the backup file. */
    fun onBackup(outputUri: String) {
        viewModelScope.launch {
            _uiState.value = DataAdminUiState.BackupInProgress
            val result = createBackup(outputUri)
            _uiState.value = if (result.isSuccess) {
                DataAdminUiState.BackupSuccess
            } else {
                DataAdminUiState.BackupError(result.exceptionOrNull()?.message)
            }
        }
    }

    /** Called once the user has chosen a zip file to restore from. */
    fun onRestore(inputUri: String) {
        viewModelScope.launch {
            _uiState.value = DataAdminUiState.RestoreInProgress
            val result = restoreBackup(inputUri)
            _uiState.value = if (result.isSuccess) {
                DataAdminUiState.RestoreSuccess
            } else {
                DataAdminUiState.RestoreError(result.exceptionOrNull()?.message)
            }
        }
    }

    /** Dismisses any success/error message and returns to [DataAdminUiState.Idle]. */
    fun onDismissMessage() {
        _uiState.value = DataAdminUiState.Idle
    }

    /**
     * Restarts the app so that Room re-opens the database that was just restored.
     *
     * Should only be called after [DataAdminUiState.RestoreSuccess].
     *
     * Launching a clear-task intent followed by [exitProcess] is the standard Android
     * pattern for an immediate, reliable app restart. The process is killed after the
     * new activity task is created, allowing Android to complete its own lifecycle
     * callbacks on the new task.
     */
    fun restartApp() {
        val intent = application.packageManager
            .getLaunchIntentForPackage(application.packageName)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
        exitProcess(0)
    }
}
