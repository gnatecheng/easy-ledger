package com.qingjizhang.app.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qingjizhang.app.AppContainer
import com.qingjizhang.app.data.AppSettings
import com.qingjizhang.app.domain.Account
import com.qingjizhang.app.domain.Budget
import com.qingjizhang.app.domain.BudgetStatus
import com.qingjizhang.app.domain.Category
import com.qingjizhang.app.domain.Dates
import com.qingjizhang.app.domain.Money
import com.qingjizhang.app.domain.MonthSummary
import com.qingjizhang.app.domain.Txn
import com.qingjizhang.app.domain.TxnKind
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.components.AppCard
import com.qingjizhang.app.ui.components.Banner
import com.qingjizhang.app.ui.components.BudgetBar
import com.qingjizhang.app.ui.components.ColorDot
import com.qingjizhang.app.ui.components.DeltaChip
import com.qingjizhang.app.ui.components.EmptyHint
import com.qingjizhang.app.ui.components.MonthSwitcher
import com.qingjizhang.app.ui.components.MoneyText
import com.qingjizhang.app.ui.components.SectionTitle
import com.qingjizhang.app.ui.components.TxnRow
import com.qingjizhang.app.ui.theme.ExpenseSoft
import com.qingjizhang.app.ui.theme.IncomeSoft
import com.qingjizhang.app.ui.theme.InkMuted
import com.qingjizhang.app.ui.theme.Over
import com.qingjizhang.app.ui.theme.OverSoft
import com.qingjizhang.app.ui.theme.Warn
import com.qingjizhang.app.ui.theme.WarnSoft
import com.qingjizhang.app.ui.vmFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class HomeUi(
    val month: YearMonth = YearMonth.now(),
    val summary: MonthSummary = MonthSummary(YearMonth.now(), 0, 0),
    val prev: MonthSummary = MonthSummary(YearMonth.now().minusMonths(1), 0, 0),
    val recent: List<Txn> = emptyList(),
    val largeTxns: List<Txn> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val lastTxnAt: Long? = null,
)

class HomeViewModel(private val app: AppContainer) : ViewModel() {
    val month = MutableStateFlow(YearMonth.now())
    private val dismissed = MutableStateFlow(setOf<String>())

