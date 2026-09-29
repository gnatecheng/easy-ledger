package com.qingjizhang.app.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.qingjizhang.app.domain.Money
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object AppFormatters {
    @Composable
    fun yearMonth(ym: YearMonth): String {
        val locale = LocalConfiguration.current.locales[0]
        return if (locale.language.startsWith("en")) {
            ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
        } else {
            ym.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA))
        }
    }

    @Composable
    fun monthDayWeek(date: java.time.LocalDate): String {
        val locale = LocalConfiguration.current.locales[0]
        return if (locale.language.startsWith("en")) {
            date.format(DateTimeFormatter.ofPattern("MMM d, EEE", locale))
        } else {
            date.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA))
        }
    }

    @Composable
    fun shortMonth(ym: YearMonth): String {
        val locale = LocalConfiguration.current.locales[0]
        return if (locale.language.startsWith("en")) {
            ym.format(DateTimeFormatter.ofPattern("MMM", Locale.US))
        } else {
            ym.format(DateTimeFormatter.ofPattern("M月", Locale.CHINA))
        }
    }

    @Composable
    fun moneyLocale(): Locale =
        LocalConfiguration.current.locales[0]

    @Composable
    fun formatYuan(cents: Long, withSign: Boolean = false): String =
        Money.formatYuan(cents, withSign, moneyLocale())

    @Composable
    fun formatPlain(cents: Long): String = Money.formatPlain(cents, moneyLocale())

    @Composable
    fun localDate(date: java.time.LocalDate): String {
        val locale = LocalConfiguration.current.locales[0]
        return if (locale.language.startsWith("en")) {
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale))
        } else {
            date.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA))
        }
    }
}
