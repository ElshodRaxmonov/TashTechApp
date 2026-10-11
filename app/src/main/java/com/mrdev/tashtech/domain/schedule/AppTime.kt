package com.mrdev.tashtech.domain.schedule

import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneId

object AppTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Tashkent")
    val WEEK_START: DayOfWeek = DayOfWeek.MONDAY

    fun systemClock(): Clock = Clock.system(ZONE)
}