package com.rajdeepkonwar.bareplayer.core.domain.library

import java.text.Normalizer

/** Helpers for alphabetical sorting and fast-scroll section letters. */
object SortKeys {
    private val ARTICLES = listOf("the ", "a ", "an ")
    private val DIACRITICS = Regex("\\p{Mn}+")

    /** Case- and accent-insensitive key that ignores leading English articles ("The Beatles" -> "beatles"). */
    fun sortKey(text: String): String {
        val lower = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()
        val article = ARTICLES.firstOrNull { lower.startsWith(it) && lower.length > it.length }
        return if (article != null) lower.substring(article.length).trimStart() else lower
    }

    /** Section letter for a fast scroller: A-Z, or '#' for anything else. */
    fun sectionOf(text: String): Char {
        val first = sortKey(text).firstOrNull()?.uppercaseChar() ?: return '#'
        return if (first in 'A'..'Z') first else '#'
    }
}
