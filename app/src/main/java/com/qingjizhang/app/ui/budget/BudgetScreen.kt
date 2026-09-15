package com.qingjizhang.app.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qingjizhang.app.AppContainer
import com.qingjizhang.app.data.FinanceRepository
import com.qingjizhang.app.domain.Budget
import com.qingjizhang.app.domain.BudgetStatus
import com.qingjizhang.app.domain.Category
import com.qingjizhang.app.domain.Dates
import com.qingjizhang.app.domain.Money
import com.qingjizhang.app.domain.TxnKind
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.components.AppCard
import com.qingjizhang.app.ui.components.BudgetBar
import com.qingjizhang.app.ui.components.MonthSwitcher
import com.qingjizhang.app.ui.theme.InkMuted
import com.qingjizhang.app.ui.vmFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class BudgetUi(
    val month: YearMonth = YearMonth.now(),
    val statuses: List<BudgetStatus> = emptyList(),
    val expenseCats: List<Category> = emptyList(),
)

class BudgetViewModel(private val app: AppContainer) : ViewModel() {
    val month = MutableStateFlow(YearMonth.now())

    val ui = combine(
        month,
        app.repo.observeBudgets(),
        app.repo.observeTransactions(),
        app.repo.observeCategories(),
    ) { ym, budgets, txns, cats ->
        val start = Dates.monthStart(ym)
        val end = Dates.monthEndExclusive(ym)
        val monthTxns = txns.filter { it.occurredAt in start until end }
        BudgetUi(
            month = ym,
            statuses = FinanceRepository.budgetStatuses(budgets, monthTxns, cats),
            expenseCats = cats.filter { it.kind == TxnKind.EXPENSE && !it.archived },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUi())

    fun save(categoryId: Long?, amountYuan: String) {
        val cents = Money.parseYuan(amountYuan) ?: return
        viewModelScope.launch {
            app.repo.upsertBudget(Budget(0, categoryId, cents))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { app.repo.deleteBudget(id) }
    }

    companion object {
        fun factory(app: AppContainer) = vmFactory { BudgetViewModel(app) }
    }
}

@Composable
fun BudgetScreen() {
    val app = LocalApp.current
    val vm: BudgetViewModel = viewModel(factory = BudgetViewModel.factory(app))
    val ui by vm.ui.collectAsState()
    var editor by remember { mutableStateOf<Pair<Long?, String>?>(null) }

    val total = ui.statuses.find { it.budget.categoryId == null }
    val cats = ui.statuses.filter { it.budget.categoryId != null }
    val usedIds = cats.mapNotNull { it.budget.categoryId }.toSet()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editor = 0L to "" }) {
                Icon(Icons.Default.Add, contentDescription = "添加分类预算")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("预算", style = MaterialTheme.typography.headlineMedium)
            Text("80% 提醒，100% 超支告警会显示在首页", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
            MonthSwitcher(ui.month) { vm.month.value = it }
            AppCard(onClick = { editor = null to Money.formatPlain(total?.budget?.amountCents ?: 0) }) {
                Text("每月总预算", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (total != null) BudgetBar(total) else Text("点此设置总预算", color = InkMuted)
            }
            AppCard {
                Text("分类预算", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                if (cats.isEmpty()) {
                    Text("还没有分类预算，点右下角添加", color = InkMuted)
                } else {
                    cats.forEach { status ->
                        BudgetBar(status)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                editor = status.budget.categoryId to Money.formatPlain(status.budget.amountCents)
                            }) { Text("编辑") }
                            TextButton(onClick = { vm.delete(status.budget.id) }) { Text("删除") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }

    val currentEditor = editor
    if (currentEditor != null) {
        val isTotal = currentEditor.first == null
        var amount by remember(currentEditor) { mutableStateOf(currentEditor.second) }
        var catId by remember(currentEditor) {
            mutableStateOf(currentEditor.first?.takeIf { it != 0L } ?: ui.expenseCats.firstOrNull { it.id !in usedIds }?.id)
        }
        AlertDialog(
            onDismissRequest = { editor = null },
            title = { Text(if (isTotal) "每月总预算" else "分类预算") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isTotal) {
                        Text("选择分类", color = InkMuted)
                        ui.expenseCats.forEach { c ->
                            FilterChip(
                                selected = catId == c.id,
                                onClick = { catId = c.id },
                                label = { Text("${c.emoji} ${c.name}") },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("金额") },
                        prefix = { Text("¥") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.save(if (isTotal) null else catId, amount)
                    editor = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editor = null }) { Text("取消") } },
        )
    }
}