    val ui = combine(
        combine(
            month,
            app.repo.observeTransactions(),
            app.repo.observeAccounts(),
            app.repo.observeBudgets(),
            app.repo.observeCategories(),
        ) { ym, txns, accounts, budgets, cats ->
            Tuple(ym, txns, accounts, budgets, cats)
        },
        app.settings.settings,
    ) { t, settings ->
        val start = Dates.monthStart(t.ym)
        val end = Dates.monthEndExclusive(t.ym)
        val monthTxns = t.txns.filter { it.occurredAt in start until end }
        HomeUi(
            month = t.ym,
            summary = com.qingjizhang.app.data.FinanceRepository.monthSummary(t.txns, t.ym),
            prev = com.qingjizhang.app.data.FinanceRepository.monthSummary(t.txns, t.ym.minusMonths(1)),
            recent = monthTxns.take(8),
            largeTxns = monthTxns.filter { it.kind == TxnKind.EXPENSE && it.amountCents >= settings.largeTxnThresholdCents },
            accounts = t.accounts.filter { !it.archived },
            budgetStatuses = com.qingjizhang.app.data.FinanceRepository.budgetStatuses(t.budgets, monthTxns, t.cats),
            settings = settings,
            lastTxnAt = t.txns.maxOfOrNull { it.occurredAt },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())

    private data class Tuple(
        val ym: YearMonth,
        val txns: List<Txn>,
        val accounts: List<Account>,
        val budgets: List<Budget>,
        val cats: List<Category>,
    )

    val dismissedKeys = dismissed.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun dismiss(key: String) {
        dismissed.value = dismissed.value + key
    }

    fun dismissNudge() {
        viewModelScope.launch {
            app.settings.update { it.copy(nudgeDismissedAt = System.currentTimeMillis()) }
        }
    }

    companion object {
        fun factory(app: AppContainer) = vmFactory { HomeViewModel(app) }
    }
}

// combine with 6 flows uses Array; keep a named helper if compiler complains.

@Composable
fun HomeScreen(
    onOpenTxn: (Long) -> Unit,
    onQuickAdd: () -> Unit,
    onOpenTxns: () -> Unit,
    onOpenBudgets: () -> Unit,
) {
    val app = LocalApp.current
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val ui by vm.ui.collectAsState()
    val dismissed by vm.dismissedKeys.collectAsState()

    val alerts = FinanceAlerts.build(ui, dismissed)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onQuickAdd) {
                Icon(Icons.Default.Add, contentDescription = "记一笔")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("轻记账", style = MaterialTheme.typography.headlineMedium)
                Text("把每一笔都记清楚", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                MonthSwitcher(ui.month, onChange = { vm.month.value = it })
            }
            items(alerts, key = { it.key }) { alert ->
                Banner(
                    title = alert.title,
                    body = alert.body,
                    container = alert.container,
                    content = alert.content,
                    onDismiss = {
                        vm.dismiss(alert.key)
                        if (alert.key == "nudge") vm.dismissNudge()
                    },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("本月支出", ui.summary.expenseCents, TxnKind.EXPENSE, ui.prev.expenseCents, Modifier.weight(1f))
                    MiniStat("本月收入", ui.summary.incomeCents, TxnKind.INCOME, ui.prev.incomeCents, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                AppCard {
                    Text("结余", color = InkMuted)
                    Spacer(Modifier.height(4.dp))
                    MoneyText(ui.summary.balanceCents, large = true)
                    DeltaChip(ui.summary.balanceCents, ui.prev.balanceCents)
                }
            }
            if (ui.budgetStatuses.isNotEmpty()) {
                item {
                    AppCard(onClick = onOpenBudgets) {
                        SectionTitle("预算进度")
                        ui.budgetStatuses.take(4).forEach { BudgetBar(it) }
                    }
                }
            }
            item {
                Text("账户", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.accounts.forEach { acc ->
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ColorDot(acc.colorArgb)
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text(acc.name)
                                        Text(Money.formatYuan(acc.balanceCents, withSign = true), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            },
                        )
                    }
                }
            }
            item {
                SectionTitle("本月流水") {
                    TextButton(onClick = onOpenTxns) { Text("全部") }
                }
            }
            if (ui.recent.isEmpty()) {
                item { EmptyHint("这个月还没有记账，点右下角记一笔") }
            } else {
                items(ui.recent, key = { it.id }) { txn ->
                    AppCard(onClick = { onOpenTxn(txn.id) }) {
                        TxnRow(txn) { onOpenTxn(txn.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(
    title: String,
    cents: Long,
    kind: TxnKind,
    prev: Long,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier) {
        Text(title, color = InkMuted)
        Spacer(Modifier.height(4.dp))
        MoneyText(cents, kind, large = true)
        DeltaChip(cents, prev)
    }
}

object FinanceAlerts {
    data class Item(
        val key: String,
        val title: String,
        val body: String,
        val container: androidx.compose.ui.graphics.Color,
        val content: androidx.compose.ui.graphics.Color,
    )

    fun build(ui: HomeUi, dismissed: Set<String>): List<Item> {
        val items = mutableListOf<Item>()
        ui.budgetStatuses.filter { it.over }.forEach { s ->
            val key = "over-${s.budget.id}"
            if (key !in dismissed) {
                items += Item(
                    key, "预算已超支",
                    "${s.category?.name ?: "总预算"}本月已花 ${Money.formatYuan(s.spentCents)}，超出 ${Money.formatYuan(s.spentCents - s.budget.amountCents)}",
                    OverSoft, Over,
                )
            }
        }
        ui.budgetStatuses.filter { it.warn }.forEach { s ->
            val key = "warn-${s.budget.id}"
            if (key !in dismissed) {
                items += Item(
                    key, "预算即将用完",
                    "${s.category?.name ?: "总预算"}已使用 ${(s.ratio * 100).toInt()}%，请留意开支",
                    WarnSoft, Warn,
                )
            }
        }
        ui.largeTxns.take(2).forEach { t ->
            val key = "large-${t.id}"
            if (key !in dismissed) {
                items += Item(
                    key, "大额支出提醒",
                    "${t.categoryName} ${Money.formatYuan(t.amountCents)}（阈值 ${Money.formatYuan(ui.settings.largeTxnThresholdCents)}）",
                    ExpenseSoft, com.qingjizhang.app.ui.theme.Expense,
                )
            }
        }
        val last = ui.lastTxnAt
        val days = ui.settings.inactivityNudgeDays
        if (last != null && days > 0 && "nudge" !in dismissed) {
            val lastDate = Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault()).toLocalDate()
            val gap = ChronoUnit.DAYS.between(lastDate, LocalDate.now())
            val dismissedAt = ui.settings.nudgeDismissedAt
            val recentlyDismissed = dismissedAt > 0 &&
                ChronoUnit.DAYS.between(
                    Instant.ofEpochMilli(dismissedAt).atZone(ZoneId.systemDefault()).toLocalDate(),
                    LocalDate.now(),
                ) < 1
            if (gap >= days && !recentlyDismissed) {
                items += Item(
                    "nudge", "好久没记账了",
                    "已经 $gap 天没有新的记录，花 10 秒补一笔吧",
                    IncomeSoft, com.qingjizhang.app.ui.theme.TealDark,
                )
            }
        }
        return items
    }
}
