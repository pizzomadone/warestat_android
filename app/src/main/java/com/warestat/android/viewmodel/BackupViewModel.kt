package com.warestat.android.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warestat.android.util.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BackupState(
    val backupFiles: List<File> = emptyList(),
    val autoBackupEnabled: Boolean = true,
    val retentionDays: Int = 7,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state.asStateFlow()

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            val autoBackup = backupManager.isAutoBackupEnabled()
            val retention = backupManager.getRetentionDays()
            val files = backupManager.listBackups()
            _state.update { it.copy(autoBackupEnabled = autoBackup, retentionDays = retention, backupFiles = files) }
        }
    }

    fun performBackup() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = backupManager.performBackup()
            result.fold(
                onSuccess = { path ->
                    _state.update { it.copy(
                        isLoading = false,
                        successMessage = "Backup completato: $path",
                        backupFiles = backupManager.listBackups()
                    ) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = backupManager.restoreBackup(uri)
            result.fold(
                onSuccess = { _state.update { it.copy(isLoading = false, successMessage = "Ripristino completato. Riavvia l'app.") } },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            backupManager.setAutoBackupEnabled(enabled)
            _state.update { it.copy(autoBackupEnabled = enabled) }
        }
    }

    fun setRetentionDays(days: Int) {
        viewModelScope.launch {
            backupManager.setRetentionDays(days)
            _state.update { it.copy(retentionDays = days) }
        }
    }

    fun clearMessages() = _state.update { it.copy(successMessage = null, error = null) }
}
