package com.qingjizhang.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Home alert banner container and content colors; light theme matches pre-1.3.1 appearance. */
@Composable
fun financeAlertOverColors(): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    return if (isAppInDarkTheme()) {
        cs.errorContainer to cs.onErrorContainer
    } else {
        OverSoft to Over
    }
}

@Composable
fun financeAlertWarnColors(): Pair<Color, Color> {
    return if (isAppInDarkTheme()) {
        Color(0xFF3D3018) to Color(0xFFFFDDB3)
    } else {
        WarnSoft to Warn
    }
}

@Composable
fun financeAlertLargeExpenseColors(): Pair<Color, Color> {
    return if (isAppInDarkTheme()) {
        Color(0xFF4A2C24) to Color(0xFFFFB4A8)
    } else {
        ExpenseSoft to Expense
    }
}

@Composable
fun financeAlertNudgeColors(): Pair<Color, Color> {
    return if (isAppInDarkTheme()) {
        Color(0xFF1E3A36) to Color(0xFFB8E8E3)
    } else {
        IncomeSoft to TealDark
    }
}
