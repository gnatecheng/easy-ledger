package com.qingjizhang.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qingjizhang.app.R
import com.qingjizhang.app.domain.TxnKind
import com.qingjizhang.app.ui.budget.BudgetScreen
import com.qingjizhang.app.ui.home.HomeScreen
import com.qingjizhang.app.ui.mine.AboutScreen
import com.qingjizhang.app.ui.mine.AccountsScreen
import com.qingjizhang.app.ui.mine.CategoriesScreen
import com.qingjizhang.app.ui.mine.ImportExportScreen
import com.qingjizhang.app.ui.mine.MineScreen
import com.qingjizhang.app.ui.mine.AppearanceSettingsScreen
import com.qingjizhang.app.ui.mine.SettingsScreen
import com.qingjizhang.app.ui.recurring.RecurringEditScreen
import com.qingjizhang.app.ui.recurring.RecurringListScreen
import com.qingjizhang.app.ui.stats.StatsScreen
import com.qingjizhang.app.ui.txn.QuickAddSheet
import com.qingjizhang.app.ui.txn.TransactionEditScreen
import com.qingjizhang.app.ui.txn.TransactionListScreen

private enum class Tab(val route: String, val labelRes: Int, val icon: ImageVector) {
    HOME("home", R.string.tab_home, Icons.Outlined.Home),
    TXNS("txns", R.string.tab_txns, Icons.Outlined.ReceiptLong),
    STATS("stats", R.string.tab_stats, Icons.Outlined.PieChart),
    BUDGETS("budgets", R.string.tab_budgets, Icons.Outlined.AccountBalanceWallet),
    MINE("mine", R.string.tab_mine, Icons.Outlined.Person),
}

@Composable
fun QingJiZhangRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val showBottom = Tab.entries.any { it.route == route }
    var quickAdd by rememberSaveable { mutableStateOf(false) }
    var quickKind by rememberSaveable { mutableStateOf("EXPENSE") }

    Scaffold(
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(tab.icon, contentDescription = stringResource(tab.labelRes))
                            },
                            label = {
                                Text(
                                    stringResource(tab.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    onOpenTxn = { nav.navigate("txn/edit/$it") },
                    onQuickAdd = {
                        quickKind = TxnKind.EXPENSE.name
                        quickAdd = true
                    },
                    onTransfer = {
                        quickKind = TxnKind.TRANSFER.name
                        quickAdd = true
                    },
                    onOpenTxns = {
                        nav.navigate("txns") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenBudgets = {
                        nav.navigate("budgets") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable("txns") {
                TransactionListScreen(
                    onOpenTxn = { nav.navigate("txn/edit/$it") },
                    onQuickAdd = {
                        quickKind = TxnKind.EXPENSE.name
                        quickAdd = true
                    },
                    onTransfer = {
                        quickKind = TxnKind.TRANSFER.name
                        quickAdd = true
                    },
                )
            }
            composable("stats") { StatsScreen() }
            composable("budgets") { BudgetScreen() }
            composable("mine") {
                MineScreen(
                    onCategories = { nav.navigate("categories") },
                    onAccounts = { nav.navigate("accounts") },
                    onRecurring = { nav.navigate("recurring") },
                    onImportExport = { nav.navigate("import") },
                    onAppearance = { nav.navigate("appearance") },
                    onSettings = { nav.navigate("settings") },
                    onAbout = { nav.navigate("about") },
                )
            }
            composable(
                route = "txn/edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                TransactionEditScreen(txnId = id, onBack = { nav.popBackStack() })
            }
            composable("categories") { CategoriesScreen(onBack = { nav.popBackStack() }) }
            composable("accounts") { AccountsScreen(onBack = { nav.popBackStack() }) }
            composable("recurring") {
                RecurringListScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("recurring/edit/$it") },
                )
            }
            composable(
                route = "recurring/edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                RecurringEditScreen(
                    ruleId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("import") { ImportExportScreen(onBack = { nav.popBackStack() }) }
            composable("appearance") { AppearanceSettingsScreen(onBack = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable("about") { AboutScreen(onBack = { nav.popBackStack() }) }
        }
    }
    if (quickAdd) {
        QuickAddSheet(
            initialKind = TxnKind.fromRaw(quickKind),
            onDismiss = { quickAdd = false },
        )
    }
}
