package com.mrdev.tashtech.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupFilterTest {
    private val groups = listOf("BA-24-01", "CS-24-01", "SE-24-01", "SE-24-02")

    @Test fun blankQueryReturnsAll() = assertEquals(groups, filterGroups(groups, "  "))

    @Test fun ignoresCaseDashesAndSpaces() =
        assertEquals(listOf("SE-24-01"), filterGroups(groups, "se 2401"))

    @Test fun prefixMatchesMultiple() =
        assertEquals(listOf("SE-24-01", "SE-24-02"), filterGroups(groups, "SE-24"))

    @Test fun noMatchReturnsEmpty() = assertEquals(emptyList<String>(), filterGroups(groups, "xyz"))
}
