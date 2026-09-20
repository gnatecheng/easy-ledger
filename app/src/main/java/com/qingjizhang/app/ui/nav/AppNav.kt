package com.qingjizhang.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qingjizhang.app.ui.budget.BudgetScreen
import com.qingjizhang.app.ui.home.HomeScreen
import com.qingjizhang.app.ui.mine.AccountsScreen
import com.qingjizhang.app.ui.mine.CategoriesScreen
import com.qingjizhang.app.ui.mine.ImportExportScreen
import com.qingjizhang.app.ui.mine.MineScreen
import com.qingjizhang.app.ui.mine.SettingsScreen
import com.qingjizhang.app.ui.recurring.RecurringEditScreen
import com.qingjizhang.app.ui.recurring.RecurringListScreen
import com.qingjizhang.app.ui.stats.StatsScreen
import com.qingjizhang.app.ui.txn.QuickAddSheet
import com.qingjizhang.app.ui.txn.TransactionEditScreen
import com.qingjizhang.app.ui.txn.TransactionListScreen
import com.qingjizhang.app.domain.TxnKind

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "首页", Icons.Outlined.Home),
    Tab("txns", "明细", Icons.Outlined.ReceiptLong),
    Tab("stats", "统计", Icons.Outlined.PieChart),
    Tab("budgets", "预算", Icons.Outlined.AccountBalanceWallet),
    Tab("mine", "我的", Icons.Outlined.Person),
)

@Composable
fun QingJiZhangRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route.orEmpty()
    val showBottom = tabs.any { it.route == route }
    var quickAdd by rememberSaveable { mutableStateOf(false) }
    var quickKind by rememberSaveable { mutableStateOf("EXPENSE") }

    Scaffold(
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
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
                    onSettings = { nav.navigate("settings") },
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
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
    if (quickAdd) {
        QuickAddSheet(
            initialKind = TxnKind.fromRaw(quickKind),
            onDismiss = { quickAdd = false },
        )
    }
}
