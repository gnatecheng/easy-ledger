package com.qingjizhang.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val initialBalanceCents: Long = 0,
    val colorArgb: Int,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,
    val colorArgb: Int,
    val emoji: String = "",
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "transactions",
    indices = [Index("accountId"), Index("categoryId"), Index("occurredAt"), Index("transferToAccountId")],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val kind: String,
    val occurredAt: Long,
    val categoryId: Long,
    val accountId: Long,
    val note: String = "",
    val tags: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val receiptPath: String? = null,
    val recurringRuleId: Long? = null,
    val transferToAccountId: Long? = null,
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    val amountCents: Long,
)

@Entity(
    tableName = "recurring_rules",
    indices = [Index("accountId"), Index("categoryId")],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val kind: String,
    val categoryId: Long,
    val accountId: Long,
    val note: String = "",
    val frequency: String,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long? = null,
    val maxCount: Int? = null,
    val generatedCount: Int = 0,
    val lastGeneratedEpochDay: Long? = null,
    val paused: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
