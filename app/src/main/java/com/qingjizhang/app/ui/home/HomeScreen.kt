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
import androidx.compose.material.icons.outlined.SwapHoriz
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
import androidx.compose.ui.platform.LocalContext
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
import com.qingjizhang.app.domain.CategorySlice
import com.qingjizhang.app.domain.Dates
import com.qingjizhang.app.domain.MonthSummary
import com.qingjizhang.app.domain.Txn
import com.qingjizhang.app.domain.TxnKind
import com.qingjizhang.app.R
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.i18n.AppFormatters
import com.qingjizhang.app.ui.i18n.displayName
import com.qingjizhang.app.ui.i18n.localizedCategoryName
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
import com.qingjizhang.app.ui.share.MonthShare
import androidx.compose.ui.res.stringResource
import com.qingjizhang.app.ui.theme.financeAlertLargeExpenseColors
import com.qingjizhang.app.ui.theme.financeAlertNudgeColors
import com.qingjizhang.app.ui.theme.financeAlertOverColors
import com.qingjizhang.app.ui.theme.financeAlertWarnColors
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
    val slices: List<CategorySlice> = emptyList(),
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
            slices = com.qingjizhang.app.data.FinanceRepository.slices(monthTxns, TxnKind.EXPENSE, t.cats),
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
    onTransfer: () -> Unit = {},
) {
    val app = LocalApp.current
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val ui by vm.ui.collectAsState()
    val dismissed by vm.dismissedKeys.collectAsState()
    val context = LocalContext.current

    val alerts = FinanceAlerts.build(ui, dismissed)

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingActionButton(
                    onClick = onTransfer,
                    containerColor = com.qingjizhang.app.ui.theme.Transfer,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = stringResource(R.string.cd_transfer))
                }
                FloatingActionButton(onClick = onQuickAdd) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_quick_add))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.home_tagline),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                MonthSwitcher(ui.month, onChange = { vm.month.value = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { MonthShare.shareImage(context, ui.summary, ui.slices) }) {
                        Text(stringResource(R.string.share_month_report))
                    }
                    TextButton(onClick = { MonthShare.sharePdf(context, ui.summary, ui.slices) }) {
                        Text(stringResource(R.string.export_pdf))
                    }
                }
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
                    MiniStat(stringResource(R.string.month_expense), ui.summary.expenseCents, TxnKind.EXPENSE, ui.prev.expenseCents, Modifier.weight(1f))
                    MiniStat(stringResource(R.string.month_income), ui.summary.incomeCents, TxnKind.INCOME, ui.prev.incomeCents, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                AppCard {
                    Text(stringResource(R.string.balance), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    MoneyText(ui.summary.balanceCents, large = true)
                    DeltaChip(ui.summary.balanceCents, ui.prev.balanceCents)
                    if (ui.summary.transferCents > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.month_transfer_note, AppFormatters.formatYuan(ui.summary.transferCents)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            if (ui.budgetStatuses.isNotEmpty()) {
                item {
                    AppCard(onClick = onOpenBudgets) {
                        SectionTitle(stringResource(R.string.budget_progress))
                        ui.budgetStatuses.take(4).forEach { BudgetBar(it) }
                    }
                }
            }
            item {
                Text(stringResource(R.string.accounts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                                        Text(acc.displayName())
                                        Text(AppFormatters.formatYuan(acc.balanceCents, withSign = true), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            },
                        )
                    }
                }
            }
            item {
                SectionTitle(stringResource(R.string.month_txns)) {
                    TextButton(onClick = onOpenTxns) { Text(stringResource(R.string.action_all)) }
                }
            }
            if (ui.recent.isEmpty()) {
                item { EmptyHint(stringResource(R.string.empty_month_txns)) }
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
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    @Composable
    fun build(ui: HomeUi, dismissed: Set<String>): List<Item> {
        val items = mutableListOf<Item>()
        val totalBudgetLabel = stringResource(R.string.budget_total_label)
        ui.budgetStatuses.filter { it.over }.forEach { s ->
            val key = "over-${s.budget.id}"
            if (key !in dismissed) {
                val name = s.category?.displayName() ?: totalBudgetLabel
                val (container, content) = financeAlertOverColors()
                items += Item(
                    key,
                    stringResource(R.string.alert_budget_over_title),
                    stringResource(
                        R.string.alert_budget_over_body,
                        name,
                        AppFormatters.formatYuan(s.spentCents),
                        AppFormatters.formatYuan(s.spentCents - s.budget.amountCents),
                    ),
                    container,
                    content,
                )
            }
        }
        ui.budgetStatuses.filter { it.warn }.forEach { s ->
            val key = "warn-${s.budget.id}"
            if (key !in dismissed) {
                val name = s.category?.displayName() ?: totalBudgetLabel
                val (container, content) = financeAlertWarnColors()
                items += Item(
                    key,
                    stringResource(R.string.alert_budget_warn_title),
                    stringResource(R.string.alert_budget_warn_body, name, (s.ratio * 100).toInt()),
                    container,
                    content,
                )
            }
        }
        ui.largeTxns.take(2).forEach { t ->
            val key = "large-${t.id}"
            if (key !in dismissed) {
                val (container, content) = financeAlertLargeExpenseColors()
                items += Item(
                    key,
                    stringResource(R.string.alert_large_txn_title),
                    stringResource(
                        R.string.alert_large_txn_body,
                        localizedCategoryName(t.categoryName),
                        AppFormatters.formatYuan(t.amountCents),
                        AppFormatters.formatYuan(ui.settings.largeTxnThresholdCents),
                    ),
                    container,
                    content,
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
                val (container, content) = financeAlertNudgeColors()
                items += Item(
                    "nudge",
                    stringResource(R.string.alert_nudge_title),
                    stringResource(R.string.alert_nudge_body, gap.toInt()),
                    container,
                    content,
                )
            }
        }
        return items
    }
}
