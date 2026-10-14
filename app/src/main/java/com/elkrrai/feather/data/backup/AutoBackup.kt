package com.elkrrai.feather.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.elkrrai.feather.domain.backup.BackupRepository
import com.elkrrai.feather.domain.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AutoBackupResult {
    DONE,

    /** No folder has been chosen. */
    NO_FOLDER,

    /** The folder is gone or the permission to write to it was taken away. */
    NO_ACCESS,

    FAILED,
}

/**
 * Writes one backup file per day into the folder the user chose and keeps only the newest
 * [KEEP] of them. The files have their own name prefix, so manual exports saved in the same
 * folder are never replaced or removed.
 */
@Singleton
class AutoBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backup: BackupRepository,
    private val settings: SettingsRepository,
    private val files: BackupFiles,
    private val clock: Clock,
) {
    suspend fun runNow(): AutoBackupResult {
        val folderUri = settings.current().backupFolder ?: return AutoBackupResult.NO_FOLDER
        return try {
            val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri))
            if (folder == null || !folder.exists() || !folder.canWrite()) return AutoBackupResult.NO_ACCESS

            val text = backup.exportJson()
            val name = fileName(LocalDate.now())
            val file = withContext(Dispatchers.IO) {
                // A second backup on the same day replaces the first.
                folder.findFile(name)?.delete()
                folder.createFile(MIME_TYPE, name)
            } ?: return AutoBackupResult.FAILED
            files.write(file.uri, text)

            withContext(Dispatchers.IO) { prune(folder) }
            settings.setLastBackupAt(clock.millis())
            AutoBackupResult.DONE
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            AutoBackupResult.NO_ACCESS
        } catch (e: Exception) {
            AutoBackupResult.FAILED
        }
    }

    /** Deletes the oldest automatic backups beyond [KEEP]; the date in the name sorts by age. */
    private fun prune(folder: DocumentFile) {
        folder.listFiles()
            .filter { file -> file.name?.let { isAutoBackupName(it) } == true }
            .sortedByDescending { it.name }
            .drop(KEEP)
            .forEach { it.delete() }
    }

    companion object {
        const val KEEP = 7
        private const val MIME_TYPE = "application/json"
        private const val PREFIX = "feather-auto-backup-"
        private const val SUFFIX = ".json"
        private val NAME_PATTERN = Regex("^${Regex.escape(PREFIX)}\\d{4}-\\d{2}-\\d{2}${Regex.escape(SUFFIX)}$")

        fun fileName(date: LocalDate): String = "$PREFIX$date$SUFFIX"

        fun isAutoBackupName(name: String): Boolean = NAME_PATTERN.matches(name)
    }
}
