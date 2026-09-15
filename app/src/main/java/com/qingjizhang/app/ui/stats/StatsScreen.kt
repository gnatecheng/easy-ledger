package com.qingjizhang.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qingjizhang.app.AppContainer
import com.qingjizhang.app.data.FinanceRepository
import com.qingjizhang.app.domain.Account
import com.qingjizhang.app.domain.CategorySlice
import com.qingjizhang.app.domain.Dates
import com.qingjizhang.app.domain.MonthSummary
import com.qingjizhang.app.domain.Txn
import com.qingjizhang.app.domain.TxnKind
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.components.AppCard
import com.qingjizhang.app.ui.components.CategoryPieChart
import com.qingjizhang.app.ui.components.DeltaChip
import com.qingjizhang.app.ui.components.MoneyText
import com.qingjizhang.app.ui.components.TrendChart
import com.qingjizhang.app.ui.theme.InkMuted
import com.qingjizhang.app.ui.vmFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

enum class RangePreset { THIS_MONTH, LAST_3, LAST_12, CUSTOM }

data class StatsUi(
    val preset: RangePreset = RangePreset.THIS_MONTH,
    val start: LocalDate = YearMonth.now().atDay(1),
    val end: LocalDate = LocalDate.now(),
    val kind: TxnKind = TxnKind.EXPENSE,
    val accountId: Long? = null,
    val tag: String? = null,
    val accounts: List<Account> = emptyList(),
    val tags: List<String> = emptyList(),
    val filtered: List<Txn> = emptyList(),
    val slices: List<CategorySlice> = emptyList(),
    val trend: List<MonthSummary> = emptyList(),
    val income: Long = 0,
    val expense: Long = 0,
    val prevIncome: Long = 0,
    val prevExpense: Long = 0,
)

class StatsViewModel(app: AppContainer) : ViewModel() {
    val preset = MutableStateFlow(RangePreset.THIS_MONTH)
    val customStart = MutableStateFlow(YearMonth.now().atDay(1))
    val customEnd = MutableStateFlow(LocalDate.now())
    val kind = MutableStateFlow(TxnKind.EXPENSE)
    val accountId = MutableStateFlow<Long?>(null)
    val tag = MutableStateFlow<String?>(null)

