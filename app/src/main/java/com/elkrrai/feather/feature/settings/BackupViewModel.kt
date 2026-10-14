package com.elkrrai.feather.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.data.backup.BackupFiles
import com.elkrrai.feather.domain.backup.BackupFormatException
import com.elkrrai.feather.domain.backup.BackupRepository
import com.elkrrai.feather.domain.backup.ImportMode
import com.elkrrai.feather.domain.backup.ImportSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What to tell the user after a backup action. The screen turns it into a message. */
sealed interface BackupResult {
    data object Exported : BackupResult
    data object CsvExported : BackupResult
    data class Imported(val summary: ImportSummary) : BackupResult
    data object NotABackup : BackupResult
    data object TooNew : BackupResult
    data object Failed : BackupResult
}

data class BackupUiState(
    val busy: Boolean = false,
    val result: BackupResult? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backup: BackupRepository,
    private val files: BackupFiles,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun exportTo(uri: Uri) = launchWrite(BackupResult.Exported) {
        files.write(uri, backup.exportJson())
    }

    fun exportCsvTo(uri: Uri) = launchWrite(BackupResult.CsvExported) {
        files.write(uri, backup.exportCsv())
    }

    fun importFrom(uri: Uri, mode: ImportMode) {
        launchAction {
            val summary = backup.importJson(files.read(uri), mode)
            BackupResult.Imported(summary)
        }
    }

    /** Called once the message has been shown. */
    fun resultShown() = _state.update { it.copy(result = null) }

    private fun launchWrite(success: BackupResult, action: suspend () -> Unit) = launchAction {
        action()
        success
    }

    private fun launchAction(action: suspend () -> BackupResult) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, result = null) }
        viewModelScope.launch {
            val result = try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupFormatException) {
                if (e.reason == BackupFormatException.Reason.TOO_NEW) BackupResult.TooNew else BackupResult.NotABackup
            } catch (e: Exception) {
                BackupResult.Failed
            }
            _state.update { it.copy(busy = false, result = result) }
        }
    }
}
