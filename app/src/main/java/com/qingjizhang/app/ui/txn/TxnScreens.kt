package com.qingjizhang.app.ui.txn

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qingjizhang.app.AppContainer
import com.qingjizhang.app.data.FinanceRepository
import com.qingjizhang.app.domain.Account
import com.qingjizhang.app.domain.Category
import com.qingjizhang.app.domain.Dates
import com.qingjizhang.app.domain.Money
import com.qingjizhang.app.domain.Txn
import com.qingjizhang.app.domain.TxnKind
import com.qingjizhang.app.domain.parseTags
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.components.AppCard
import com.qingjizhang.app.ui.components.ColorDot
import com.qingjizhang.app.ui.components.EmptyHint
import com.qingjizhang.app.ui.components.MonthSwitcher
import com.qingjizhang.app.ui.components.MoneyText
import com.qingjizhang.app.ui.components.TxnRow
import com.qingjizhang.app.ui.theme.InkMuted
import com.qingjizhang.app.ui.vmFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

data class TxnListUi(
    val month: YearMonth = YearMonth.now(),
    val query: String = "",
    val kind: TxnKind? = null,
    val accountId: Long? = null,
    val items: List<Txn> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val dayGroups: List<Pair<LocalDate, List<Txn>>> = emptyList(),
)

class TxnListViewModel(app: AppContainer) : ViewModel() {
    val month = MutableStateFlow(YearMonth.now())
    val query = MutableStateFlow("")
    val kind = MutableStateFlow<TxnKind?>(null)
    val accountId = MutableStateFlow<Long?>(null)

    val ui = combine(
        combine(month, query, kind, accountId) { m, q, k, a -> Filter(m, q, k, a) },
        app.repo.observeTransactions(),
        app.repo.observeAccounts(),
    ) { f, txns, accounts ->
        val start = Dates.monthStart(f.month)
        val end = Dates.monthEndExclusive(f.month)
        val filtered = FinanceRepository.filterTxns(
            txns.filter { it.occurredAt in start until end },
            f.query, f.kind, f.accountId, null, null, null, null,
        )
        TxnListUi(
            month = f.month,
            query = f.query,
            kind = f.kind,
            accountId = f.accountId,
            items = filtered,
            accounts = accounts.filter { !it.archived },
            dayGroups = filtered.groupBy { it.date }.toList().sortedByDescending { it.first },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TxnListUi())

    private data class Filter(val month: YearMonth, val query: String, val kind: TxnKind?, val accountId: Long?)

    companion object {
        fun factory(app: AppContainer) = vmFactory { TxnListViewModel(app) }
    }
}

@Composable
fun TransactionListScreen(
    onOpenTxn: (Long) -> Unit,
    onQuickAdd: () -> Unit,
) {
    val app = LocalApp.current
    val vm: TxnListViewModel = viewModel(factory = TxnListViewModel.factory(app))
    val ui by vm.ui.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onQuickAdd) {
                Icon(Icons.Default.Add, contentDescription = "记一笔")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("明细", style = MaterialTheme.typography.headlineMedium)
                MonthSwitcher(ui.month) { vm.month.value = it }
                OutlinedTextField(
                    value = ui.query,
                    onValueChange = { vm.query.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索备注、分类、标签、金额") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = ui.kind == null, onClick = { vm.kind.value = null }, label = { Text("全部") })
                    FilterChip(selected = ui.kind == TxnKind.EXPENSE, onClick = { vm.kind.value = TxnKind.EXPENSE }, label = { Text("支出") })
                    FilterChip(selected = ui.kind == TxnKind.INCOME, onClick = { vm.kind.value = TxnKind.INCOME }, label = { Text("收入") })
                }
                Row(
                    Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = ui.accountId == null, onClick = { vm.accountId.value = null }, label = { Text("全部账户") })
                    ui.accounts.forEach { acc ->
                        FilterChip(
                            selected = ui.accountId == acc.id,
                            onClick = { vm.accountId.value = if (ui.accountId == acc.id) null else acc.id },
                            label = { Text(acc.name) },
                        )
                    }
                }
            }
            if (ui.dayGroups.isEmpty()) {
                item { EmptyHint("没有符合条件的流水") }
            }
            ui.dayGroups.forEach { (day, list) ->
                item(key = "h-$day") {
                    val sub = list.sumOf { it.signedCents }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(day.format(Dates.dayCn), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        MoneyText(sub)
                    }
                }
                items(list, key = { it.id }) { txn ->
                    AppCard(onClick = { onOpenTxn(txn.id) }) { TxnRow(txn) { onOpenTxn(txn.id) } }
                }
            }
        }
    }
}

class EditorViewModel(private val app: AppContainer) : ViewModel() {
    val categories = app.repo.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accounts = app.repo.observeAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun load(id: Long): Txn? = if (id <= 0) null else app.repo.getTxn(id)

