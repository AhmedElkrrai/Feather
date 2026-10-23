package com.elkrrai.feather.data.backup

import com.elkrrai.feather.domain.backup.BackupFormatException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {

    private fun rejectionOf(text: String): BackupFormatException.Reason {
        try {
            BackupCodec.decode(text)
        } catch (e: BackupFormatException) {
            return e.reason
        }
        fail("Expected '$text' to be rejected")
        throw AssertionError()
    }

    @Test
    fun aBackupSurvivesAWriteAndRead() {
        val file = BackupFile(
            exportedAt = 123,
            tags = listOf(TagRecord("t", "عمل", deletedAt = 9)),
            tasks = listOf(
                TaskRecord(
                    id = "a",
                    title = "Call mom \"today\"\nnow",
                    dueDate = "2026-10-14",
                    dueTime = "17:00",
                    priority = 3,
                    rrule = "FREQ=WEEKLY;BYDAY=MO",
                    repeatMode = 1,
                ),
            ),
            taskTags = listOf(TaskTagRecord("a", "t")),
            reminders = listOf(ReminderRecord("r", "a", kind = 1, atDate = "2026-10-13", atTime = "09:00")),
            completions = listOf(CompletionRecord("c", "a", completedAt = 42, dueDate = "2026-10-07")),
        )

        assertEquals(file, BackupCodec.decode(BackupCodec.encode(file)))
    }

    @Test
    fun theFileSaysWhatItIs() {
        val text = BackupCodec.encode(BackupFile())

        assertTrue(text.contains("\"format\": \"feather-backup\""))
        assertTrue(text.contains("\"version\": 1"))
    }

    @Test
    fun fieldsAddedLaterMayBeMissingAndUnknownFieldsAreIgnored() {
        val text = """
            {
              "format": "feather-backup",
              "version": 1,
              "somethingNew": [1, 2, 3],
              "tasks": [
                { "id": "a", "listId": "l", "title": "Old file", "futureField": true }
              ]
            }
        """.trimIndent()

        val task = BackupCodec.decode(text).tasks.single()

        assertEquals("Old file", task.title)
        assertEquals("", task.notes)
        assertEquals(0, task.priority)
        assertNull(task.rrule)
        assertEquals(0, task.repeatMode)
    }

    @Test
    fun textThatIsNotABackupIsRejected() {
        val notABackup = BackupFormatException.Reason.NOT_A_BACKUP
        assertEquals(notABackup, rejectionOf(""))
        assertEquals(notABackup, rejectionOf("hello"))
        assertEquals(notABackup, rejectionOf("[1, 2]"))
        assertEquals(notABackup, rejectionOf("{\"tasks\": []}"))
        assertEquals(notABackup, rejectionOf("{\"format\": \"other\", \"version\": 1}"))
        assertEquals(notABackup, rejectionOf("{\"format\": \"feather-backup\", \"version\": 0}"))
        assertEquals(notABackup, rejectionOf("{\"format\": \"feather-backup\", \"version\": 1, \"tasks\": [{\"id\": 1}]}"))
    }

    @Test
    fun aNewerVersionIsRefusedWithItsOwnReason() {
        val text = "{\"format\": \"feather-backup\", \"version\": ${BackupFile.VERSION + 1}}"

        assertEquals(BackupFormatException.Reason.TOO_NEW, rejectionOf(text))
    }

    @Test
    fun anOlderFileWithListsFoldersAndFiltersStillReads() {
        // Written before lists, folders and saved filters left the app: those parts are ignored and the
        // tasks (and their tags) are all still read.
        val text = """
            {
              "format": "feather-backup",
              "version": 1,
              "folders": [{ "id": "f", "name": "Areas" }],
              "lists": [{ "id": "l", "name": "Work", "folderId": "f" }],
              "filters": [{ "id": "x", "name": "Important", "minPriority": 3 }],
              "tags": [{ "id": "t", "name": "work", "parentId": "p" }],
              "tasks": [{ "id": "a", "listId": "l", "title": "Report" }]
            }
        """.trimIndent()

        val file = BackupCodec.decode(text)

        assertEquals("Report", file.tasks.single().title)
        assertEquals("work", file.tags.single().name)
        assertEquals("p", file.tags.single().parentId)
    }

    @Test
    fun aTagThatSitsUnderAnotherSurvivesAWriteAndRead() {
        val file = BackupFile(tags = listOf(TagRecord("t", "bugfix", parentId = "w"), TagRecord("w", "work")))

        assertEquals(file, BackupCodec.decode(BackupCodec.encode(file)))
    }
}
