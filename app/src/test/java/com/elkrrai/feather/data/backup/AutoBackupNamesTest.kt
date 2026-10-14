package com.elkrrai.feather.data.backup

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupNamesTest {

    @Test
    fun theFileNameCarriesTheDateSoNamesSortByAge() {
        val names = listOf(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 9, 30), LocalDate.of(2027, 1, 2))
            .map { AutoBackup.fileName(it) }

        assertEquals("feather-auto-backup-2026-10-14.json", names[0])
        assertEquals(names.sorted(), names.sortedBy { it })
        assertEquals(
            listOf("feather-auto-backup-2026-09-30.json", "feather-auto-backup-2026-10-14.json", "feather-auto-backup-2027-01-02.json"),
            names.sorted(),
        )
    }

    @Test
    fun onlyAutomaticBackupsAreRecognized() {
        assertTrue(AutoBackup.isAutoBackupName("feather-auto-backup-2026-10-14.json"))

        // A manual export saved in the same folder must never be pruned or replaced.
        assertFalse(AutoBackup.isAutoBackupName("feather-backup-2026-10-14.json"))
        assertFalse(AutoBackup.isAutoBackupName("feather-auto-backup-2026-10-14.json.bak"))
        assertFalse(AutoBackup.isAutoBackupName("feather-auto-backup-2026-10-14 (1).json"))
        assertFalse(AutoBackup.isAutoBackupName("notes.json"))
    }
}