    fun save(
        id: Long?,
        amountCents: Long,
        kind: TxnKind,
        occurredAt: Long,
        categoryId: Long,
        accountId: Long,
        note: String,
        tags: List<String>,
        receiptPath: String? = null,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            app.repo.upsertTxn(id, amountCents, kind, occurredAt, categoryId, accountId, note, tags, receiptPath)
            onDone()
        }
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            app.repo.deleteTxn(id)
            onDone()
        }
    }

    companion object {
        fun factory(app: AppContainer) = vmFactory { EditorViewModel(app) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(txnId: Long, onBack: () -> Unit) {
    val app = LocalApp.current
    val vm: EditorViewModel = viewModel(factory = EditorViewModel.factory(app))
    TransactionForm(
        txnId = txnId,
        vm = vm,
        onBack = onBack,
        asSheet = false,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(onDismiss: () -> Unit) {
    val app = LocalApp.current
    val vm: EditorViewModel = viewModel(factory = EditorViewModel.factory(app))
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        TransactionForm(txnId = 0, vm = vm, onBack = onDismiss, asSheet = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TransactionForm(
    txnId: Long,
    vm: EditorViewModel,
    onBack: () -> Unit,
    asSheet: Boolean,
) {
    val cats by vm.categories.collectAsState()
    val accs by vm.accounts.collectAsState()
    var kind by remember { mutableStateOf(TxnKind.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(0L) }
    var accountId by remember { mutableStateOf(0L) }
    var note by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var receiptPath by remember { mutableStateOf<String?>(null) }
    var captureName by remember { mutableStateOf<String?>(null) }
    val existingId = txnId.takeIf { it > 0 }
    val app = LocalApp.current
    val receipts = app.receipts
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val name = captureName
        if (ok && name != null) {
            receiptPath = receipts.finalizeCapture(name) ?: name
        }
    }
    val pickGallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val imported = receipts.importFromUri(uri)
            if (imported != null) receiptPath = imported
        }
    }

    LaunchedEffect(txnId, cats, accs) {
        val existing = vm.load(txnId)
        if (existing != null) {
            kind = existing.kind
            amount = Money.formatPlain(existing.amountCents)
            categoryId = existing.categoryId
            accountId = existing.accountId
            note = existing.note
            tags = existing.tags.joinToString("，")
            date = existing.date
            time = existing.dateTime.toLocalTime().withSecond(0).withNano(0)
            receiptPath = existing.receiptPath
        } else {
            val filtered = cats.filter { it.kind == kind && !it.archived }
            if (categoryId == 0L) categoryId = filtered.firstOrNull()?.id ?: 0
            if (accountId == 0L) accountId = accs.firstOrNull { !it.archived }?.id ?: 0
        }
    }

    val filteredCats = cats.filter { it.kind == kind && !it.archived }
    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (asSheet) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TxnKind.entries.forEachIndexed { index, k ->
                    SegmentedButton(
                        selected = kind == k,
                        onClick = {
                            kind = k
                            categoryId = cats.firstOrNull { it.kind == k && !it.archived }?.id ?: 0
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, TxnKind.entries.size),
                    ) { Text(k.label) }
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("金额") },
                prefix = { Text("¥") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium,
            )
            Text("分类", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredCats.forEach { c ->
                    FilterChip(
                        selected = categoryId == c.id,
                        onClick = { categoryId = c.id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColorDot(c.colorArgb)
                                Spacer(Modifier.width(6.dp))
                                Text("${c.emoji} ${c.name}")
                            }
                        },
                    )
                }
            }
            Text("账户", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accs.filter { !it.archived }.forEach { a ->
                    FilterChip(
                        selected = accountId == a.id,
                        onClick = { accountId = a.id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColorDot(a.colorArgb)
                                Spacer(Modifier.width(6.dp))
                                Text(a.name)
                            }
                        },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                    Text(date.toString())
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                    Text(time.toString().take(5))
                }
            }
            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("备注") })
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标签（逗号分隔）") },
                placeholder = { Text("日常，通勤") },
            )
            Text("收据照片", style = MaterialTheme.typography.titleMedium)
            val thumb = remember(receiptPath) { receipts.decodeThumb(receiptPath, 320) }
            if (thumb != null) {
                Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = "收据缩略图",
                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Text("未添加收据，可拍照或从相册选择，照片只保存在本机。", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val (uri, name) = receipts.captureUri()
                        captureName = name
                        takePicture.launch(uri)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("拍照") }
                OutlinedButton(onClick = { pickGallery.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("相册") }
            }
            if (receiptPath != null) {
                TextButton(onClick = {
                    receipts.delete(receiptPath)
                    receiptPath = null
                }) { Text("移除收据") }
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
                            val occurred = Dates.of(date, time.hour, time.minute)
                            vm.save(existingId, cents, kind, occurred, categoryId, accountId, note, tags.parseTags(), receiptPath, onBack)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (existingId == null) "保存" else "更新") }
            if (existingId != null) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("删除这笔流水") }
            }
            if (asSheet) Spacer(Modifier.height(24.dp))
        }
    }

    if (asSheet) {
        Column {
            Text("记一笔", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) { body() }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (existingId == null) "记一笔" else "编辑流水") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) { body() }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDate = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }
    if (showTime) {
        val state = rememberTimePickerState(time.hour, time.minute, true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    showTime = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("取消") } },
            text = { TimePicker(state = state) },
        )
    }
    if (confirmDelete && existingId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这笔流水？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = { vm.delete(existingId, onBack) }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}
