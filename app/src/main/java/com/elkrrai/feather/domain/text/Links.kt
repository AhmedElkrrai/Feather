package com.elkrrai.feather.domain.text

/** A web address found in text: what to open and a short form to show. */
data class Link(val url: String, val label: String)

private val LINK = Regex("""(?i)(?<![\p{L}\p{N}/@.])(?:https?://|www\.)[^\s<>"']+""")

/** Punctuation that ends a sentence rather than an address ("see https://example.com."). */
private const val TRAILING = ".,;:!?)]}،؛؟"

/**
 * The web addresses in [text], in order, without repeats. Addresses starting with `www.` are
 * opened as https. Pure Kotlin, so it is tested without Android.
 */
fun findLinks(text: String): List<Link> =
    LINK.findAll(text)
        .map { it.value.trimEnd { char -> char in TRAILING } }
        .filter { it.isNotEmpty() }
        .map { raw ->
            val url = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
            Link(url = url, label = shortLabel(url))
        }
        .distinctBy { it.url }
        .toList()

/** `https://www.example.com/a/b?x=1` shown as `example.com/a/b?x=1`. */
private fun shortLabel(url: String): String =
    url.replaceFirst(Regex("(?i)^https?://"), "").removePrefix("www.").trimEnd('/')
