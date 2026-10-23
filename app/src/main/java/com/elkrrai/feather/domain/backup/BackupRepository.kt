package com.elkrrai.feather.domain.backup

/** What happens to the tasks already on the phone when a backup is imported. */
enum class ImportMode {
    /** Keep both: for every record the newer edit wins, nothing is lost. */
    MERGE,

    /** Delete everything on the phone first, so it matches the file exactly. */
    REPLACE,
}

/** How many records the imported file contained (not how many changed). */
data class ImportSummary(val tasks: Int, val tags: Int)

/** The file could not be used. */
class BackupFormatException(val reason: Reason, message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    enum class Reason {
        /** Not a Feather backup, or damaged. */
        NOT_A_BACKUP,

        /** Made by a newer version of the app than this one. */
        TOO_NEW,
    }
}

interface BackupRepository {
    /** The whole database as a backup file (see `BackupFile`), in the JSON text form. */
    suspend fun exportJson(): String

    /** Open and finished top-level tasks as CSV text, for reading in a spreadsheet. */
    suspend fun exportCsv(): String

    /** @throws BackupFormatException if [text] is not a usable backup */
    suspend fun importJson(text: String, mode: ImportMode): ImportSummary
}
