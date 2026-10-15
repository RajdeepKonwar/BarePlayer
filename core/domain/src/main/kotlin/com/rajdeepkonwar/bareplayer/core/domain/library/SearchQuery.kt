package com.rajdeepkonwar.bareplayer.core.domain.library

/** Converts free-form user input into safe search expressions. */
object SearchQuery {
    private val TOKEN_SPLIT = Regex("[^\\p{L}\\p{N}]+")

    fun tokens(input: String): List<String> =
        input.lowercase().split(TOKEN_SPLIT).filter { it.isNotEmpty() }.take(MAX_TOKENS)

    /**
     * Builds an FTS4 MATCH expression where every token must prefix-match, e.g. "beat it" ->
     * `"beat"* "it"*`. Returns null when the input has no searchable characters.
     */
    fun toFtsMatch(input: String): String? {
        val tokens = tokens(input)
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "\"$it\"*" }
    }

    /** A LIKE pattern for substring matches, with SQL wildcards escaped using '\'. */
    fun toLikePattern(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val escaped = trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return "%$escaped%"
    }

    private const val MAX_TOKENS = 8
}
