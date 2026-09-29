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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
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
import com.qingjizhang.app.R
import com.qingjizhang.app.data.AppLanguage
import com.qingjizhang.app.data.ImportMode
import com.qingjizhang.app.data.ThemeMode
import com.qingjizhang.app.updateAppLanguage
import androidx.activity.ComponentActivity
import com.qingjizhang.app.ui.components.LanguagePicker
import com.qingjizhang.app.ui.components.ThemeModePicker
import com.qingjizhang.app.ui.i18n.LocaleHelper
import com.qingjizhang.app.ui.i18n.displayName
import com.qingjizhang.app.ui.i18n.localizedDemoNote
import com.qingjizhang.app.ui.i18n.localizedLabel
import androidx.compose.ui.res.stringResource
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
    onAppearance: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    val app = LocalApp.current
    val accounts by app.repo.observeAccounts().collectAsState(initial = emptyList())
    val txns by app.repo.observeTransactions().collectAsState(initial = emptyList())
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.mine_title), style = MaterialTheme.typography.headlineMedium)
        AppCard {
            Text(stringResource(R.string.net_worth), color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyText(accounts.sumOf { it.balanceCents }, large = true)
            Text(
                stringResource(R.string.mine_stats, txns.size, accounts.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        AppCard {
            MenuRow(stringResource(R.string.menu_categories), stringResource(R.string.menu_categories_sub), Icons.Outlined.Category, onCategories)
            HorizontalDivider()
            MenuRow(stringResource(R.string.menu_accounts), stringResource(R.string.menu_accounts_sub), Icons.Outlined.AccountBalanceWallet, onAccounts)
            HorizontalDivider()
            MenuRow(stringResource(R.string.menu_recurring), stringResource(R.string.menu_recurring_sub), Icons.Outlined.Repeat, onRecurring)
            HorizontalDivider()
            MenuRow(stringResource(R.string.menu_import_export), stringResource(R.string.menu_import_export_sub), Icons.Outlined.FileDownload, onImportExport)
            HorizontalDivider()
            MenuRow(stringResource(R.string.menu_appearance), stringResource(R.string.menu_appearance_sub), Icons.Outlined.Palette, onAppearance)
            HorizontalDivider()
            MenuRow(stringResource(R.string.menu_reminders), stringResource(R.string.menu_reminders_sub), Icons.Outlined.Notifications, onSettings)
            HorizontalDivider()
            MenuRow(stringResource(R.string.menu_about), stringResource(R.string.menu_about_sub), Icons.Outlined.Info, onAbout)
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

    fun deleteCategory(id: Long, onError: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                app.repo.deleteCategory(id)
            } catch (_: Exception) {
                onError(R.string.error_category_in_use)
            }
        }
    }

    fun saveAccount(a: Account) {
        viewModelScope.launch { app.repo.upsertAccount(a) }
    }

    fun deleteAccount(id: Long, onError: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                app.repo.deleteAccount(id)
            } catch (_: Exception) {
                onError(R.string.error_account_in_use)
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
    var errorRes by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        editing = Category(0, "", kind, Palette.expense.first(), "•", false, cats.size)
                    }) { Text(stringResource(R.string.action_add)) }
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
                    ) { Text(k.localizedLabel()) }
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats.filter { it.kind == kind }, key = { it.id }) { c ->
                    AppCard(onClick = { editing = c }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorDot(c.colorArgb, size = 14)
                            Spacer(Modifier.width(10.dp))
                            Text("${c.emoji} ${c.displayName()}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            TextButton(onClick = { vm.deleteCategory(c.id) { errorRes = it } }) {
                                Text(stringResource(R.string.action_delete))
                            }
                        }
                    }
                }
            }
        }
    }
    errorRes?.let { resId ->
        AlertDialog(
            onDismissRequest = { errorRes = null },
            confirmButton = { TextButton(onClick = { errorRes = null }) { Text(stringResource(R.string.action_ok)) } },
            title = { Text(stringResource(R.string.cannot_delete_title)) },
            text = { Text(stringResource(resId)) },
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
        title = {
            Text(stringResource(if (initial.id == 0L) R.string.new_category else R.string.edit_category))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true)
                OutlinedTextField(emoji, { emoji = it.take(2) }, label = { Text(stringResource(R.string.field_emoji)) }, singleLine = true)
                Text(stringResource(R.string.field_color), color = InkMuted)
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
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountsScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val vm: CatalogViewModel = viewModel(factory = CatalogViewModel.factory(app))
    val accounts by vm.accounts.collectAsState()
    var editing by remember { mutableStateOf<Account?>(null) }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.accounts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        editing = Account(0, "", AccountType.CASH, 0, Palette.accounts.first(), false, accounts.size)
                    }) { Text(stringResource(R.string.action_add)) }
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
                            Text(a.displayName(), fontWeight = FontWeight.Medium)
                            Text(a.type.localizedLabel(), color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                        MoneyText(a.balanceCents)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { vm.deleteAccount(a.id) { errorRes = it } }) {
                            Text(stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }
    errorRes?.let { resId ->
        AlertDialog(
            onDismissRequest = { errorRes = null },
            confirmButton = { TextButton(onClick = { errorRes = null }) { Text(stringResource(R.string.action_ok)) } },
            title = { Text(stringResource(R.string.cannot_delete_title)) },
            text = { Text(stringResource(resId)) },
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
        title = {
            Text(stringResource(if (initial.id == 0L) R.string.new_account else R.string.edit_account))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.localizedLabel()) })
                    }
                }
                OutlinedTextField(
                    opening,
                    { opening = it },
                    label = { Text(stringResource(R.string.opening_balance)) },
                    prefix = { Text(stringResource(R.string.currency_yuan)) },
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
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
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

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { app.settings.update { it.copy(themeMode = mode) } }
    }

    fun setAppLanguage(language: AppLanguage, onApplied: () -> Unit) {
        viewModelScope.launch {
            app.settings.update { it.copy(appLanguage = language) }
            LocaleHelper.applyAppLanguage(language)
            onApplied()
        }
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
fun AppearanceSettingsScreen(onBack: () -> Unit) {
    val app = LocalApp.current
    val vm: SettingsVm = viewModel(factory = SettingsVm.factory(app))
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appearance_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                ThemeModePicker(settings.themeMode, vm::setThemeMode)
                Spacer(Modifier.height(16.dp))
                LanguagePicker(settings.appLanguage) { lang ->
                    if (lang != settings.appLanguage) {
                        vm.setAppLanguage(lang) {
                            activity?.let { act ->
                                act.updateAppLanguage(settings.copy(appLanguage = lang))
                            }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.dark_mode_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
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
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_reminders)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                Text(stringResource(R.string.large_txn_title), fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.large_txn_desc), color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    threshold, { threshold = it },
                    label = { Text(stringResource(R.string.large_txn_threshold)) },
                    prefix = { Text(stringResource(R.string.currency_yuan)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
            AppCard {
                Text(stringResource(R.string.inactivity_title), fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.inactivity_desc), color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    days, { days = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.inactivity_days)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(onClick = {
                vm.save(threshold, days)
                scope.launch { snack.showSnackbar(context.getString(R.string.settings_saved)) }
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_settings)) }
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.clear_all_txns))
            }
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.restore_demo))
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.confirm_clear_txns_title)) },
            text = { Text(stringResource(R.string.confirm_clear_txns_body)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearDemo { scope.launch { snack.showSnackbar(context.getString(R.string.txns_cleared)) } }
                    confirmClear = false
                }) { Text(stringResource(R.string.action_clear)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.confirm_restore_demo_title)) },
            text = { Text(stringResource(R.string.confirm_restore_demo_body)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetAll { scope.launch { snack.showSnackbar(context.getString(R.string.demo_restored)) } }
                    confirmReset = false
                }) { Text(stringResource(R.string.action_restore)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

class ImportVm(private val app: AppContainer) : ViewModel() {
    var preview by mutableStateOf<ImportPreview?>(null)
    var busy by mutableStateOf(false)
    var message by mutableStateOf<Pair<Int, List<Any>>?>(null)

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
                message = R.string.parse_failed to listOf(e.message ?: "")
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
                message = R.string.import_done to emptyList()
                preview = null
                onDone()
            } catch (e: Exception) {
                message = R.string.import_failed to listOf(e.message ?: "")
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
            snack.showSnackbar(context.getString(R.string.csv_saved))
        }
    }
    val createJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val body = vm.json()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(body.toByteArray()) }
            }
            snack.showSnackbar(context.getString(R.string.json_saved))
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
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_backup_chooser)))
    }

    val csvDefaultName = context.getString(R.string.export_csv_filename, LocalDate.now())
    val jsonDefaultName = context.getString(R.string.export_json_filename, LocalDate.now())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_export_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                Text(stringResource(R.string.export_section), fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.export_desc), color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { createCsv.launch(csvDefaultName) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.save_csv)) }
                OutlinedButton(
                    onClick = {
                        scope.launch { share(csvDefaultName, "text/csv", vm.csv()) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.share_csv)) }
                Button(
                    onClick = { createJson.launch(jsonDefaultName) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.save_json)) }
                OutlinedButton(
                    onClick = {
                        scope.launch { share(jsonDefaultName, "application/json", vm.json()) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.share_json)) }
            }
            AppCard {
                Text(stringResource(R.string.import_section), fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.import_desc), color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { openDoc.launch(arrayOf("text/*", "application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.FileUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pick_file))
                }
            }
            vm.message?.let { (resId, args) ->
                Text(
                    stringResource(resId, *args.toTypedArray()),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (vm.busy) Text(stringResource(R.string.processing), color = InkMuted)
            preview?.let { p ->
                AppCard {
                    Text(stringResource(R.string.preview_title, p.format), fontWeight = FontWeight.Medium)
                    Text(p.sourceName, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.preview_counts, p.newCount, p.duplicateCount))
                    p.warnings.forEach { Text(it, color = InkMuted, style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.height(8.dp))
                    val rows = p.csvRows.take(8)
                    rows.forEach { r ->
                        Text(
                            "${r.date} ${r.type} ${r.amount} ${r.category} ${localizedDemoNote(r.note)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (p.csvRows.size > 8) {
                        Text(stringResource(R.string.preview_rows_more, p.csvRows.size), color = InkMuted)
                    }
                    p.backup?.let { b ->
                        Text(
                            stringResource(
                                R.string.preview_backup_counts,
                                b.accounts.size,
                                b.categories.size,
                                b.transactions.size,
                                b.budgets.size,
                            ),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.apply(ImportMode.SKIP_DUPLICATES) {} }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_skip_dupes))
                    }
                    OutlinedButton(onClick = { vm.apply(ImportMode.IMPORT_ALL) {} }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_all))
                    }
                    OutlinedButton(onClick = { vm.apply(ImportMode.REPLACE_ALL) {} }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_replace))
                    }
                }
            }
        }
    }
}
