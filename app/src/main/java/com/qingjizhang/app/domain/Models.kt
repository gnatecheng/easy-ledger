package com.qingjizhang.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TxnKind(val label: String) {
    EXPENSE("支出"),
    INCOME("收入"),
    TRANSFER("转账");

    companion object {
        val ledger: List<TxnKind> = listOf(EXPENSE, INCOME)

        fun fromRaw(raw: String): TxnKind =
            entries.find { it.name.equals(raw, true) || it.label == raw }
                ?: when {
                    raw.contains("转") || raw.equals("transfer", true) -> TRANSFER
                    raw.contains("收") || raw.equals("in", true) -> INCOME
                    else -> EXPENSE
                }
    }
}

enum class AccountType(val label: String) {
    CASH("现金"),
    BANK("银行卡"),
    CREDIT_CARD("信用卡"),
    ALIPAY("支付宝"),
    WECHAT("微信"),
    OTHER("其他");

    companion object {
        fun fromRaw(raw: String): AccountType =
            entries.find { it.name.equals(raw, true) || it.label == raw } ?: OTHER
    }
}

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val initialBalanceCents: Long,
    val colorArgb: Int,
    val archived: Boolean,
    val sortOrder: Int,
    val balanceCents: Long = initialBalanceCents,
)

data class Category(
    val id: Long,
    val name: String,
    val kind: TxnKind,
    val colorArgb: Int,
    val emoji: String,
    val archived: Boolean,
    val sortOrder: Int,
)

data class Txn(
    val id: Long,
    val amountCents: Long,
    val kind: TxnKind,
    val occurredAt: Long,
    val categoryId: Long,
    val accountId: Long,
    val note: String,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val categoryName: String = "",
    val categoryColor: Int = 0,
    val categoryEmoji: String = "",
    val accountName: String = "",
    val accountColor: Int = 0,
    val receiptPath: String? = null,
    val recurringRuleId: Long? = null,
    val transferToAccountId: Long? = null,
    val transferToAccountName: String = "",
    val transferToAccountColor: Int = 0,
) {
    val signedCents: Long get() = when (kind) {
        TxnKind.INCOME -> amountCents
        TxnKind.EXPENSE -> -amountCents
        TxnKind.TRANSFER -> 0L
    }
    val dateTime: LocalDateTime
        get() = Instant.ofEpochMilli(occurredAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val date: LocalDate get() = dateTime.toLocalDate()
}

data class Budget(
    val id: Long,
    val categoryId: Long?,
    val amountCents: Long,
)

data class MonthSummary(
    val yearMonth: YearMonth,
    val incomeCents: Long,
    val expenseCents: Long,
    val transferCents: Long = 0,
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

data class CategorySlice(
    val category: Category,
    val amountCents: Long,
    val ratio: Float,
)

data class BudgetStatus(
    val budget: Budget,
    val spentCents: Long,
    val category: Category?,
) {
    val ratio: Float
        get() = if (budget.amountCents <= 0L) 0f else (spentCents.toFloat() / budget.amountCents).coerceAtLeast(0f)
    val over: Boolean get() = spentCents > budget.amountCents && budget.amountCents > 0
    val warn: Boolean get() = !over && ratio >= 0.8f && budget.amountCents > 0
}

object Money {
    fun formatYuan(cents: Long, withSign: Boolean = false, locale: Locale = Locale.getDefault()): String {
        val abs = kotlin.math.abs(cents) / 100.0
        val body = "¥" + "%.2f".format(locale, abs)
        return when {
            !withSign -> body
            cents > 0 -> "+$body"
            cents < 0 -> "-$body"
            else -> body
        }
    }

    fun formatPlain(cents: Long, locale: Locale = Locale.getDefault()): String =
        "%.2f".format(locale, cents / 100.0)

    fun parseYuan(raw: String): Long? {
        val cleaned = raw.trim()
            .replace("¥", "")
            .replace("￥", "")
            .replace(",", "")
            .replace("，", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        return Math.round(value * 100.0)
    }
}

object Dates {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    val ymCn: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
    val dayCn: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA)
    val dateCn: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    val timeCn: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    val dateTimeCn: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)

    fun monthStart(ym: YearMonth): Long =
        ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun monthEndExclusive(ym: YearMonth): Long =
        ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDayExclusive(date: LocalDate): Long =
        date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun of(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
}

fun String.parseTags(): List<String> =
    split(',', '，', ' ', '、', ';', '；')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

fun List<String>.joinTags(): String = joinToString("，")

enum class RecurringFrequency(val label: String) {
    DAILY("每天"),
    WEEKLY("每周"),
    MONTHLY("每月");

    companion object {
        fun fromRaw(raw: String): RecurringFrequency =
            entries.find { it.name.equals(raw, true) || it.label == raw } ?: MONTHLY
    }
}

data class RecurringRule(
    val id: Long,
    val amountCents: Long,
    val kind: TxnKind,
    val categoryId: Long,
    val accountId: Long,
    val note: String,
    val frequency: RecurringFrequency,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val maxCount: Int?,
    val generatedCount: Int,
    val lastGeneratedDate: LocalDate?,
    val paused: Boolean,
    val categoryName: String = "",
    val categoryColor: Int = 0,
    val categoryEmoji: String = "",
    val accountName: String = "",
) {
    fun nextDate(today: LocalDate = LocalDate.now()): LocalDate? {
        if (paused) return null
        if (maxCount != null && generatedCount >= maxCount) return null
        var cursor = lastGeneratedDate?.let { RecurringDates.next(it, frequency, startDate) } ?: startDate
        if (endDate != null && cursor.isAfter(endDate)) return null
        if (cursor.isAfter(today) && (lastGeneratedDate != null || cursor != startDate && cursor.isAfter(today))) {
            return cursor.takeUnless { endDate != null && it.isAfter(endDate) }
        }
        return cursor.takeUnless { endDate != null && it.isAfter(endDate) }
    }
}

object RecurringDates {
    fun next(from: LocalDate, frequency: RecurringFrequency, start: LocalDate): LocalDate {
        return when (frequency) {
            RecurringFrequency.DAILY -> from.plusDays(1)
            RecurringFrequency.WEEKLY -> from.plusWeeks(1)
            RecurringFrequency.MONTHLY -> {
                val nxt = from.plusMonths(1)
                val day = start.dayOfMonth.coerceAtMost(nxt.lengthOfMonth())
                nxt.withDayOfMonth(day)
            }
        }
    }

    fun dueDates(rule: RecurringRule, today: LocalDate): List<LocalDate> {
        if (rule.paused) return emptyList()
        val out = mutableListOf<LocalDate>()
        var cursor = rule.lastGeneratedDate?.let { next(it, rule.frequency, rule.startDate) } ?: rule.startDate
        var count = rule.generatedCount
        var guard = 0
        while (guard++ < 400) {
            if (cursor.isAfter(today)) break
            if (rule.endDate != null && cursor.isAfter(rule.endDate)) break
            if (rule.maxCount != null && count >= rule.maxCount) break
            out += cursor
            count++
            cursor = next(cursor, rule.frequency, rule.startDate)
        }
        return out
    }
}
