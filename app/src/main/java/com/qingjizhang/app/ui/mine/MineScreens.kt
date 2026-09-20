package com.qingjizhang.app.ui.mine

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qingjizhang.app.AppContainer
import com.qingjizhang.app.BuildConfig
import com.qingjizhang.app.data.ImportMode
import com.qingjizhang.app.data.ImportPreview
import com.qingjizhang.app.data.Palette
import com.qingjizhang.app.domain.Account
import com.qingjizhang.app.domain.AccountType
import com.qingjizhang.app.domain.Category
import com.qingjizhang.app.domain.Money
import com.qingjizhang.app.domain.TxnKind
import com.qingjizhang.app.ui.LocalApp
import com.qingjizhang.app.ui.components.AppCard
import com.qingjizhang.app.ui.components.ColorDot
import com.qingjizhang.app.ui.components.MoneyText
import com.qingjizhang.app.ui.theme.InkMuted
import com.qingjizhang.app.ui.vmFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

@Composable
fun MineScreen(
    onCategories: () -> Unit,
    onAccounts: () -> Unit,
    onRecurring: () -> Unit,
    onImportExport: () -> Unit,
    onSettings: () -> Unit,
) {
    val app = LocalApp.current
    val accounts by app.repo.observeAccounts().collectAsState(initial = emptyList())
    val txns by app.repo.observeTransactions().collectAsState(initial = emptyList())
    val settings by app.settings.settings.collectAsState(initial = com.qingjizhang.app.data.AppSettings())
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("我的", style = MaterialTheme.typography.headlineMedium)
        AppCard {
            Text("净资产", color = InkMuted)
            MoneyText(accounts.sumOf { it.balanceCents }, large = true)
            Text("共 ${txns.size} 笔流水 · ${accounts.size} 个账户", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
        AppCard {
            MenuRow("分类管理", "支出 / 收入分类与颜色", Icons.Outlined.Category, onCategories)
            HorizontalDivider()
            MenuRow("账户管理", "现金、银行卡、支付宝…", Icons.Outlined.AccountBalanceWallet, onAccounts)
            HorizontalDivider()
            MenuRow("周期记账", "每天 / 每周 / 每月自动入账", Icons.Outlined.Repeat, onRecurring)
            HorizontalDivider()
            MenuRow("导入 / 导出", "CSV、JSON 备份与恢复", Icons.Outlined.FileDownload, onImportExport)
            HorizontalDivider()
            MenuRow("提醒设置", "大额阈值、未记账提醒", Icons.Outlined.Notifications, onSettings)
        }
        AppCard {
            ListItem(
                headlineContent = { Text("深色模式") },
                supportingContent = { Text("夜间使用更护眼，数据仍只保存在本机") },
                leadingContent = { Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = {
                    Switch(
                        checked = settings.darkTheme,
                        onCheckedChange = { checked ->
                            scope.launch { app.settings.update { it.copy(darkTheme = checked) } }
                        },
                    )
                },
                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            )
        }
        AppCard {
            Text("关于轻记账", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text("版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）", color = InkMuted)
            Text("本地记账，数据只保存在这台手机上。", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun MenuRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

class CatalogViewModel(private val app: AppContainer) : ViewModel() {
    val categories = app.repo.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accounts = app.repo.observeAccounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveCategory(c: Category) {
        viewModelScope.launch { app.repo.upsertCategory(c) }
    }

    fun deleteCategory(id: Long, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                app.repo.deleteCategory(id)
            } catch (_: Exception) {
                onError("该分类下还有流水，无法删除")
            }
        }
    }

    fun saveAccount(a: Account) {
        viewModelScope.launch { app.repo.upsertAccount(a) }
    }

    fun deleteAccount(id: Long, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                app.repo.deleteAccount(id)
            } catch (_: Exception) {
                onError("该账户下还有流水，无法删除")
            }
        }
    }

    companion object {
        fun factory(app: AppContainer) = vmFactory { CatalogViewModel(app) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val vm: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(app))
    val cats by vm.categories.collectAsState()
    var kind by remember { mutableStateOf(TxnKind.EXPENSE) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    TextButton(onClick = {
                        editing = Category(0, "", kind, Palette.expense.first(), "•", false, cats.size)
                    }) { Text("新增") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TxnKind.ledger.forEachIndexed { i, k ->
                    SegmentedButton(
                        selected = kind == k,
                        onClick = { kind = k },
                        shape = SegmentedButtonDefaults.itemShape(i, TxnKind.ledger.size),
                    ) { Text(k.label) }
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats.filter { it.kind == kind }, key = { it.id }) { c ->
                    AppCard(onClick = { editing = c }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorDot(c.colorArgb, size = 14)
                            Spacer(Modifier.width(10.dp))
                            Text("${c.emoji} ${c.name}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            TextButton(onClick = { vm.deleteCategory(c.id) { error = it } }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }
    error?.let {
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton(onClick = { error = null }) { Text("好") } },
            title = { Text("无法删除") },
            text = { Text(it) },
        )
    }
    val current = editing
    if (current != null) {
        CategoryEditor(current, onDismiss = { editing = null }, onSave = {
            vm.saveCategory(it.copy(kind = kind))
            editing = null
        })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditor(initial: Category, onDismiss: () -> Unit, onSave: (Category) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var emoji by remember { mutableStateOf(initial.emoji) }
    var color by remember { mutableStateOf(initial.colorArgb) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "新分类" else "编辑分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true)
                OutlinedTextField(emoji, { emoji = it.take(2) }, label = { Text("表情") }, singleLine = true)
                Text("颜色", color = InkMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palette.extras.forEach { c ->
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .clickable { color = c },
                        ) {
                            if (c == color) Text("✓", color = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onSave(initial.copy(name = name.trim(), emoji = emoji, colorArgb = color))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountsScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val vm: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(app))
    val accounts by vm.accounts.collectAsState()
    var editing by remember { mutableStateOf<Account?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账户管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    TextButton(onClick = {
                        editing = Account(0, "", AccountType.CASH, 0, Palette.accounts.first(), false, accounts.size)
                    }) { Text("新增") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(accounts, key = { it.id }) { a ->
                AppCard(onClick = { editing = a }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(a.colorArgb, size = 14)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.name, fontWeight = FontWeight.Medium)
                            Text(a.type.label, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                        MoneyText(a.balanceCents)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { vm.deleteAccount(a.id) { error = it } }) { Text("删除") }
                    }
                }
            }
        }
    }
    error?.let {
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton(onClick = { error = null }) { Text("好") } },
            title = { Text("无法删除") },
            text = { Text(it) },
        )
    }
    val current = editing
    if (current != null) {
        AccountEditor(current, onDismiss = { editing = null }, onSave = {
            vm.saveAccount(it)
            editing = null
        })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountEditor(initial: Account, onDismiss: () -> Unit, onSave: (Account) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var type by remember { mutableStateOf(initial.type) }
    var opening by remember { mutableStateOf(Money.formatPlain(initial.initialBalanceCents)) }
    var color by remember { mutableStateOf(initial.colorArgb) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "新账户" else "编辑账户") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                    }
                }
                OutlinedTextField(
                    opening,
                    { opening = it },
                    label = { Text("期初余额") },
                    prefix = { Text("¥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palette.extras.forEach { c ->
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(Color(c)).clickable { color = c },
                        ) {
                            if (c == color) Text("✓", color = Color.White, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = Money.parseYuan(opening) ?: 0L
                if (name.isNotBlank()) onSave(initial.copy(name = name.trim(), type = type, initialBalanceCents = cents, colorArgb = color))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

class SettingsVm(private val app: AppContainer) : ViewModel() {
    val settings = app.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.qingjizhang.app.data.AppSettings())

    fun save(thresholdYuan: String, days: String) {
        viewModelScope.launch {
            val t = Money.parseYuan(thresholdYuan) ?: return@launch
            val d = days.toIntOrNull()?.coerceIn(0, 30) ?: return@launch
            app.settings.update { it.copy(largeTxnThresholdCents = t, inactivityNudgeDays = d) }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { app.settings.update { it.copy(darkTheme = enabled) } }
    }

    fun clearDemo(onDone: () -> Unit) {
        viewModelScope.launch {
            app.repo.deleteAllTransactions()
            onDone()
        }
    }

    fun resetAll(onDone: () -> Unit) {
        viewModelScope.launch {
            app.receipts.deleteAll()
            app.seeder.resetDemoData()
            app.repo.generateDueRecurring()
            onDone()
        }
    }

    companion object {
        fun factory(app: AppContainer) = vmFactory { SettingsVm(app) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val vm: SettingsVm = viewModel(factory = SettingsVm.factory(app))
    val settings by vm.settings.collectAsState()
    var threshold by remember(settings.largeTxnThresholdCents) { mutableStateOf(Money.formatPlain(settings.largeTxnThresholdCents)) }
    var days by remember(settings.inactivityNudgeDays) { mutableStateOf(settings.inactivityNudgeDays.toString()) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                Text("外观", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("深色模式", modifier = Modifier.weight(1f))
                    Switch(checked = settings.darkTheme, onCheckedChange = { vm.setDarkTheme(it) })
                }
            }
            AppCard {
                Text("大额交易提醒", fontWeight = FontWeight.Medium)
                Text("单笔金额达到该阈值时，首页会显示醒目横幅。", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    threshold, { threshold = it },
                    label = { Text("阈值") }, prefix = { Text("¥") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
            AppCard {
                Text("未记账提醒", fontWeight = FontWeight.Medium)
                Text("连续若干天没有新记录时，首页会轻轻提醒。设为 0 可关闭。", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    days, { days = it.filter { ch -> ch.isDigit() } },
                    label = { Text("天数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(onClick = {
                vm.save(threshold, days)
                scope.launch { snack.showSnackbar("已保存") }
            }, modifier = Modifier.fillMaxWidth()) { Text("保存设置") }
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) { Text("清除全部流水（保留分类账户）") }
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("恢复演示数据") }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清除全部流水？") },
            text = { Text("分类、账户和预算会保留，流水不可恢复（除非你事先导出了备份）。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearDemo { scope.launch { snack.showSnackbar("流水已清空") } }
                    confirmClear = false
                }) { Text("清除") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("恢复演示数据？") },
            text = { Text("将重置分类、账户、预算和流水为首次启动时的示例。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetAll { scope.launch { snack.showSnackbar("已恢复演示数据") } }
                    confirmReset = false
                }) { Text("恢复") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("取消") } },
        )
    }
}

class ImportVm(private val app: AppContainer) : ViewModel() {
    var preview by mutableStateOf<ImportPreview?>(null)
    var busy by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    fun parse(name: String, text: String) {
        viewModelScope.launch {
            busy = true
            message = null
            preview = try {
                if (name.endsWith(".json", true) || text.trim().startsWith("{")) {
                    app.backup.previewJson(text, name)
                } else {
                    app.backup.previewCsv(text, name)
                }
            } catch (e: Exception) {
                message = "无法解析：${e.message}"
                null
            }
            busy = false
        }
    }

    fun apply(mode: ImportMode, onDone: () -> Unit) {
        val p = preview ?: return
        viewModelScope.launch {
            busy = true
            try {
                app.backup.apply(p, mode)
                message = "导入完成"
                preview = null
                onDone()
            } catch (e: Exception) {
                message = "导入失败：${e.message}"
            }
            busy = false
        }
    }

    suspend fun csv(): String = app.backup.exportCsv()
    suspend fun json(): String = app.backup.exportJson()

    companion object {
        fun factory(app: AppContainer) = vmFactory { ImportVm(app) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val vm: ImportVm = viewModel(factory = ImportVm.factory(app))
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val preview = vm.preview

    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
            }
            val name = uri.lastPathSegment ?: "import"
            vm.parse(name, text)
        }
    }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val body = vm.csv()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(body.toByteArray()) }
            }
            snack.showSnackbar("CSV 已保存")
        }
    }
    val createJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val body = vm.json()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(body.toByteArray()) }
            }
            snack.showSnackbar("JSON 备份已保存")
        }
    }

    fun share(name: String, mime: String, content: String) {
        val file = File(context.cacheDir, name)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享备份"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入 / 导出") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                Text("导出", fontWeight = FontWeight.Medium)
                Text("CSV 适合用表格软件打开；JSON 是完整备份（流水、分类、账户、预算）。", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { createCsv.launch("轻记账-${LocalDate.now()}.csv") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("保存 CSV") }
                OutlinedButton(
                    onClick = {
                        scope.launch { share("轻记账-${LocalDate.now()}.csv", "text/csv", vm.csv()) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("分享 CSV") }
                Button(
                    onClick = { createJson.launch("轻记账-备份-${LocalDate.now()}.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("保存 JSON 备份") }
                OutlinedButton(
                    onClick = {
                        scope.launch { share("轻记账-备份-${LocalDate.now()}.json", "application/json", vm.json()) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("一键分享备份") }
            }
            AppCard {
                Text("导入", fontWeight = FontWeight.Medium)
                Text("支持本应用导出的 JSON，以及含「日期、金额、分类、备注」等常见列的 CSV。导入前会预览并提示重复项。", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { openDoc.launch(arrayOf("text/*", "application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.FileUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("选择文件恢复")
                }
            }
            vm.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (vm.busy) Text("处理中…", color = InkMuted)
            preview?.let { p ->
                AppCard {
                    Text("预览 · ${p.format}", fontWeight = FontWeight.Medium)
                    Text(p.sourceName, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("新增 ${p.newCount} 笔 · 可能重复 ${p.duplicateCount} 笔")
                    p.warnings.forEach { Text(it, color = InkMuted, style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.height(8.dp))
                    val rows = p.csvRows.take(8)
                    rows.forEach { r ->
                        Text("${r.date} ${r.type} ${r.amount} ${r.category} ${r.note}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (p.csvRows.size > 8) Text("…共 ${p.csvRows.size} 行", color = InkMuted)
                    p.backup?.let { b ->
                        Text("账户 ${b.accounts.size} · 分类 ${b.categories.size} · 流水 ${b.transactions.size} · 预算 ${b.budgets.size}")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.apply(ImportMode.SKIP_DUPLICATES) {} }, modifier = Modifier.fillMaxWidth()) {
                        Text("导入并跳过重复")
                    }
                    OutlinedButton(onClick = { vm.apply(ImportMode.IMPORT_ALL) {} }, modifier = Modifier.fillMaxWidth()) {
                        Text("全部导入（允许重复）")
                    }
                    OutlinedButton(onClick = { vm.apply(ImportMode.REPLACE_ALL) {} }, modifier = Modifier.fillMaxWidth()) {
                        Text("覆盖现有数据")
                    }
                }
            }
        }
    }
}
