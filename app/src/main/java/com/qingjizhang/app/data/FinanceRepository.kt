package com.qingjizhang.app.data

import com.qingjizhang.app.domain.Account
import com.qingjizhang.app.domain.AccountType
import com.qingjizhang.app.domain.Budget
import com.qingjizhang.app.domain.BudgetStatus
import com.qingjizhang.app.domain.Category
import com.qingjizhang.app.domain.CategorySlice
import com.qingjizhang.app.domain.Dates
import com.qingjizhang.app.domain.MonthSummary
import com.qingjizhang.app.domain.RecurringDates
import com.qingjizhang.app.domain.RecurringFrequency
import com.qingjizhang.app.domain.RecurringRule
import com.qingjizhang.app.domain.Txn
import com.qingjizhang.app.domain.TxnKind
import com.qingjizhang.app.domain.joinTags
import com.qingjizhang.app.domain.parseTags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth

class FinanceRepository(
    private val db: AppDatabase,
    private val receipts: ReceiptStore,
    private val onChanged: () -> Unit = {},
) {
    private val accounts = db.accounts()
    private val categories = db.categories()
    private val transactions = db.transactions()
    private val budgets = db.budgets()
    private val recurring = db.recurring()
    private val generateMutex = Mutex()

    fun observeAccounts(): Flow<List<Account>> =
        combine(accounts.observeAll(), transactions.observeAll()) { accs, txns ->
            accs.map { acc ->
                val delta = txns.filter { it.accountId == acc.id }.sumOf { it.signed() }
                acc.toDomain(acc.initialBalanceCents + delta)
            }
        }

    fun observeCategories(): Flow<List<Category>> =
        categories.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeBudgets(): Flow<List<Budget>> =
        budgets.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeRecurring(): Flow<List<RecurringRule>> =
        combine(recurring.observeAll(), categories.observeAll(), accounts.observeAll()) { rules, cats, accs ->
            val catMap = cats.associateBy { it.id }
            val accMap = accs.associateBy { it.id }
            rules.map { it.toDomain(catMap[it.categoryId], accMap[it.accountId]) }
        }

    fun observeTransactions(): Flow<List<Txn>> =
        combine(
            transactions.observeAll(),
            categories.observeAll(),
            accounts.observeAll(),
        ) { txns, cats, accs ->
            val catMap = cats.associateBy { it.id }
            val accMap = accs.associateBy { it.id }
            txns.map { it.toDomain(catMap[it.categoryId], accMap[it.accountId]) }
        }

    fun observeRange(start: Long, endExclusive: Long): Flow<List<Txn>> =
        combine(
            transactions.observeRange(start, endExclusive),
            categories.observeAll(),
            accounts.observeAll(),
        ) { txns, cats, accs ->
            val catMap = cats.associateBy { it.id }
            val accMap = accs.associateBy { it.id }
            txns.map { it.toDomain(catMap[it.categoryId], accMap[it.accountId]) }
        }

    suspend fun getTxn(id: Long): Txn? {
        val entity = transactions.getById(id) ?: return null
        return entity.toDomain(categories.getById(entity.categoryId), accounts.getById(entity.accountId))
    }

    suspend fun upsertTxn(
        id: Long?,
        amountCents: Long,
        kind: TxnKind,
        occurredAt: Long,
        categoryId: Long,
        accountId: Long,
        note: String,
        tags: List<String>,
        receiptPath: String? = null,
        recurringRuleId: Long? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val result = if (id == null || id == 0L) {
            transactions.insert(
                TransactionEntity(
                    amountCents = amountCents,
                    kind = kind.name,
                    occurredAt = occurredAt,
                    categoryId = categoryId,
                    accountId = accountId,
                    note = note.trim(),
                    tags = tags.joinTags(),
                    createdAt = now,
                    updatedAt = now,
                    receiptPath = receiptPath,
                    recurringRuleId = recurringRuleId,
                ),
            )
        } else {
            val existing = transactions.getById(id) ?: return 0
            if (receiptPath != existing.receiptPath) {
                receipts.delete(existing.receiptPath)
            }
            transactions.update(
                existing.copy(
                    amountCents = amountCents,
                    kind = kind.name,
                    occurredAt = occurredAt,
                    categoryId = categoryId,
                    accountId = accountId,
                    note = note.trim(),
                    tags = tags.joinTags(),
                    updatedAt = now,
                    receiptPath = receiptPath,
                ),
            )
            id
        }
        notifyChanged()
        return result
    }

    suspend fun deleteTxn(id: Long) {
        transactions.getById(id)?.let {
            receipts.delete(it.receiptPath)
            transactions.delete(it)
            notifyChanged()
        }
    }

    suspend fun upsertAccount(account: Account): Long {
        val entity = AccountEntity(
            id = account.id,
            name = account.name.trim(),
            type = account.type.name,
            initialBalanceCents = account.initialBalanceCents,
            colorArgb = account.colorArgb,
            archived = account.archived,
            sortOrder = account.sortOrder,
        )
        return if (account.id == 0L) accounts.insert(entity.copy(id = 0)) else {
            accounts.update(entity)
            account.id
        }
    }

    suspend fun deleteAccount(id: Long) {
        accounts.getById(id)?.let { accounts.delete(it) }
    }

    suspend fun upsertCategory(category: Category): Long {
        val entity = CategoryEntity(
            id = category.id,
            name = category.name.trim(),
            kind = category.kind.name,
            colorArgb = category.colorArgb,
            emoji = category.emoji,
            archived = category.archived,
            sortOrder = category.sortOrder,
        )
        return if (category.id == 0L) categories.insert(entity.copy(id = 0)) else {
            categories.update(entity)
            category.id
        }
    }

    suspend fun deleteCategory(id: Long) {
        categories.getById(id)?.let { categories.delete(it) }
    }

    suspend fun upsertBudget(budget: Budget): Long {
        val existing = if (budget.categoryId == null) {
            budgets.getTotal()
        } else {
            budgets.getForCategory(budget.categoryId)
        }
        return if (existing == null) {
            budgets.insert(BudgetEntity(categoryId = budget.categoryId, amountCents = budget.amountCents))
        } else {
            budgets.update(existing.copy(amountCents = budget.amountCents))
            existing.id
        }
    }

    suspend fun deleteBudget(id: Long) {
        budgets.getAll().find { it.id == id }?.let { budgets.delete(it) }
    }

    suspend fun lastOccurredAt(): Long? = transactions.lastOccurredAt()

    suspend fun getRecurring(id: Long): RecurringRule? {
        val entity = recurring.getById(id) ?: return null
        return entity.toDomain(categories.getById(entity.categoryId), accounts.getById(entity.accountId))
    }

    suspend fun upsertRecurring(rule: RecurringRule): Long {
        val now = System.currentTimeMillis()
        val entity = RecurringRuleEntity(
            id = rule.id,
            amountCents = rule.amountCents,
            kind = rule.kind.name,
            categoryId = rule.categoryId,
            accountId = rule.accountId,
            note = rule.note.trim(),
            frequency = rule.frequency.name,
            startDateEpochDay = rule.startDate.toEpochDay(),
            endDateEpochDay = rule.endDate?.toEpochDay(),
            maxCount = rule.maxCount,
            generatedCount = rule.generatedCount,
            lastGeneratedEpochDay = rule.lastGeneratedDate?.toEpochDay(),
            paused = rule.paused,
            createdAt = now,
            updatedAt = now,
        )
        val id = if (rule.id == 0L) {
            recurring.insert(entity.copy(id = 0, generatedCount = 0, lastGeneratedEpochDay = null, createdAt = now))
        } else {
            val existing = recurring.getById(rule.id) ?: return 0
            recurring.update(
                entity.copy(
                    generatedCount = existing.generatedCount,
                    lastGeneratedEpochDay = existing.lastGeneratedEpochDay,
                    createdAt = existing.createdAt,
                ),
            )
            rule.id
        }
        generateDueRecurring(LocalDate.now())
        return id
    }

    suspend fun setRecurringPaused(id: Long, paused: Boolean) {
        val existing = recurring.getById(id) ?: return
        recurring.update(existing.copy(paused = paused, updatedAt = System.currentTimeMillis()))
        if (!paused) generateDueRecurring(LocalDate.now())
    }

    suspend fun deleteRecurring(id: Long) {
        recurring.getById(id)?.let { recurring.delete(it) }
    }

    suspend fun generateDueRecurring(today: LocalDate = LocalDate.now()): Int = generateMutex.withLock {
        val cats = categories.getAll().associateBy { it.id }
        val accs = accounts.getAll().associateBy { it.id }
        var created = 0
        recurring.getActive().forEach { entity ->
            val rule = entity.toDomain(cats[entity.categoryId], accs[entity.accountId])
            val due = RecurringDates.dueDates(rule, today)
            if (due.isEmpty()) return@forEach
            var count = entity.generatedCount
            var last = entity.lastGeneratedEpochDay
            val now = System.currentTimeMillis()
            due.forEach { day ->
                val start = Dates.startOfDay(day)
                val end = Dates.endOfDayExclusive(day)
                val exists = transactions.countForRuleOnDay(entity.id, start, end) > 0
                if (!exists) {
                    transactions.insert(
                        TransactionEntity(
                            amountCents = entity.amountCents,
                            kind = entity.kind,
                            occurredAt = Dates.of(day, 9, 0),
                            categoryId = entity.categoryId,
                            accountId = entity.accountId,
                            note = entity.note.ifBlank { "周期记账" },
                            tags = "周期",
                            createdAt = now,
                            updatedAt = now,
                            recurringRuleId = entity.id,
                        ),
                    )
                    created++
                }
                count++
                last = day.toEpochDay()
            }
            recurring.update(
                entity.copy(generatedCount = count, lastGeneratedEpochDay = last, updatedAt = now),
            )
        }
        if (created > 0) notifyChanged()
        return created
    }

    private fun notifyChanged() = onChanged()

    suspend fun deleteAllTransactions() {
        transactions.getAll().forEach { receipts.delete(it.receiptPath) }
        transactions.deleteAll()
        notifyChanged()
    }

    suspend fun clearAll() {
        transactions.getAll().forEach { receipts.delete(it.receiptPath) }
        transactions.deleteAll()
        recurring.deleteAll()
        budgets.deleteAll()
        categories.deleteAll()
        accounts.deleteAll()
        receipts.deleteAll()
        notifyChanged()
    }

    suspend fun insertTransactions(entities: List<TransactionEntity>) {
        if (entities.isNotEmpty()) {
            transactions.insertAll(entities.map { it.copy(id = 0) })
            notifyChanged()
        }
    }

    suspend fun insertRecurring(entities: List<RecurringRuleEntity>) {
        if (entities.isNotEmpty()) recurring.insertAll(entities.map { it.copy(id = 0) })
    }

    suspend fun snapshot(): BackupSnapshot {
        return BackupSnapshot(
            accounts = accounts.getAll(),
            categories = categories.getAll(),
            transactions = transactions.getAll(),
            budgets = budgets.getAll(),
            recurring = recurring.getAll(),
        )
    }

    companion object {
        fun monthSummary(txns: List<Txn>, ym: YearMonth): MonthSummary {
            val start = Dates.monthStart(ym)
            val end = Dates.monthEndExclusive(ym)
            val inMonth = txns.filter { it.occurredAt in start until end }
            return MonthSummary(
                yearMonth = ym,
                incomeCents = inMonth.filter { it.kind == TxnKind.INCOME }.sumOf { it.amountCents },
                expenseCents = inMonth.filter { it.kind == TxnKind.EXPENSE }.sumOf { it.amountCents },
            )
        }

        fun slices(txns: List<Txn>, kind: TxnKind, categories: List<Category>): List<CategorySlice> {
            val grouped = txns.filter { it.kind == kind }.groupBy { it.categoryId }
            val total = grouped.values.sumOf { list -> list.sumOf { it.amountCents } }.coerceAtLeast(1)
            return grouped.mapNotNull { (id, list) ->
                val cat = categories.find { it.id == id } ?: return@mapNotNull null
                val amount = list.sumOf { it.amountCents }
                CategorySlice(cat, amount, amount.toFloat() / total)
            }.sortedByDescending { it.amountCents }
        }

        fun budgetStatuses(
            budgets: List<Budget>,
            monthTxns: List<Txn>,
            categories: List<Category>,
        ): List<BudgetStatus> {
            val expense = monthTxns.filter { it.kind == TxnKind.EXPENSE }
            return budgets.map { b ->
                val spent = if (b.categoryId == null) {
                    expense.sumOf { it.amountCents }
                } else {
                    expense.filter { it.categoryId == b.categoryId }.sumOf { it.amountCents }
                }
                BudgetStatus(b, spent, categories.find { it.id == b.categoryId })
            }
        }

        fun filterTxns(
            txns: List<Txn>,
            query: String,
            kind: TxnKind?,
            accountId: Long?,
            categoryId: Long?,
            tag: String?,
            startDate: LocalDate?,
            endDate: LocalDate?,
        ): List<Txn> {
            val q = query.trim()
            return txns.filter { t ->
                val qOk = q.isEmpty() ||
                    t.note.contains(q, true) ||
                    t.categoryName.contains(q, true) ||
                    t.accountName.contains(q, true) ||
                    t.tags.any { it.contains(q, true) } ||
                    t.amountCents.toString().contains(q) ||
                    (t.amountCents / 100.0).toString().contains(q)
                val kindOk = kind == null || t.kind == kind
                val accOk = accountId == null || t.accountId == accountId
                val catOk = categoryId == null || t.categoryId == categoryId
                val tagOk = tag.isNullOrBlank() || t.tags.any { it.equals(tag, true) }
                val startOk = startDate == null || !t.date.isBefore(startDate)
                val endOk = endDate == null || !t.date.isAfter(endDate)
                qOk && kindOk && accOk && catOk && tagOk && startOk && endOk
            }
        }
    }
}

