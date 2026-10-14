package com.elkrrai.feather.data.backup

import com.elkrrai.feather.domain.backup.BackupFormatException
import com.elkrrai.feather.domain.backup.BackupFormatException.Reason
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/** Reads and writes the JSON text of a [BackupFile], and refuses files it cannot use. */
object BackupCodec {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /** @throws BackupFormatException for text that is not a backup, or is from a newer app */
    fun decode(text: String): BackupFile {
        // Check what the file says it is before reading the details: the records all have
        // defaults, so a random JSON object would otherwise pass as an empty backup.
        val root = try {
            json.parseToJsonElement(text)
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException too.
            throw notABackup(e)
        }
        val obj = root as? JsonObject ?: throw notABackup()
        val format = (obj["format"] as? JsonPrimitive)?.contentOrNull
        val version = (obj["version"] as? JsonPrimitive)?.intOrNull
        if (format != BackupFile.FORMAT || version == null || version < 1) throw notABackup()
        if (version > BackupFile.VERSION) {
            throw BackupFormatException(
                Reason.TOO_NEW,
                "Backup version $version is newer than this app understands (${BackupFile.VERSION})",
            )
        }
        return try {
            json.decodeFromJsonElement(BackupFile.serializer(), obj)
        } catch (e: IllegalArgumentException) {
            throw notABackup(e)
        }
    }

    private fun notABackup(cause: Throwable? = null) =
        BackupFormatException(Reason.NOT_A_BACKUP, "Not a Feather backup", cause)
}
