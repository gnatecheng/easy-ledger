package com.qingjizhang.app.data

import com.qingjizhang.app.domain.AccountType
import com.qingjizhang.app.domain.Dates
import com.qingjizhang.app.domain.TxnKind
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlin.random.Random

object Palette {
    val expense = listOf(
        0xFFE07A5F.toInt(),
        0xFF3D5A80.toInt(),
        0xFFE9C46A.toInt(),
        0xFF2A9D8F.toInt(),
        0xFF8D6A9F.toInt(),
        0xFFF4A261.toInt(),
        0xFFE76F51.toInt(),
        0xFF457B9D.toInt(),
        0xFF5C9EAD.toInt(),
        0xFFC9A27A.toInt(),
        0xFF81B29A.toInt(),
        0xFF6D6875.toInt(),
    )
    val income = listOf(
        0xFF2D6A4F.toInt(),
        0xFF40916C.toInt(),
        0xFF1D3557.toInt(),
        0xFF52796F.toInt(),
        0xFFC1121F.toInt(),
        0xFF6D6875.toInt(),
    )
    val accounts = listOf(
        0xFF6B705C.toInt(),
        0xFF1D3557.toInt(),
        0xFF9B2226.toInt(),
        0xFF1677FF.toInt(),
        0xFF07C160.toInt(),
    )
    val extras = listOf(
        0xFF264653.toInt(), 0xFF2A9D8F.toInt(), 0xFFE9C46A.toInt(), 0xFFF4A261.toInt(),
        0xFFE76F51.toInt(), 0xFF606C38.toInt(), 0xFF283618.toInt(), 0xFFBC6C25.toInt(),
        0xFF0077B6.toInt(), 0xFF7209B7.toInt(), 0xFFB5179E.toInt(), 0xFF80B918.toInt(),
        0xFFD62828.toInt(), 0xFF023E8A.toInt(), 0xFF588157.toInt(), 0xFFCA6702.toInt(),
    )
}

