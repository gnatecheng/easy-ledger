package com.qingjizhang.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingjizhang.app.domain.Dates
import com.qingjizhang.app.domain.Money
import com.qingjizhang.app.domain.Txn
import com.qingjizhang.app.domain.TxnKind
import com.qingjizhang.app.ui.theme.Expense
import com.qingjizhang.app.ui.theme.Income
import com.qingjizhang.app.ui.theme.InkMuted
import com.qingjizhang.app.ui.theme.Transfer
import com.qingjizhang.app.ui.theme.TransferSoft
import java.time.YearMonth

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun MonthSwitcher(
    month: YearMonth,
    modifier: Modifier = Modifier,
    onChange: (YearMonth) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
        }
        Text(
            month.format(Dates.ymCn),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = { onChange(month.plusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
        }
    }
}

@Composable
fun ColorDot(color: Int, modifier: Modifier = Modifier, size: Int = 10) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(color)),
    )
}

@Composable
fun MoneyText(cents: Long, kind: TxnKind? = null, large: Boolean = false) {
    val color = when (kind) {
        TxnKind.INCOME -> Income
        TxnKind.EXPENSE -> Expense
        TxnKind.TRANSFER -> Transfer
        null -> if (cents >= 0) Income else Expense
    }
    val value = when (kind) {
        TxnKind.INCOME -> cents
        TxnKind.EXPENSE -> -cents
        TxnKind.TRANSFER -> cents
        null -> cents
    }
    val withSign = when (kind) {
        TxnKind.TRANSFER -> false
        else -> kind != null || cents != 0L
    }
    Text(
        text = Money.formatYuan(value, withSign = withSign),
        color = color,
        fontWeight = FontWeight.SemiBold,
        fontSize = if (large) 22.sp else 16.sp,
    )
}

@Composable
fun TxnRow(txn: Txn, onClick: () -> Unit) {
    val isTransfer = txn.kind == TxnKind.TRANSFER
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isTransfer) TransferSoft
                    else Color(txn.categoryColor).copy(alpha = 0.16f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (isTransfer) "🔁" else txn.categoryEmoji.ifBlank { "•" }, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(if (isTransfer) Transfer.toArgb() else txn.categoryColor)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isTransfer) "转账" else txn.categoryName,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isTransfer) Transfer else Color.Unspecified,
                )
            }
            val sub = buildString {
                append(txn.dateTime.toLocalTime().toString().take(5))
                append(" · ")
                if (isTransfer) {
                    append(txn.accountName.ifBlank { "账户" })
                    append(" → ")
                    append(txn.transferToAccountName.ifBlank { "账户" })
                } else {
                    append(txn.accountName)
                }
                if (txn.note.isNotBlank()) {
                    append(" · ")
                    append(txn.note)
                }
            }
            Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!txn.receiptPath.isNullOrBlank()) {
            Text("📷", modifier = Modifier.padding(end = 6.dp))
        }
        MoneyText(txn.amountCents, txn.kind)
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = InkMuted)
    }
}

@Composable
fun DeltaChip(current: Long, previous: Long) {
    val delta = current - previous
    val label = when {
        previous == 0L && current == 0L -> "与上月持平"
        previous == 0L -> "新产生"
        else -> {
            val pct = ((delta.toDouble() / kotlin.math.abs(previous)) * 100).toInt()
            val arrow = if (delta >= 0) "↑" else "↓"
            "较上月 $arrow${kotlin.math.abs(pct)}%"
        }
    }
    Text(label, style = MaterialTheme.typography.labelSmall, color = InkMuted)
}

@Composable
fun SectionTitle(text: String, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun Banner(
    title: String,
    body: String,
    container: Color,
    content: Color,
    onDismiss: (() -> Unit)? = null,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(14.dp),
    ) {
        Column(Modifier.padding(end = 16.dp)) {
            Text(title, color = content, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(body, color = content.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
        }
        if (onDismiss != null) {
            Text(
                "×",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(onClick = onDismiss)
                    .padding(4.dp),
                color = content,
            )
        }
    }
}
