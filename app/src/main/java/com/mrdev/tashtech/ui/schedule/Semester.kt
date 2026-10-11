package com.mrdev.tashtech.ui.schedule

import java.time.LocalDate

object Semester {
    val END: LocalDate = LocalDate.of(2026, 12, 25)
    fun isActive(date: LocalDate) = !date.isAfter(END)
    val START: LocalDate = LocalDate.of(2026, 9, 1) // earliest week you can browse back to
}