private fun TransactionEntity.signed(): Long =
    if (kind == TxnKind.INCOME.name) amountCents else -amountCents

private fun AccountEntity.toDomain(balance: Long) = Account(
    id, name, AccountType.fromRaw(type), initialBalanceCents, colorArgb, archived, sortOrder, balance,
)

private fun CategoryEntity.toDomain() = Category(
    id, name, TxnKind.fromRaw(kind), colorArgb, emoji, archived, sortOrder,
)

private fun BudgetEntity.toDomain() = Budget(id, categoryId, amountCents)

private fun RecurringRuleEntity.toDomain(cat: CategoryEntity?, acc: AccountEntity?) = RecurringRule(
    id = id,
    amountCents = amountCents,
    kind = TxnKind.fromRaw(kind),
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    frequency = RecurringFrequency.fromRaw(frequency),
    startDate = java.time.LocalDate.ofEpochDay(startDateEpochDay),
    endDate = endDateEpochDay?.let { java.time.LocalDate.ofEpochDay(it) },
    maxCount = maxCount,
    generatedCount = generatedCount,
    lastGeneratedDate = lastGeneratedEpochDay?.let { java.time.LocalDate.ofEpochDay(it) },
    paused = paused,
    categoryName = cat?.name.orEmpty(),
    categoryColor = cat?.colorArgb ?: 0,
    categoryEmoji = cat?.emoji.orEmpty(),
    accountName = acc?.name.orEmpty(),
)

private fun TransactionEntity.toDomain(cat: CategoryEntity?, acc: AccountEntity?) = Txn(
    id = id,
    amountCents = amountCents,
    kind = TxnKind.fromRaw(kind),
    occurredAt = occurredAt,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    tags = tags.parseTags(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    categoryName = cat?.name.orEmpty(),
    categoryColor = cat?.colorArgb ?: 0,
    categoryEmoji = cat?.emoji.orEmpty(),
    accountName = acc?.name.orEmpty(),
    accountColor = acc?.colorArgb ?: 0,
    receiptPath = receiptPath,
    recurringRuleId = recurringRuleId,
)

data class BackupSnapshot(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val recurring: List<RecurringRuleEntity> = emptyList(),
)