class Seeder(
    private val db: AppDatabase,
    private val settings: SettingsStore,
) {
    suspend fun seedIfNeeded() {
        val current = settings.settings.first()
        if (current.demoSeeded) return
        seedFresh()
        settings.update { it.copy(demoSeeded = true) }
    }

    suspend fun resetDemoData() {
        db.transactions().deleteAll()
        db.recurring().deleteAll()
        db.budgets().deleteAll()
        db.categories().deleteAll()
        db.accounts().deleteAll()
        seedFresh()
        settings.update { it.copy(demoSeeded = true) }
    }

    suspend fun clearTransactionsKeepMasters() {
        db.transactions().deleteAll()
    }

    private suspend fun seedFresh() {
        val now = System.currentTimeMillis()
        val expenseCats = listOf(
            "餐饮" to "🍜",
            "交通" to "🚇",
            "购物" to "🛍️",
            "住房" to "🏠",
            "日用" to "🧺",
            "娱乐" to "🎬",
            "医疗" to "💊",
            "教育" to "📚",
            "通讯" to "📱",
            "人情" to "🎁",
            "宠物" to "🐱",
            "其他" to "📦",
        ).mapIndexed { i, (name, emoji) ->
            CategoryEntity(
                name = name,
                kind = TxnKind.EXPENSE.name,
                colorArgb = Palette.expense[i % Palette.expense.size],
                emoji = emoji,
                sortOrder = i,
            )
        }
        val incomeCats = listOf(
            "工资" to "💰",
            "奖金" to "🎉",
            "理财" to "📈",
            "兼职" to "🧑‍💻",
            "红包" to "🧧",
            "其他" to "✨",
        ).mapIndexed { i, (name, emoji) ->
            CategoryEntity(
                name = name,
                kind = TxnKind.INCOME.name,
                colorArgb = Palette.income[i % Palette.income.size],
                emoji = emoji,
                sortOrder = i,
            )
        }
        db.categories().insertAll(expenseCats + incomeCats)
        val cats = db.categories().getAll()
        val catByName = cats.associateBy { "${it.kind}:${it.name}" }

        val accounts = listOf(
            AccountEntity(name = "现金", type = AccountType.CASH.name, initialBalanceCents = 32_000, colorArgb = Palette.accounts[0], sortOrder = 0),
            AccountEntity(name = "银行卡", type = AccountType.BANK.name, initialBalanceCents = 1_280_000, colorArgb = Palette.accounts[1], sortOrder = 1),
            AccountEntity(name = "信用卡", type = AccountType.CREDIT_CARD.name, initialBalanceCents = -45_000, colorArgb = Palette.accounts[2], sortOrder = 2),
            AccountEntity(name = "支付宝", type = AccountType.ALIPAY.name, initialBalanceCents = 86_500, colorArgb = Palette.accounts[3], sortOrder = 3),
            AccountEntity(name = "微信", type = AccountType.WECHAT.name, initialBalanceCents = 21_800, colorArgb = Palette.accounts[4], sortOrder = 4),
        )
        db.accounts().insertAll(accounts)
        val accs = db.accounts().getAll()
        val accByName = accs.associateBy { it.name }

        fun cat(kind: TxnKind, name: String) = catByName.getValue("${kind.name}:$name").id
        fun acc(name: String) = accByName.getValue(name).id

        db.budgets().insertAll(
            listOf(
                BudgetEntity(categoryId = null, amountCents = 800_000),
                BudgetEntity(categoryId = cat(TxnKind.EXPENSE, "餐饮"), amountCents = 200_000),
                BudgetEntity(categoryId = cat(TxnKind.EXPENSE, "交通"), amountCents = 50_000),
                BudgetEntity(categoryId = cat(TxnKind.EXPENSE, "购物"), amountCents = 150_000),
                BudgetEntity(categoryId = cat(TxnKind.EXPENSE, "娱乐"), amountCents = 80_000),
            ),
        )

        val rng = Random(20260911)
        val today = LocalDate.now()
        val txns = mutableListOf<TransactionEntity>()

        fun add(
            daysAgo: Int,
            hour: Int,
            minute: Int,
            kind: TxnKind,
            category: String,
            account: String,
            yuan: Double,
            note: String,
            tags: String = "",
        ) {
            val date = today.minusDays(daysAgo.toLong())
            txns += TransactionEntity(
                amountCents = Math.round(yuan * 100.0),
                kind = kind.name,
                occurredAt = Dates.of(date, hour, minute),
                categoryId = cat(kind, category),
                accountId = acc(account),
                note = note,
                tags = tags,
                createdAt = now,
                updatedAt = now,
            )
        }

        // Recurring-ish income
        for (m in 0..5) {
            val monthStart = today.withDayOfMonth(1).minusMonths(m.toLong())
            val offset = today.toEpochDay() - monthStart.toEpochDay()
            add(offset.toInt() + 0, 9, 12, TxnKind.INCOME, "工资", "银行卡", 12800.0, "月薪到账", "工资")
            if (m == 0 || m == 3) {
                add(offset.toInt() + 2, 10, 0, TxnKind.INCOME, "奖金", "银行卡", 2000.0, "季度奖金", "奖金")
            }
            add(offset.toInt() + 8, 16, 20, TxnKind.INCOME, "理财", "支付宝", 86.5 + m * 3, "余额宝收益", "理财")
        }

        val expensePool = listOf(
            Triple("餐饮", listOf("早餐" to 12.0, "午餐" to 38.0, "晚餐" to 56.0, "咖啡" to 18.0, "外卖" to 42.0, "超市零食" to 23.5), "支付宝"),
            Triple("交通", listOf("地铁" to 6.0, "公交" to 2.0, "打车" to 28.0, "共享单车" to 1.5), "微信"),
            Triple("购物", listOf("日用品" to 89.0, "衣服" to 199.0, "数码配件" to 59.0), "信用卡"),
            Triple("住房", listOf("房租" to 3200.0, "水电" to 180.0, "物业" to 120.0), "银行卡"),
            Triple("日用", listOf("洗衣液" to 29.9, "纸巾" to 24.8), "支付宝"),
            Triple("娱乐", listOf("电影" to 45.0, "会员" to 15.0, "聚餐" to 128.0), "微信"),
            Triple("通讯", listOf("话费" to 38.0, "宽带" to 100.0), "支付宝"),
            Triple("医疗", listOf("买药" to 36.0, "体检" to 280.0), "银行卡"),
            Triple("教育", listOf("网课" to 99.0, "书籍" to 48.0), "支付宝"),
            Triple("人情", listOf("礼物" to 168.0, "红包" to 200.0), "微信"),
        )

        for (day in 0..150) {
            if (rng.nextFloat() > 0.55f) continue
            val n = 1 + rng.nextInt(3)
            repeat(n) {
                val bucket = expensePool[rng.nextInt(expensePool.size)]
                val item = bucket.second[rng.nextInt(bucket.second.size)]
                val jitter = 1.0 + (rng.nextDouble() - 0.5) * 0.25
                val amount = if (item.first == "房租") item.second else ((item.second * jitter * 100).toLong() / 100.0)
                if (item.first == "房租" && today.minusDays(day.toLong()).dayOfMonth !in 1..3) return@repeat
                val hour = 8 + rng.nextInt(14)
                val minute = rng.nextInt(60)
                val tags = when (bucket.first) {
                    "餐饮" -> "日常"
                    "交通" -> "通勤"
                    "娱乐" -> "休闲"
                    else -> ""
                }
                add(day, hour, minute, TxnKind.EXPENSE, bucket.first, bucket.third, amount, item.first, tags)
            }
        }

        // A few large transactions so the threshold banner can show
        add(3, 14, 10, TxnKind.EXPENSE, "购物", "信用卡", 1299.0, "耳机", "数码,大额")
        add(18, 11, 0, TxnKind.EXPENSE, "住房", "银行卡", 3200.0, "本月房租", "住房")
        add(1, 19, 40, TxnKind.EXPENSE, "餐饮", "支付宝", 26.0, "晚餐", "日常")
        add(0, 8, 20, TxnKind.EXPENSE, "交通", "微信", 6.0, "地铁上班", "通勤")
        add(0, 12, 35, TxnKind.EXPENSE, "餐饮", "支付宝", 32.0, "午饭", "日常")

        // Deduplicate identical timestamps a bit by keeping all; Room is fine.
        db.transactions().insertAll(txns.sortedBy { it.occurredAt })

        val nowEpochDay = today.toEpochDay()
        db.recurring().insertAll(
            listOf(
                RecurringRuleEntity(
                    amountCents = 320_000,
                    kind = TxnKind.EXPENSE.name,
                    categoryId = cat(TxnKind.EXPENSE, "住房"),
                    accountId = acc("银行卡"),
                    note = "房租",
                    frequency = "MONTHLY",
                    startDateEpochDay = today.withDayOfMonth(1).toEpochDay(),
                    generatedCount = 1,
                    lastGeneratedEpochDay = today.withDayOfMonth(1).toEpochDay(),
                    paused = false,
                    createdAt = now,
                    updatedAt = now,
                ),
                RecurringRuleEntity(
                    amountCents = 1_280_000,
                    kind = TxnKind.INCOME.name,
                    categoryId = cat(TxnKind.INCOME, "工资"),
                    accountId = acc("银行卡"),
                    note = "月薪",
                    frequency = "MONTHLY",
                    startDateEpochDay = today.withDayOfMonth(1).toEpochDay(),
                    generatedCount = 1,
                    lastGeneratedEpochDay = today.withDayOfMonth(1).toEpochDay(),
                    paused = false,
                    createdAt = now,
                    updatedAt = now,
                ),
                RecurringRuleEntity(
                    amountCents = 6_00,
                    kind = TxnKind.EXPENSE.name,
                    categoryId = cat(TxnKind.EXPENSE, "交通"),
                    accountId = acc("微信"),
                    note = "地铁通勤",
                    frequency = "DAILY",
                    startDateEpochDay = today.minusDays(1).toEpochDay(),
                    generatedCount = 1,
                    lastGeneratedEpochDay = nowEpochDay,
                    paused = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
        )
    }
}
