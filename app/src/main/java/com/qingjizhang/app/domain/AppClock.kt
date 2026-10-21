package com.qingjizhang.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/** Wall clock for the app; override in screenshot / deterministic tests. */
object AppClock {
    private val zone: ZoneId = ZoneId.systemDefault()

    @Volatile
    var todayOverride: LocalDate? = null

    fun today(): LocalDate = todayOverride ?: LocalDate.now(zone)

    fun yearMonth(): YearMonth = YearMonth.from(today())

    fun nowMillis(): Long =
        if (todayOverride != null) {
            today().atTime(9, 30).atZone(zone).toInstant().toEpochMilli()
        } else {
            System.currentTimeMillis()
        }

    fun instant(): Instant = Instant.ofEpochMilli(nowMillis())

    fun atStartOfDay(): LocalDateTime = today().atStartOfDay()
}
