package com.elkrrai.feather.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads and writes the files the user picked with the system file picker (content URIs). */
@Singleton
class BackupFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Replaces the content of [uri] with [text] (UTF-8). */
    suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        // "wt" truncates: without it a shorter text would leave the tail of an older file behind.
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw FileNotFoundException("Cannot open $uri for writing")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    /**
     * Puts [text] in a file of the cache named [name] and returns a link other apps can open it with, for the share sheet.
     * Older shared backups are removed first, so at most one stays on the phone.
     */
    suspend fun writeShareable(name: String, text: String): Uri = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, SHARE_FOLDER).apply { mkdirs() }
        folder.listFiles()?.forEach { it.delete() }
        val file = File(folder, name)
        file.writeText(text, Charsets.UTF_8)
        FileProvider.getUriForFile(context, "${context.packageName}.backupshare", file)
    }

    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("Cannot open $uri for reading")
        stream.use { String(it.readBytes(), Charsets.UTF_8) }
    }

    private companion object {
        /** Matches `backup_share_paths.xml`. */
        const val SHARE_FOLDER = "backup-share"
    }
}
