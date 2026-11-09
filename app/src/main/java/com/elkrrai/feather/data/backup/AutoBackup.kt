package com.elkrrai.feather.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.elkrrai.feather.domain.backup.BackupRepository
import com.elkrrai.feather.domain.settings.BackupStyle
import com.elkrrai.feather.domain.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.TimeUnit
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
 * Writes the automatic backup into the folder the user chose, in the way the user picked ([BackupStyle]): one
 * file that is replaced each time (the default), a file for each of the last [KEEP] days, or one file plus one
 * older copy. A file is first written under a temporary name and takes its real name only once it is complete,
 * so a backup that fails half-way never destroys the one before it. The files have their own name prefix, so
 * manual exports saved in the same folder are never replaced or removed, and the files an earlier style made
 * are left where they are.
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
            val saved = withContext(Dispatchers.IO) {
                when (settings.current().backupStyle) {
                    BackupStyle.ONE_FILE -> replace(folder, MAIN_NAME, text)
                    // A second backup on the same day replaces the first.
                    BackupStyle.SEVEN_DAYS -> replace(folder, fileName(LocalDate.now()), text).also { if (it) prune(folder) }
                    BackupStyle.ONE_PLUS_OLDER -> {
                        keepOlderCopy(folder)
                        replace(folder, MAIN_NAME, text)
                    }
                }
            }
            if (!saved) return AutoBackupResult.FAILED
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

    /**
     * Writes [text] as [name] in [folder]; a file of that name is replaced only once the new one is complete. A folder such as Downloads does not
     * let an app delete a file an earlier install made, and renaming the new file onto that name then makes "name (1).json" and a copy every day.
     * So when the old file is there, the complete text is written over it in place; the numbered copies an earlier run made are removed.
     */
    private suspend fun replace(folder: DocumentFile, name: String, text: String): Boolean {
        folder.findFile(TEMP_NAME)?.delete() // left by a backup that was interrupted
        val temp = folder.createFile(MIME_TYPE, TEMP_NAME) ?: return false
        files.write(temp.uri, text)
        val existing = folder.findFile(name)
        val done = if (existing == null) {
            takeName(folder, temp, name, text)
        } else if (writeInPlace(existing, text)) {
            temp.delete()
            true
        } else {
            existing.delete() && takeName(folder, temp, name, text)
        }
        if (done) removeNumberedCopies(folder, name)
        return done
    }

    private suspend fun writeInPlace(file: DocumentFile, text: String): Boolean =
        try {
            files.write(file.uri, text)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }

    /** Gives the finished temporary file its real name, or writes the text under that name in a folder that cannot rename. */
    private suspend fun takeName(folder: DocumentFile, temp: DocumentFile, name: String, text: String): Boolean {
        if (temp.renameTo(name)) return true
        val target = folder.createFile(MIME_TYPE, name) ?: return false
        files.write(target.uri, text)
        temp.delete()
        return true
    }

    /** Removes "name (1).json", "name (2).json" and so on: the copies that older runs left when the old file could not be replaced. */
    private fun removeNumberedCopies(folder: DocumentFile, name: String) {
        val base = name.substringBeforeLast('.')
        val extension = name.substringAfterLast('.')
        val numbered = Regex("^${Regex.escape(base)} \\(\\d+\\)\\.${Regex.escape(extension)}$")
        folder.listFiles()
            .filter { file -> file.name?.let { numbered.matches(it) } == true }
            .forEach { it.delete() }
    }

    /** Before the file is replaced, copies it to the older-copy file when that one is missing or about a week old. */
    private suspend fun keepOlderCopy(folder: DocumentFile) {
        val current = folder.findFile(MAIN_NAME) ?: return
        val previous = folder.findFile(PREVIOUS_NAME)
        if (!shouldRotate(previous?.lastModified(), clock.millis())) return
        replace(folder, PREVIOUS_NAME, files.read(current.uri))
    }

    /** Deletes the oldest dated backups beyond [KEEP]; the date in the name sorts by age. */
    private fun prune(folder: DocumentFile) {
        folder.listFiles()
            .filter { file -> file.name?.let { isAutoBackupName(it) } == true }
            .sortedByDescending { it.name }
            .drop(KEEP)
            .forEach { it.delete() }
    }

    companion object {
        /** How many daily files the "last 7 days" style keeps. */
        const val KEEP = 7
        private const val MIME_TYPE = "application/json"
        private const val PREFIX = "feather-auto-backup-"
        private const val SUFFIX = ".json"
        private val NAME_PATTERN = Regex("^${Regex.escape(PREFIX)}\\d{4}-\\d{2}-\\d{2}${Regex.escape(SUFFIX)}$")

        /** The one file of the default style, and of "one file plus an older copy". */
        const val MAIN_NAME = "feather-auto-backup.json"

        /** The older copy, refreshed about once a week. Neither name matches the dated pattern, so nothing prunes them. */
        const val PREVIOUS_NAME = "feather-auto-backup-previous.json"

        /** Where a backup is written before it takes its real name. */
        private const val TEMP_NAME = "feather-auto-backup-new.json"

        /** The older copy is refreshed when it is this old. */
        private val OLDER_EVERY_MILLIS = TimeUnit.DAYS.toMillis(7)

        fun fileName(date: LocalDate): String = "$PREFIX$date$SUFFIX"

        fun isAutoBackupName(name: String): Boolean = NAME_PATTERN.matches(name)

        /**
         * Whether the older copy should be refreshed from the current file: it is missing, or its age is not known
         * (some folders do not tell), or it is a week old. [previousModifiedAt] is its last change in epoch millis.
         */
        fun shouldRotate(previousModifiedAt: Long?, now: Long): Boolean =
            previousModifiedAt == null || previousModifiedAt <= 0L || now - previousModifiedAt >= OLDER_EVERY_MILLIS
    }
}
