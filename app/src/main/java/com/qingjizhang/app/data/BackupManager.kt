package com.qingjizhang.app.data

import com.qingjizhang.app.domain.Account
import com.qingjizhang.app.domain.AccountType
import com.qingjizhang.app.domain.Budget
import com.qingjizhang.app.domain.Category
import com.qingjizhang.app.domain.Money
import com.qingjizhang.app.domain.TxnKind
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Serializable
data class BackupFile(
    val app: String = "轻记账",
    val version: Int = 3,
    val exportedAt: String = "",
    val accounts: List<BackupAccount> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    val transactions: List<BackupTxn> = emptyList(),
    val budgets: List<BackupBudget> = emptyList(),
    val recurringRules: List<BackupRecurring> = emptyList(),
    val settings: BackupSettings? = null,
)

@Serializable
data class BackupAccount(
    val id: Long = 0,
    val name: String,
    val type: String,
    val initialBalanceCents: Long = 0,
    val colorArgb: Int = 0xFF6B705C.toInt(),
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

@Serializable
data class BackupCategory(
    val id: Long = 0,
    val name: String,
    val kind: String,
    val colorArgb: Int = 0xFF6D6875.toInt(),
    val emoji: String = "",
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

@Serializable
data class BackupTxn(
    val id: Long = 0,
    val amountCents: Long,
    val kind: String,
    val occurredAt: Long,
    val categoryId: Long,
    val accountId: Long,
    val categoryName: String = "",
    val accountName: String = "",
    val note: String = "",
    val tags: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val receiptPath: String? = null,
    val transferToAccountId: Long? = null,
    val transferToAccountName: String = "",
)

@Serializable
data class BackupBudget(
    val id: Long = 0,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val amountCents: Long,
)

@Serializable
data class BackupRecurring(
    val id: Long = 0,
    val amountCents: Long,
    val kind: String,
    val categoryId: Long = 0,
    val accountId: Long = 0,
    val categoryName: String = "",
    val accountName: String = "",
    val note: String = "",
    val frequency: String,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long? = null,
    val maxCount: Int? = null,
    val generatedCount: Int = 0,
    val lastGeneratedEpochDay: Long? = null,
    val paused: Boolean = false,
)

@Serializable
data class BackupSettings(
    val largeTxnThresholdCents: Long = 100_000,
    val inactivityNudgeDays: Int = 3,
)

data class CsvRow(
    val date: String,
    val time: String,
    val type: String,
    val amount: String,
    val category: String,
    val account: String,
    val note: String,
    val tags: String,
    val occurredAt: Long,
    val amountCents: Long,
    val kind: TxnKind,
    val toAccount: String = "",
)

enum class ImportMode { SKIP_DUPLICATES, IMPORT_ALL, REPLACE_ALL }

data class ImportPreview(
    val sourceName: String,
    val format: String,
    val csvRows: List<CsvRow> = emptyList(),
    val backup: BackupFile? = null,
    val newCount: Int,
    val duplicateCount: Int,
    val warnings: List<String> = emptyList(),
)

class BackupManager(
    private val repo: FinanceRepository,
    private val settings: SettingsStore,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportJson(): String {
        val snap = repo.snapshot()
        val current = settings.settings.first()
        val catById = snap.categories.associateBy { it.id }
        val accById = snap.accounts.associateBy { it.id }
        val file = BackupFile(
            version = 3,
            exportedAt = Instant.now().toString(),
            accounts = snap.accounts.map {
                BackupAccount(it.id, it.name, it.type, it.initialBalanceCents, it.colorArgb, it.archived, it.sortOrder)
            },
            categories = snap.categories.map {
                BackupCategory(it.id, it.name, it.kind, it.colorArgb, it.emoji, it.archived, it.sortOrder)
            },
            transactions = snap.transactions.map {
                BackupTxn(
                    id = it.id,
                    amountCents = it.amountCents,
                    kind = it.kind,
                    occurredAt = it.occurredAt,
                    categoryId = it.categoryId,
                    accountId = it.accountId,
                    categoryName = catById[it.categoryId]?.name.orEmpty(),
                    accountName = accById[it.accountId]?.name.orEmpty(),
                    note = it.note,
                    tags = it.tags,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    receiptPath = it.receiptPath,
                    transferToAccountId = it.transferToAccountId,
                    transferToAccountName = it.transferToAccountId?.let { id -> accById[id]?.name }.orEmpty(),
                )
            },
            budgets = snap.budgets.map {
                BackupBudget(it.id, it.categoryId, catById[it.categoryId]?.name, it.amountCents)
            },
            recurringRules = snap.recurring.map {
                BackupRecurring(
                    id = it.id,
                    amountCents = it.amountCents,
                    kind = it.kind,
                    categoryId = it.categoryId,
                    accountId = it.accountId,
                    categoryName = catById[it.categoryId]?.name.orEmpty(),
                    accountName = accById[it.accountId]?.name.orEmpty(),
                    note = it.note,
                    frequency = it.frequency,
                    startDateEpochDay = it.startDateEpochDay,
                    endDateEpochDay = it.endDateEpochDay,
                    maxCount = it.maxCount,
                    generatedCount = it.generatedCount,
                    lastGeneratedEpochDay = it.lastGeneratedEpochDay,
                    paused = it.paused,
                )
            },
            settings = BackupSettings(current.largeTxnThresholdCents, current.inactivityNudgeDays),
        )
        return json.encodeToString(file)
    }

    suspend fun exportCsv(): String {
        val snap = repo.snapshot()
        val catById = snap.categories.associateBy { it.id }
        val accById = snap.accounts.associateBy { it.id }
        val zone = ZoneId.systemDefault()
        val sb = StringBuilder()
        sb.appendLine("日期,时间,类型,金额,分类,账户,备注,标签,转入账户")
        snap.transactions.sortedBy { it.occurredAt }.forEach { t ->
            val dt = Instant.ofEpochMilli(t.occurredAt).atZone(zone).toLocalDateTime()
            val kind = when (t.kind) {
                TxnKind.INCOME.name -> "收入"
                TxnKind.TRANSFER.name -> "转账"
                else -> "支出"
            }
            val line = listOf(
                dt.toLocalDate().toString(),
                dt.toLocalTime().withSecond(0).withNano(0).toString().take(5),
                kind,
                Money.formatPlain(t.amountCents),
                catById[t.categoryId]?.name.orEmpty(),
                accById[t.accountId]?.name.orEmpty(),
                t.note,
                t.tags,
                t.transferToAccountId?.let { accById[it]?.name }.orEmpty(),
            ).joinToString(",") { csvEscape(it) }
            sb.appendLine(line)
        }
        return sb.toString()
    }

    suspend fun previewJson(text: String, fileName: String): ImportPreview {
        val backup = json.decodeFromString<BackupFile>(text)
        val existing = repo.snapshot()
        val existingKeys = existing.transactions.map { dupKey(it.occurredAt, it.amountCents, it.kind, it.note) }.toSet()
        val dup = backup.transactions.count { dupKey(it.occurredAt, it.amountCents, it.kind, it.note) in existingKeys }
        val warnings = buildList {
            if (backup.version > 3) add("备份版本较新，部分字段可能被忽略")
            if (backup.transactions.isEmpty()) add("备份中没有流水")
            add("账户 ${backup.accounts.size} · 分类 ${backup.categories.size} · 预算 ${backup.budgets.size} · 周期 ${backup.recurringRules.size}")
        }
        return ImportPreview(
            sourceName = fileName,
            format = "JSON",
            backup = backup,
            newCount = (backup.transactions.size - dup).coerceAtLeast(0),
            duplicateCount = dup,
            warnings = warnings,
        )
    }

    suspend fun previewCsv(text: String, fileName: String): ImportPreview {
        val rows = parseCsv(text)
        val existing = repo.snapshot()
        val existingKeys = existing.transactions.map { dupKey(it.occurredAt, it.amountCents, it.kind, it.note) }.toSet()
        val dup = rows.count { dupKey(it.occurredAt, it.amountCents, it.kind.name, it.note) in existingKeys }
        val warnings = buildList {
            if (rows.isEmpty()) add("没有解析到有效行，请确认表头包含日期和金额")
        }
        return ImportPreview(
            sourceName = fileName,
            format = "CSV",
            csvRows = rows,
            newCount = (rows.size - dup).coerceAtLeast(0),
            duplicateCount = dup,
            warnings = warnings,
        )
    }

    suspend fun apply(preview: ImportPreview, mode: ImportMode) {
        when {
            preview.backup != null -> applyJson(preview.backup, mode)
            else -> applyCsv(preview.csvRows, mode)
        }
    }

    private suspend fun applyJson(backup: BackupFile, mode: ImportMode) {
        if (mode == ImportMode.REPLACE_ALL) {
            repo.clearAll()
        }
        val incomingCats = backup.categories.toMutableList()
        if (backup.transactions.any {
                TxnKind.fromRaw(it.kind) == TxnKind.TRANSFER ||
                    it.transferToAccountId != null ||
                    it.transferToAccountName.isNotBlank()
            }
        ) {
            if (incomingCats.none { it.kind == TxnKind.TRANSFER.name || it.name == "转账" }) {
                incomingCats += BackupCategory(
                    name = "转账",
                    kind = TxnKind.TRANSFER.name,
                    colorArgb = 0xFF3D7EA6.toInt(),
                    emoji = "🔁",
                )
            }
        }
        val catMap = ensureCategories(incomingCats)
        val accMap = ensureAccounts(backup.accounts)
        val existing = repo.snapshot()
        val existingKeys = existing.transactions.map { dupKey(it.occurredAt, it.amountCents, it.kind, it.note) }.toSet()
        if (mode == ImportMode.REPLACE_ALL) {
            repo.deleteAllTransactions()
        }
        val now = System.currentTimeMillis()
        val toInsert = backup.transactions.mapNotNull { t ->
            if (mode == ImportMode.SKIP_DUPLICATES &&
                dupKey(t.occurredAt, t.amountCents, t.kind, t.note) in existingKeys
            ) return@mapNotNull null
            val categoryName = t.categoryName.ifBlank {
                backup.categories.find { it.id == t.categoryId }?.name.orEmpty()
            }
            val accountName = t.accountName.ifBlank {
                backup.accounts.find { it.id == t.accountId }?.name.orEmpty()
            }
            val kind = TxnKind.fromRaw(t.kind)
            val resolvedKind = if (t.transferToAccountId != null || t.transferToAccountName.isNotBlank()) {
                TxnKind.TRANSFER
            } else kind
            val categoryId = if (resolvedKind == TxnKind.TRANSFER) {
                catMap[TxnKind.TRANSFER.name + ":转账"]
                    ?: catMap["转账"]
                    ?: catMap.values.firstOrNull()
                    ?: return@mapNotNull null
            } else {
                catMap[resolvedKind.name + ":" + categoryName]
                    ?: catMap[categoryName]
                    ?: catMap.values.firstOrNull()
                    ?: return@mapNotNull null
            }
            val accountId = accMap[accountName] ?: accMap.values.firstOrNull() ?: return@mapNotNull null
            val toAccountId = if (resolvedKind == TxnKind.TRANSFER) {
                accMap[t.transferToAccountName].takeIf { t.transferToAccountName.isNotBlank() }
                    ?: t.transferToAccountId?.let { id ->
                        backup.accounts.find { it.id == id }?.let { accMap[it.name] }
                    }
            } else null
            TransactionEntity(
                amountCents = kotlin.math.abs(t.amountCents),
                kind = resolvedKind.name,
                occurredAt = t.occurredAt,
                categoryId = categoryId,
                accountId = accountId,
                note = t.note,
                tags = t.tags,
                createdAt = t.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = now,
                receiptPath = t.receiptPath,
                transferToAccountId = toAccountId,
            )
        }
        repo.insertTransactions(toInsert)
        backup.budgets.forEach { b ->
            val cid = when {
                b.categoryName.isNullOrBlank() && b.categoryId == null -> null
                else -> catMap[b.categoryName.orEmpty()]
                    ?: backup.categories.find { it.id == b.categoryId }?.let { catMap[it.kind + ":" + it.name] }
            }
            repo.upsertBudget(Budget(0, cid, b.amountCents))
        }
        val recNow = System.currentTimeMillis()
        val recEntities = backup.recurringRules.mapNotNull { r ->
            val kind = TxnKind.fromRaw(r.kind)
            val categoryId = catMap[kind.name + ":" + r.categoryName] ?: catMap[r.categoryName] ?: return@mapNotNull null
            val accountId = accMap[r.accountName] ?: return@mapNotNull null
            RecurringRuleEntity(
                amountCents = r.amountCents,
                kind = kind.name,
                categoryId = categoryId,
                accountId = accountId,
                note = r.note,
                frequency = r.frequency,
                startDateEpochDay = r.startDateEpochDay,
                endDateEpochDay = r.endDateEpochDay,
                maxCount = r.maxCount,
                generatedCount = r.generatedCount,
                lastGeneratedEpochDay = r.lastGeneratedEpochDay,
                paused = r.paused,
                createdAt = recNow,
                updatedAt = recNow,
            )
        }
        repo.insertRecurring(recEntities)
        backup.settings?.let { s ->
            settings.update {
                it.copy(
                    largeTxnThresholdCents = s.largeTxnThresholdCents,
                    inactivityNudgeDays = s.inactivityNudgeDays,
                )
            }
        }
    }

    private suspend fun applyCsv(rows: List<CsvRow>, mode: ImportMode) {
        if (mode == ImportMode.REPLACE_ALL) {
            repo.deleteAllTransactions()
        }
        val neededCats = rows.map {
            BackupCategory(
                name = it.category.ifBlank { if (it.kind == TxnKind.TRANSFER) "转账" else "其他" },
                kind = it.kind.name,
                colorArgb = when (it.kind) {
                    TxnKind.INCOME -> Palette.income.first()
                    TxnKind.TRANSFER -> 0xFF3D7EA6.toInt()
                    else -> Palette.expense.last()
                },
                emoji = if (it.kind == TxnKind.TRANSFER) "🔁" else "",
            )
        }
        val neededAccs = rows.flatMap {
            listOf(it.account, it.toAccount)
        }.filter { it.isNotBlank() }.distinct().map {
            BackupAccount(
                name = it.ifBlank { "现金" },
                type = AccountType.fromRaw(it).name,
                colorArgb = Palette.accounts.first(),
            )
        } + rows.map {
            BackupAccount(
                name = it.account.ifBlank { "现金" },
                type = AccountType.fromRaw(it.account).name,
                colorArgb = Palette.accounts.first(),
            )
        }
        val catMap = ensureCategories(neededCats)
        val accMap = ensureAccounts(neededAccs)
        val existing = repo.snapshot()
        val existingKeys = existing.transactions.map { dupKey(it.occurredAt, it.amountCents, it.kind, it.note) }.toSet()
        val now = System.currentTimeMillis()
        val toInsert = rows.mapNotNull { row ->
            if (mode == ImportMode.SKIP_DUPLICATES &&
                dupKey(row.occurredAt, row.amountCents, row.kind.name, row.note) in existingKeys
            ) return@mapNotNull null
            val catName = row.category.ifBlank { "其他" }
            val accName = row.account.ifBlank { "现金" }
            val categoryId = if (row.kind == TxnKind.TRANSFER) {
                catMap[TxnKind.TRANSFER.name + ":转账"] ?: catMap["转账"] ?: catMap[row.kind.name + ":" + catName]
                    ?: catMap[catName] ?: return@mapNotNull null
            } else {
                catMap[row.kind.name + ":" + catName] ?: catMap[catName] ?: return@mapNotNull null
            }
            val accountId = accMap[accName] ?: return@mapNotNull null
            val toAccountId = row.toAccount.takeIf { it.isNotBlank() }?.let { accMap[it] }
            TransactionEntity(
                amountCents = row.amountCents,
                kind = row.kind.name,
                occurredAt = row.occurredAt,
                categoryId = categoryId,
                accountId = accountId,
                note = row.note,
                tags = row.tags,
                createdAt = now,
                updatedAt = now,
                transferToAccountId = if (row.kind == TxnKind.TRANSFER) toAccountId else null,
            )
        }
        repo.insertTransactions(toInsert)
    }

    private suspend fun ensureCategories(incoming: List<BackupCategory>): MutableMap<String, Long> {
        val existing = repo.snapshot().categories
        val map = mutableMapOf<String, Long>()
        existing.forEach {
            map[it.kind + ":" + it.name] = it.id
            map[it.name] = it.id
        }
        incoming.forEach { c ->
            val key = c.kind + ":" + c.name
            if (key !in map) {
                val id = repo.upsertCategory(
                    Category(0, c.name, TxnKind.fromRaw(c.kind), c.colorArgb, c.emoji, c.archived, c.sortOrder),
                )
                map[key] = id
                map[c.name] = id
            }
        }
        return map
    }

    private suspend fun ensureAccounts(incoming: List<BackupAccount>): MutableMap<String, Long> {
        val existing = repo.snapshot().accounts
        val map = existing.associate { it.name to it.id }.toMutableMap()
        incoming.forEach { a ->
            if (a.name !in map) {
                map[a.name] = repo.upsertAccount(
                    Account(0, a.name, AccountType.fromRaw(a.type), a.initialBalanceCents, a.colorArgb, a.archived, a.sortOrder),
                )
            }
        }
        return map
    }

    companion object {
        fun dupKey(occurredAt: Long, amount: Long, kind: String, note: String): String {
            val day = Instant.ofEpochMilli(occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
            return "$day|$amount|$kind|${note.trim()}"
        }

        fun parseCsv(text: String): List<CsvRow> {
            val lines = text.replace("\uFEFF", "").split('\n').map { it.trimEnd('\r') }.filter { it.isNotBlank() }
            if (lines.isEmpty()) return emptyList()
            val header = splitCsv(lines.first()).map { normalizeHeader(it) }
            fun idx(vararg names: String): Int =
                names.firstNotNullOfOrNull { n -> header.indexOfFirst { it == n }.takeIf { it >= 0 } } ?: -1
            val iDate = idx("日期", "date", "datetime", "记账日期")
            val iTime = idx("时间", "time", "时刻")
            val iType = idx("类型", "type", "收支", "方向")
            val iAmount = idx("金额", "amount", "money", "数额")
            val iCat = idx("分类", "category", "类别")
            val iAcc = idx("账户", "account", "账号", "钱包")
            val iNote = idx("备注", "note", "memo", "说明", "描述")
            val iTags = idx("标签", "tags", "tag")
            val iToAcc = idx("转入账户", "toaccount", "转入", "对方账户")
            val hasHeader = header.any { it in setOf("日期", "date", "金额", "amount", "分类", "category") }
            val start = if (hasHeader) 1 else 0
            val rows = mutableListOf<CsvRow>()
            for (line in lines.drop(start)) {
                val cols = splitCsv(line)
                fun col(i: Int) = cols.getOrNull(i)?.trim().orEmpty()
                val amountRaw = col(iAmount).ifBlank { cols.getOrNull(3).orEmpty() }
                val amountCents = Money.parseYuan(amountRaw) ?: continue
                val dateRaw = col(iDate).ifBlank { col(0) }
                val timeRaw = if (iTime >= 0 && iTime != iDate) col(iTime) else ""
                val occurredAt = parseDateTime(dateRaw, timeRaw) ?: continue
                val typeRaw = col(iType)
                val kind = when {
                    typeRaw.contains("转") || typeRaw.equals("transfer", true) -> TxnKind.TRANSFER
                    typeRaw.contains("收") || typeRaw.equals("income", true) || typeRaw.equals("in", true) -> TxnKind.INCOME
                    typeRaw.contains("支") || typeRaw.equals("expense", true) || typeRaw.equals("out", true) -> TxnKind.EXPENSE
                    amountCents < 0 -> TxnKind.EXPENSE
                    else -> TxnKind.EXPENSE
                }
                val absAmount = kotlin.math.abs(amountCents)
                val dt = Instant.ofEpochMilli(occurredAt).atZone(ZoneId.systemDefault())
                rows += CsvRow(
                    date = dt.toLocalDate().toString(),
                    time = dt.toLocalTime().toString().take(5),
                    type = kind.label,
                    amount = Money.formatPlain(absAmount),
                    category = col(iCat).ifBlank { "其他" },
                    account = col(iAcc).ifBlank { "现金" },
                    note = col(iNote),
                    tags = col(iTags),
                    occurredAt = occurredAt,
                    amountCents = absAmount,
                    kind = kind,
                    toAccount = if (iToAcc >= 0) col(iToAcc) else "",
                )
            }
            return rows
        }

        private fun normalizeHeader(raw: String): String =
            raw.trim().trim('"').lowercase().replace(" ", "")

        private fun splitCsv(line: String): List<String> {
            val out = mutableListOf<String>()
            val buf = StringBuilder()
            var quoted = false
            var i = 0
            while (i < line.length) {
                val c = line[i]
                when {
                    c == '"' -> {
                        if (quoted && i + 1 < line.length && line[i + 1] == '"') {
                            buf.append('"'); i++
                        } else quoted = !quoted
                    }
                    c == ',' && !quoted -> {
                        out += buf.toString(); buf.clear()
                    }
                    else -> buf.append(c)
                }
                i++
            }
            out += buf.toString()
            return out
        }

        private fun parseDateTime(dateRaw: String, timeRaw: String): Long? {
            val zone = ZoneId.systemDefault()
            val combined = if (timeRaw.isNotBlank() && !dateRaw.contains(":")) "${dateRaw.trim()} ${timeRaw.trim()}" else dateRaw.trim()
            val patterns = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy/MM/dd HH:mm",
                "yyyy年M月d日 HH:mm",
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "yyyy年M月d日",
            )
            for (p in patterns) {
                try {
                    val fmt = DateTimeFormatter.ofPattern(p)
                    return if (p.contains("H")) {
                        LocalDateTime.parse(combined, fmt).atZone(zone).toInstant().toEpochMilli()
                    } else {
                        LocalDate.parse(combined, fmt).atTime(parseTime(timeRaw)).atZone(zone).toInstant().toEpochMilli()
                    }
                } catch (_: Exception) {
                }
            }
            return try {
                val date = LocalDate.parse(combined.take(10).replace('/', '-'))
                date.atTime(parseTime(timeRaw)).atZone(zone).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }

        private fun parseTime(raw: String): LocalTime {
            if (raw.isBlank()) return LocalTime.of(12, 0)
            return try {
                LocalTime.parse(raw.trim().take(5))
            } catch (_: Exception) {
                LocalTime.of(12, 0)
            }
        }

        fun csvEscape(value: String): String {
            return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
                "\"" + value.replace("\"", "\"\"") + "\""
            } else value
        }
    }
}
