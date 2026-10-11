package com.mrdev.tashtech.ui.onboarding

/** Case-, dash- and whitespace-insensitive: "se2401" matches "SE-24-01". */
internal fun filterGroups(groups: List<String>, query: String): List<String> {
    val q = query.normalizeForSearch()
    if (q.isEmpty()) return groups
    return groups.filter { it.normalizeForSearch().contains(q) }
}

private fun String.normalizeForSearch() =
    filterNot { it == '-' || it.isWhitespace() }.lowercase()
