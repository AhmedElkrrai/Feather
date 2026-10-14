package com.elkrrai.feather.domain.text

import org.junit.Assert.assertEquals
import org.junit.Test

class LinksTest {

    private fun urls(text: String) = findLinks(text).map { it.url }

    @Test
    fun findsWebAddressesInOrder() {
        val text = "Read https://example.com/a and then http://other.org/b?x=1 later"

        assertEquals(listOf("https://example.com/a", "http://other.org/b?x=1"), urls(text))
    }

    @Test
    fun addressesStartingWithWwwAreOpenedAsHttps() {
        val link = findLinks("see www.example.com/page").single()

        assertEquals("https://www.example.com/page", link.url)
        assertEquals("example.com/page", link.label)
    }

    @Test
    fun theEndOfASentenceIsNotPartOfTheAddress() {
        assertEquals(listOf("https://example.com/a"), urls("Open https://example.com/a."))
        assertEquals(listOf("https://example.com/a"), urls("(see https://example.com/a)"))
        assertEquals(listOf("https://example.com/a"), urls("Is it https://example.com/a?"))
        assertEquals(listOf("https://example.com/a"), urls("افتح https://example.com/a، ثم"))
    }

    @Test
    fun repeatedAddressesAreListedOnce() {
        assertEquals(listOf("https://example.com"), urls("https://example.com and https://example.com"))
    }

    @Test
    fun theLabelIsShort() {
        assertEquals("example.com/x", findLinks("https://www.example.com/x/").single().label)
    }

    @Test
    fun plainTextHasNoLinks() {
        assertEquals(emptyList<String>(), urls("Buy milk, eggs. Call example dot com"))
        assertEquals(emptyList<String>(), urls("name@example.com"))
    }
}
