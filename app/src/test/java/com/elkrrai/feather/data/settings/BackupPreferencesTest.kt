package com.elkrrai.feather.data.settings

import androidx.datastore.preferences.core.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPreferencesTest {

    @Test
    fun everyKindOfPreferenceSurvivesTheRoundTrip() {
        for (value in listOf(true, false, 7, "DARK", setOf("a", "b"), emptySet<String>())) {
            assertEquals(value, PrefCodec.decode(PrefCodec.encode(value)!!))
        }
    }

    @Test
    fun textThatIsNotOursIsRefused() {
        assertNull(PrefCodec.decode("nonsense"))
        assertNull(PrefCodec.decode("b:maybe"))
        assertNull(PrefCodec.decode("i:many"))
        assertNull(PrefCodec.decode("x:1"))
        assertNull(PrefCodec.encode(3.5))
    }

    @Test
    fun earnedThingsAreCombinedAndChoicesAreReplaced() {
        assertEquals(setOf("scarf", "glasses"), mergedPreference("nest_unlocked", setOf("scarf"), setOf("glasses")))
        assertEquals(true, mergedPreference("backed_up", true, false))
        assertEquals(true, mergedPreference("voice_used", false, true))
        assertEquals(3, mergedPreference("habit_swipe_hints_shown", 3, 1))
        assertEquals("DARK", mergedPreference("theme_mode", "LIGHT", "DARK"))
        assertEquals(10, mergedPreference("snooze_minutes", null, 10))
    }

    @Test
    fun theSeenTreesKeepTheLargerNumberOfWeeksPerHabit() {
        val merged = mergedPreference("habit_trees_seen", setOf("a|5", "b|2"), setOf("a|3", "b|9", "c|1")) as Set<*>
        assertEquals(setOf("a|5", "b|9", "c|1"), merged)
    }

    /** A new preference must be decided on: it goes into the backup, or it is listed as one that stays on the phone. */
    @Test
    fun everyPreferenceKeyIsEitherBackedUpOrKeptOnThePhone() {
        val keys = Class.forName("com.elkrrai.feather.data.settings.DataStoreSettingsRepository\$Keys")
        val names = keys.declaredFields
            .filter { Preferences.Key::class.java.isAssignableFrom(it.type) }
            .map { field ->
                field.isAccessible = true
                (field.get(null) as Preferences.Key<*>).name
            }
        assertTrue("the test found no keys", names.size > 20)
        val known = DataStoreSettingsRepository.backedUpNames() + DataStoreSettingsRepository.deviceOnlyNames()
        assertEquals("preference keys that were never decided on", emptyList<String>(), names.filter { it !in known })
    }
}
