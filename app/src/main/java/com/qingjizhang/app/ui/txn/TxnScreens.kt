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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SwapHoriz
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
import com.qingjizhang.app.ui.theme.Transfer
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
    val categoryId: Long? = null,
    val tag: String? = null,
    val minAmount: String = "",
    val maxAmount: String = "",
    val items: List<Txn> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<String> = emptyList(),
    val dayGroups: List<Pair<LocalDate, List<Txn>>> = emptyList(),
    val filtersOpen: Boolean = true,
) {
    val hasFilters: Boolean
        get() = query.isNotBlank() || kind != null || accountId != null || categoryId != null ||
            !tag.isNullOrBlank() || minAmount.isNotBlank() || maxAmount.isNotBlank()
}

class TxnListViewModel(app: AppContainer) : ViewModel() {
    val month = MutableStateFlow(YearMonth.now())
    val query = MutableStateFlow("")
    val kind = MutableStateFlow<TxnKind?>(null)
    val accountId = MutableStateFlow<Long?>(null)
    val categoryId = MutableStateFlow<Long?>(null)
    val tag = MutableStateFlow<String?>(null)
    val minAmount = MutableStateFlow("")
    val maxAmount = MutableStateFlow("")
    val filtersOpen = MutableStateFlow(true)

    fun clearFilters() {
        query.value = ""
        kind.value = null
        accountId.value = null
        categoryId.value = null
        tag.value = null
        minAmount.value = ""
        maxAmount.value = ""
    }

