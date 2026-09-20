package com.qingjizhang.app.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qingjizhang.app.AppContainer
import com.qingjizhang.app.domain.Dates
import com.qingjizhang.app.domain.Money
import com.qingjizhang.app.domain.RecurringDates
import com.qingjizhang.app.domain.RecurringFrequency
import com.qingjizhang.app.domain.RecurringRule
import com.qingjizhang.app.domain.TxnKind
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.components.AppCard
import com.qingjizhang.app.ui.components.ColorDot
import com.qingjizhang.app.ui.components.EmptyHint
import com.qingjizhang.app.ui.components.MoneyText
import com.qingjizhang.app.ui.theme.InkMuted
import com.qingjizhang.app.ui.vmFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class RecurringViewModel(private val app: AppContainer) : ViewModel() {
    val rules = app.repo.observeRecurring().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = app.repo.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accounts = app.repo.observeAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun load(id: Long): RecurringRule? = if (id <= 0) null else app.repo.getRecurring(id)

    fun save(rule: RecurringRule, onDone: () -> Unit) {
        viewModelScope.launch {
            app.repo.upsertRecurring(rule)
            onDone()
        }
    }

    fun pause(id: Long, paused: Boolean) {
        viewModelScope.launch { app.repo.setRecurringPaused(id, paused) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { app.repo.deleteRecurring(id) }
    }

    companion object {
        fun factory(app: AppContainer) = vmFactory { RecurringViewModel(app) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val app = LocalApp.current
    val vm: RecurringViewModel = viewModel(factory = RecurringViewModel.factory(app))
    val rules by vm.rules.collectAsState()
    var confirmId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("周期记账") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEdit(0) }) {
                Icon(Icons.Default.Add, contentDescription = "新建规则")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("打开应用时会自动补上到期流水。已暂停的规则不会生成。", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
            }
            if (rules.isEmpty()) {
                item { EmptyHint("还没有周期规则，点右下角添加房租、工资等") }
            }
            items(rules, key = { it.id }) { rule ->
                AppCard(onClick = { onEdit(rule.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(rule.categoryColor, size = 12)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${rule.categoryEmoji} ${rule.categoryName}", fontWeight = FontWeight.Medium)
                            Text("${rule.frequency.label} · ${rule.accountName}", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                            val next = if (rule.paused) null else {
                                val candidate = rule.lastGeneratedDate?.let {
                                    RecurringDates.next(it, rule.frequency, rule.startDate)
                                } ?: rule.startDate
                                candidate.takeUnless { rule.endDate != null && it.isAfter(rule.endDate) }
                            }
                            Text(
                                when {
                                    rule.paused -> "已暂停"
                                    next == null -> "已结束"
                                    else -> "下次 ${next.format(Dates.dateCn)}"
                                },
                                color = InkMuted,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        MoneyText(rule.amountCents, rule.kind)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (rule.paused) "已暂停" else "启用中", modifier = Modifier.weight(1f), color = InkMuted)
                        Switch(checked = !rule.paused, onCheckedChange = { vm.pause(rule.id, !it) })
                        TextButton(onClick = { confirmId = rule.id }) { Text("删除") }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    confirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmId = null },
            title = { Text("删除这条周期规则？") },
            text = { Text("已经生成的流水会保留，只是不再自动记账。") },
            confirmButton = {
                TextButton(onClick = { vm.delete(id); confirmId = null }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmId = null }) { Text("取消") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringEditScreen(ruleId: Long, onBack: () -> Unit) {
    val app = LocalApp.current
    val vm: RecurringViewModel = viewModel(factory = RecurringViewModel.factory(app))
    val cats by vm.categories.collectAsState()
    val accs by vm.accounts.collectAsState()
    var kind by remember { mutableStateOf(TxnKind.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(0L) }
    var accountId by remember { mutableStateOf(0L) }
    var note by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
    var start by remember { mutableStateOf(LocalDate.now()) }
    var end by remember { mutableStateOf<LocalDate?>(null) }
    var useEnd by remember { mutableStateOf(false) }
    var maxCount by remember { mutableStateOf("") }
    var paused by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    val existingId = ruleId.takeIf { it > 0 }

    LaunchedEffect(ruleId, cats, accs) {
        val existing = vm.load(ruleId)
        if (existing != null) {
            kind = existing.kind
            amount = Money.formatPlain(existing.amountCents)
            categoryId = existing.categoryId
            accountId = existing.accountId
            note = existing.note
            frequency = existing.frequency
            start = existing.startDate
            end = existing.endDate
            useEnd = existing.endDate != null
            maxCount = existing.maxCount?.toString().orEmpty()
            paused = existing.paused
        } else {
            if (categoryId == 0L) categoryId = cats.firstOrNull { it.kind == kind && !it.archived }?.id ?: 0
            if (accountId == 0L) accountId = accs.firstOrNull { !it.archived }?.id ?: 0
        }
    }

    val filtered = cats.filter { it.kind == kind && !it.archived }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingId == null) "新建周期规则" else "编辑周期规则") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TxnKind.ledger.forEachIndexed { i, k ->
                    SegmentedButton(
                        selected = kind == k,
                        onClick = {
                            kind = k
                            categoryId = cats.firstOrNull { it.kind == k && !it.archived }?.id ?: 0
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, TxnKind.ledger.size),
                    ) { Text(k.label) }
                }
            }
            OutlinedTextField(
                amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                modifier = Modifier.fillMaxWidth(), label = { Text("金额") }, prefix = { Text("¥") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
            )
            Text("频率", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                RecurringFrequency.entries.forEachIndexed { i, f ->
                    SegmentedButton(
                        selected = frequency == f,
                        onClick = { frequency = f },
                        shape = SegmentedButtonDefaults.itemShape(i, RecurringFrequency.entries.size),
                    ) { Text(f.label) }
                }
            }
            Text("分类", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filtered.forEach { c ->
                    FilterChip(
                        selected = categoryId == c.id,
                        onClick = { categoryId = c.id },
                        label = { Text("${c.emoji} ${c.name}") },
                    )
                }
            }
            Text("账户", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accs.filter { !it.archived }.forEach { a ->
                    FilterChip(selected = accountId == a.id, onClick = { accountId = a.id }, label = { Text(a.name) })
                }
            }
            OutlinedButton(onClick = { pickStart = true }, modifier = Modifier.fillMaxWidth()) {
                Text("开始日期 ${start.format(Dates.dateCn)}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("设置结束日期", modifier = Modifier.weight(1f))
                Switch(checked = useEnd, onCheckedChange = {
                    useEnd = it
                    if (it && end == null) end = start.plusMonths(12)
                })
            }
            if (useEnd) {
                OutlinedButton(onClick = { pickEnd = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("结束日期 ${(end ?: start).format(Dates.dateCn)}")
                }
            }
            OutlinedTextField(
                maxCount, { maxCount = it.filter { ch -> ch.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("次数上限（可空）") },
                placeholder = { Text("留空表示一直重复") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("备注") })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("暂停此规则", modifier = Modifier.weight(1f))
                Switch(checked = paused, onCheckedChange = { paused = it })
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val cents = Money.parseYuan(amount)
                    when {
                        cents == null || cents <= 0 -> error = "请输入有效金额"
                        categoryId == 0L -> error = "请选择分类"
                        accountId == 0L -> error = "请选择账户"
                        else -> {
                            error = null
                            vm.save(
                                RecurringRule(
                                    id = existingId ?: 0,
                                    amountCents = cents,
                                    kind = kind,
                                    categoryId = categoryId,
                                    accountId = accountId,
                                    note = note,
                                    frequency = frequency,
                                    startDate = start,
                                    endDate = end.takeIf { useEnd },
                                    maxCount = maxCount.toIntOrNull(),
                                    generatedCount = 0,
                                    lastGeneratedDate = null,
                                    paused = paused,
                                ),
                                onBack,
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存并生成到期流水") }
        }
    }
    if (pickStart || pickEnd) {
        val isStart = pickStart
        val initial = (if (isStart) start else (end ?: start)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { pickStart = false; pickEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        if (isStart) start = d else end = d
                    }
                    pickStart = false
                    pickEnd = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { pickStart = false; pickEnd = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }
}