    val ui = combine(
        combine(preset, customStart, customEnd, kind, accountId) { p, s, e, k, a ->
            Filter(p, s, e, k, a)
        },
        tag,
        app.repo.observeTransactions(),
        app.repo.observeAccounts(),
        app.repo.observeCategories(),
    ) { f, tagValue, txns, accounts, cats ->
        val (start, end) = rangeOf(f.preset, f.start, f.end)
        val prevStart = start.minusDays(Chrono.daysBetween(start, end) + 1)
        val prevEnd = start.minusDays(1)
        val filtered = FinanceRepository.filterTxns(txns, "", null, f.accountId, null, tagValue, start, end)
        val prev = FinanceRepository.filterTxns(txns, "", null, f.accountId, null, tagValue, prevStart, prevEnd)
        val months = (0..11).map { YearMonth.now().minusMonths((11 - it).toLong()) }.map { ym ->
            val inMonth = FinanceRepository.filterTxns(
                txns, "", null, f.accountId, null, tagValue, ym.atDay(1), ym.atEndOfMonth(),
            )
            MonthSummary(
                ym,
                inMonth.filter { it.kind == TxnKind.INCOME }.sumOf { it.amountCents },
                inMonth.filter { it.kind == TxnKind.EXPENSE }.sumOf { it.amountCents },
            )
        }
        StatsUi(
            preset = f.preset,
            start = start,
            end = end,
            kind = f.kind,
            accountId = f.accountId,
            tag = tagValue,
            accounts = accounts.filter { !it.archived },
            tags = txns.flatMap { it.tags }.distinct().sorted(),
            filtered = filtered,
            slices = FinanceRepository.slices(filtered, f.kind, cats),
            trend = months,
            income = filtered.filter { it.kind == TxnKind.INCOME }.sumOf { it.amountCents },
            expense = filtered.filter { it.kind == TxnKind.EXPENSE }.sumOf { it.amountCents },
            prevIncome = prev.filter { it.kind == TxnKind.INCOME }.sumOf { it.amountCents },
            prevExpense = prev.filter { it.kind == TxnKind.EXPENSE }.sumOf { it.amountCents },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUi())

    private data class Filter(
        val preset: RangePreset,
        val start: LocalDate,
        val end: LocalDate,
        val kind: TxnKind,
        val accountId: Long?,
    )

    companion object {
        fun factory(app: AppContainer) = vmFactory { StatsViewModel(app) }

        fun rangeOf(preset: RangePreset, start: LocalDate, end: LocalDate): Pair<LocalDate, LocalDate> {
            val today = LocalDate.now()
            return when (preset) {
                RangePreset.THIS_MONTH -> YearMonth.now().atDay(1) to today
                RangePreset.LAST_3 -> today.minusMonths(3).plusDays(1) to today
                RangePreset.LAST_12 -> today.minusMonths(12).plusDays(1) to today
                RangePreset.CUSTOM -> start to end
            }
        }
    }
}

private object Chrono {
    fun daysBetween(a: LocalDate, b: LocalDate): Long = java.time.temporal.ChronoUnit.DAYS.between(a, b)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val app = LocalApp.current
    val vm: StatsViewModel = viewModel(factory = StatsViewModel.factory(app))
    val ui by vm.ui.collectAsState()
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("统计", style = MaterialTheme.typography.headlineMedium)
        Text("看看钱都去哪了", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(ui.preset == RangePreset.THIS_MONTH, { vm.preset.value = RangePreset.THIS_MONTH }, label = { Text("本月") })
            FilterChip(ui.preset == RangePreset.LAST_3, { vm.preset.value = RangePreset.LAST_3 }, label = { Text("近3月") })
            FilterChip(ui.preset == RangePreset.LAST_12, { vm.preset.value = RangePreset.LAST_12 }, label = { Text("近12月") })
            FilterChip(ui.preset == RangePreset.CUSTOM, { vm.preset.value = RangePreset.CUSTOM }, label = { Text("自定义") })
        }
        if (ui.preset == RangePreset.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.weight(1f)) { Text("从 ${ui.start}") }
                OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.weight(1f)) { Text("到 ${ui.end}") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(ui.kind == TxnKind.EXPENSE, { vm.kind.value = TxnKind.EXPENSE }, label = { Text("支出构成") })
            FilterChip(ui.kind == TxnKind.INCOME, { vm.kind.value = TxnKind.INCOME }, label = { Text("收入构成") })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(ui.accountId == null, { vm.accountId.value = null }, label = { Text("全部账户") })
            ui.accounts.take(6).forEach { acc ->
                FilterChip(
                    selected = ui.accountId == acc.id,
                    onClick = { vm.accountId.value = if (ui.accountId == acc.id) null else acc.id },
                    label = { Text(acc.name) },
                )
            }
        }
        if (ui.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(ui.tag == null, { vm.tag.value = null }, label = { Text("全部标签") })
                ui.tags.forEach { t ->
                    FilterChip(ui.tag == t, { vm.tag.value = if (ui.tag == t) null else t }, label = { Text(t) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppCard(Modifier.weight(1f)) {
                Text("支出", color = InkMuted)
                MoneyText(ui.expense, TxnKind.EXPENSE, large = true)
                DeltaChip(ui.expense, ui.prevExpense)
            }
            AppCard(Modifier.weight(1f)) {
                Text("收入", color = InkMuted)
                MoneyText(ui.income, TxnKind.INCOME, large = true)
                DeltaChip(ui.income, ui.prevIncome)
            }
        }
        AppCard {
            Text("结余", color = InkMuted)
            MoneyText(ui.income - ui.expense, large = true)
            Text("${ui.start} 至 ${ui.end} · ${ui.filtered.size} 笔", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
        AppCard {
            Text("分类占比", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            CategoryPieChart(ui.slices, centerLabel = ui.kind.label)
        }
        AppCard {
            Text("近 12 个月趋势", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            TrendChart(ui.trend)
        }
        Spacer(Modifier.height(72.dp))
    }

    if (pickingStart || pickingEnd) {
        val isStart = pickingStart
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (if (isStart) ui.start else ui.end)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingStart = false; pickingEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    val d = state.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    if (d != null) {
                        if (isStart) vm.customStart.value = d else vm.customEnd.value = d
                    }
                    pickingStart = false
                    pickingEnd = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { pickingStart = false; pickingEnd = false }) { Text("取消") }
            },
        ) { DatePicker(state = state) }
    }
}