    val ui = combine(
        combine(month, query, kind, accountId) { m, q, k, a -> FilterA(m, q, k, a) },
        combine(categoryId, tag, minAmount, maxAmount, filtersOpen) { c, t, minA, maxA, open ->
            FilterB(c, t, minA, maxA, open)
        },
        app.repo.observeTransactions(),
        app.repo.observeAccounts(),
        app.repo.observeCategories(),
    ) { a, b, txns, accounts, cats ->
        val start = Dates.monthStart(a.month)
        val end = Dates.monthEndExclusive(a.month)
        val monthTxns = txns.filter { it.occurredAt in start until end }
        val filtered = FinanceRepository.filterTxns(
            monthTxns,
            a.query,
            a.kind,
            a.accountId,
            b.categoryId,
            b.tag,
            null,
            null,
            Money.parseYuan(b.minAmount).takeIf { b.minAmount.isNotBlank() },
            Money.parseYuan(b.maxAmount).takeIf { b.maxAmount.isNotBlank() },
        )
        TxnListUi(
            month = a.month,
            query = a.query,
            kind = a.kind,
            accountId = a.accountId,
            categoryId = b.categoryId,
            tag = b.tag,
            minAmount = b.minAmount,
            maxAmount = b.maxAmount,
            items = filtered,
            accounts = accounts.filter { !it.archived },
            categories = cats.filter { !it.archived && it.kind != TxnKind.TRANSFER },
            tags = monthTxns.flatMap { it.tags }.distinct().sorted(),
            dayGroups = filtered.groupBy { it.date }.toList().sortedByDescending { it.first },
            filtersOpen = b.filtersOpen,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TxnListUi())

    private data class FilterA(val month: YearMonth, val query: String, val kind: TxnKind?, val accountId: Long?)
    private data class FilterB(
        val categoryId: Long?,
        val tag: String?,
        val minAmount: String,
        val maxAmount: String,
        val filtersOpen: Boolean,
    )

    companion object {
        fun factory(app: AppContainer) = vmFactory { TxnListViewModel(app) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionListScreen(
    onOpenTxn: (Long) -> Unit,
    onQuickAdd: () -> Unit,
    onTransfer: () -> Unit,
) {
    val app = LocalApp.current
    val vm: TxnListViewModel = viewModel(factory = TxnListViewModel.factory(app))
    val ui by vm.ui.collectAsState()

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingActionButton(
                    onClick = onTransfer,
                    containerColor = Transfer,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = "转账")
                }
                FloatingActionButton(onClick = onQuickAdd) {
                    Icon(Icons.Default.Add, contentDescription = "记一笔")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 140.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("明细", style = MaterialTheme.typography.headlineMedium)
                MonthSwitcher(ui.month) { vm.month.value = it }
                OutlinedTextField(
                    value = ui.query,
                    onValueChange = { vm.query.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索备注、标签") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (ui.query.isNotBlank()) {
                            IconButton(onClick = { vm.query.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "清除搜索")
                            }
                        }
                    },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = ui.kind == null, onClick = { vm.kind.value = null }, label = { Text("全部") })
                    FilterChip(selected = ui.kind == TxnKind.EXPENSE, onClick = { vm.kind.value = TxnKind.EXPENSE }, label = { Text("支出") })
                    FilterChip(selected = ui.kind == TxnKind.INCOME, onClick = { vm.kind.value = TxnKind.INCOME }, label = { Text("收入") })
                    FilterChip(selected = ui.kind == TxnKind.TRANSFER, onClick = { vm.kind.value = TxnKind.TRANSFER }, label = { Text("转账") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { vm.filtersOpen.value = !ui.filtersOpen }) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (ui.filtersOpen) "收起筛选" else "更多筛选")
                    }
                    if (ui.hasFilters) {
                        TextButton(onClick = { vm.clearFilters() }) { Text("清除筛选") }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${ui.items.size} 笔", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
                if (ui.filtersOpen) {
                    if (ui.kind != TxnKind.TRANSFER) {
                        Text("分类", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = ui.categoryId == null, onClick = { vm.categoryId.value = null }, label = { Text("全部分类") })
                            ui.categories.filter { ui.kind == null || it.kind == ui.kind }.forEach { c ->
                                FilterChip(
                                    selected = ui.categoryId == c.id,
                                    onClick = { vm.categoryId.value = if (ui.categoryId == c.id) null else c.id },
                                    label = { Text("${c.emoji} ${c.name}") },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("账户", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = ui.accountId == null, onClick = { vm.accountId.value = null }, label = { Text("全部账户") })
                        ui.accounts.forEach { acc ->
                            FilterChip(
                                selected = ui.accountId == acc.id,
                                onClick = { vm.accountId.value = if (ui.accountId == acc.id) null else acc.id },
                                label = { Text(acc.name) },
                            )
                        }
                    }
                    if (ui.tags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("标签", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = ui.tag == null, onClick = { vm.tag.value = null }, label = { Text("全部标签") })
                            ui.tags.forEach { t ->
                                FilterChip(
                                    selected = ui.tag == t,
                                    onClick = { vm.tag.value = if (ui.tag == t) null else t },
                                    label = { Text(t) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("金额范围", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = ui.minAmount,
                            onValueChange = { vm.minAmount.value = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            modifier = Modifier.weight(1f),
                            label = { Text("最低") },
                            prefix = { Text("¥") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                        )
                        Text("—")
                        OutlinedTextField(
                            value = ui.maxAmount,
                            onValueChange = { vm.maxAmount.value = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            modifier = Modifier.weight(1f),
                            label = { Text("最高") },
                            prefix = { Text("¥") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                        )
                    }
                }
            }
            if (ui.dayGroups.isEmpty()) {
                item { EmptyHint(if (ui.hasFilters) "没有符合条件的流水，试试清除筛选" else "没有符合条件的流水") }
            }
            ui.dayGroups.forEach { (day, list) ->
                item(key = "h-$day") {
                    val ledger = list.filter { it.kind != TxnKind.TRANSFER }
                    val sub = ledger.sumOf { it.signedCents }
                    val transferSum = list.filter { it.kind == TxnKind.TRANSFER }.sumOf { it.amountCents }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(day.format(Dates.dayCn), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        if (ledger.isNotEmpty()) MoneyText(sub)
                        if (transferSum > 0) {
                            if (ledger.isNotEmpty()) Spacer(Modifier.width(8.dp))
                            Text("转账 ${Money.formatYuan(transferSum)}", color = Transfer, style = MaterialTheme.typography.labelMedium)
                        }
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
        transferToAccountId: Long? = null,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            app.repo.upsertTxn(
                id, amountCents, kind, occurredAt, categoryId, accountId, note, tags, receiptPath,
                transferToAccountId = transferToAccountId,
            )
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
fun QuickAddSheet(initialKind: TxnKind = TxnKind.EXPENSE, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val vm: EditorViewModel = viewModel(factory = EditorViewModel.factory(app))
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        TransactionForm(txnId = 0, vm = vm, onBack = onDismiss, asSheet = true, initialKind = initialKind)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TransactionForm(
    txnId: Long,
    vm: EditorViewModel,
    onBack: () -> Unit,
    asSheet: Boolean,
    initialKind: TxnKind = TxnKind.EXPENSE,
) {
    val cats by vm.categories.collectAsState()
    val accs by vm.accounts.collectAsState()
    var kind by remember { mutableStateOf(initialKind) }
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(0L) }
    var accountId by remember { mutableStateOf(0L) }
    var toAccountId by remember { mutableStateOf(0L) }
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
            toAccountId = existing.transferToAccountId ?: 0L
            note = existing.note
            tags = existing.tags.joinToString("，")
            date = existing.date
            time = existing.dateTime.toLocalTime().withSecond(0).withNano(0)
            receiptPath = existing.receiptPath
        } else {
            kind = initialKind
            val filtered = cats.filter { it.kind == kind && !it.archived }
            if (categoryId == 0L) categoryId = filtered.firstOrNull()?.id ?: 0
            val activeAccs = accs.filter { !it.archived }
            if (accountId == 0L) accountId = activeAccs.firstOrNull()?.id ?: 0
            if (toAccountId == 0L) toAccountId = activeAccs.drop(1).firstOrNull()?.id ?: 0
        }
    }

    val filteredCats = cats.filter { it.kind == kind && !it.archived }
    val activeAccs = accs.filter { !it.archived }
    val isTransfer = kind == TxnKind.TRANSFER
    val kinds = listOf(TxnKind.EXPENSE, TxnKind.INCOME, TxnKind.TRANSFER)
    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (asSheet) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                kinds.forEachIndexed { index, k ->
                    SegmentedButton(
                        selected = kind == k,
                        onClick = {
                            kind = k
                            if (k != TxnKind.TRANSFER) {
                                categoryId = cats.firstOrNull { it.kind == k && !it.archived }?.id ?: 0
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
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
            if (isTransfer) {
                Text("转出账户", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeAccs.forEach { a ->
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
                Text("转入账户", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeAccs.forEach { a ->
                        FilterChip(
                            selected = toAccountId == a.id,
                            onClick = { toAccountId = a.id },
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
            } else {
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
                    activeAccs.forEach { a ->
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
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                    Text(date.toString())
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                    Text(time.toString().take(5))
                }
            }
            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), label = { Text("备注（可选）") })
            if (!isTransfer) {
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
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val cents = Money.parseYuan(amount)
                    when {
                        cents == null || cents <= 0 -> error = "请输入有效金额"
                        isTransfer && accountId == 0L -> error = "请选择转出账户"
                        isTransfer && toAccountId == 0L -> error = "请选择转入账户"
                        isTransfer && accountId == toAccountId -> error = "转出和转入账户不能相同"
                        !isTransfer && categoryId == 0L -> error = "请选择分类"
                        !isTransfer && accountId == 0L -> error = "请选择账户"
                        else -> {
                            error = null
                            val occurred = Dates.of(date, time.hour, time.minute)
                            vm.save(
                                existingId, cents, kind, occurred, categoryId, accountId, note,
                                if (isTransfer) listOf("转账") else tags.parseTags(),
                                if (isTransfer) null else receiptPath,
                                transferToAccountId = if (isTransfer) toAccountId else null,
                                onDone = onBack,
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (existingId == null) "保存" else "更新") }
            if (existingId != null) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isTransfer) "删除这笔转账" else "删除这笔流水")
                }
            }
            if (asSheet) Spacer(Modifier.height(24.dp))
        }
    }

    if (asSheet) {
        Column {
            Text(
                if (isTransfer) "账户转账" else "记一笔",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Column(Modifier.verticalScroll(rememberScrollState())) { body() }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when {
                                existingId == null && isTransfer -> "账户转账"
                                existingId == null -> "记一笔"
                                isTransfer -> "编辑转账"
                                else -> "编辑流水"
                            },
                        )
                    },
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
            title = { Text(if (isTransfer) "删除这笔转账？" else "删除这笔流水？") },
            text = { Text(if (isTransfer) "删除后两端账户余额会一并还原。" else "删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = { vm.delete(existingId, onBack) }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}
