package com.elkrrai.feather.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsIntentsTest {

    @Test
    fun knownPhoneMakersGetTheirOwnGuidePage() {
        assertEquals("https://dontkillmyapp.com/oneplus", SettingsIntents.vendorGuideUrl("OnePlus"))
        assertEquals("https://dontkillmyapp.com/xiaomi", SettingsIntents.vendorGuideUrl("Xiaomi"))
        assertEquals("https://dontkillmyapp.com/samsung", SettingsIntents.vendorGuideUrl("samsung"))
    }

    @Test
    fun otherMakersGetTheFrontPage() {
        assertEquals("https://dontkillmyapp.com/", SettingsIntents.vendorGuideUrl("Google"))
        assertEquals("https://dontkillmyapp.com/", SettingsIntents.vendorGuideUrl("realme"))
    }

    @Test
    fun restrictiveMakersAreRecognizedIgnoringCase() {
        assertTrue(SettingsIntents.isRestrictiveVendor("OPPO"))
        assertTrue(SettingsIntents.isRestrictiveVendor("Realme"))
        assertTrue(SettingsIntents.isRestrictiveVendor("HUAWEI"))
        assertFalse(SettingsIntents.isRestrictiveVendor("Google"))
        assertFalse(SettingsIntents.isRestrictiveVendor("Motorola"))
    }
}
