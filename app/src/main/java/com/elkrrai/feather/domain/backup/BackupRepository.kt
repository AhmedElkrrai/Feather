package com.elkrrai.feather.domain.backup

/** What happens to the tasks already on the phone when a backup is imported. */
enum class ImportMode {
    /** Keep both: for every record the newer edit wins, nothing is lost. */
    MERGE,

    /** Delete everything on the phone first, so it matches the file exactly. */
    REPLACE,
}

/**
 * How many records the imported file contained (not how many changed), whether the settings in it were put back, and the app
 * language it carried (a language tag, empty for the phone's own), which the screen applies.
 */
data class ImportSummary(val tasks: Int, val tags: Int, val settingsRestored: Boolean = false, val language: String? = null)

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

    /**
     * [restoreSettings] also puts back the preferences of the file, when it has them.
     * @throws BackupFormatException if [text] is not a usable backup
     */
    suspend fun importJson(text: String, mode: ImportMode, restoreSettings: Boolean = true): ImportSummary
}

/** The preferences as they go into a backup file, and back out of it. */
interface BackupPreferences {
    /**
     * The preferences worth keeping, as text by name. Those that only make sense on one phone (the backup folder, the app lock,
     * the cards put away today) are left out.
     */
    suspend fun backedUpPreferences(): Map<String, String>

    /** Puts preferences from a backup back: what was earned or counted is combined with what is here, a choice is replaced. Unknown names are ignored. */
    suspend fun restoreBackedUpPreferences(values: Map<String, String>)
}
