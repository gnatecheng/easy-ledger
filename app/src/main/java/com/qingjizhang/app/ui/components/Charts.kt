package com.qingjizhang.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qingjizhang.app.R
import com.qingjizhang.app.domain.BudgetStatus
import com.qingjizhang.app.domain.CategorySlice
import com.qingjizhang.app.domain.MonthSummary
import com.qingjizhang.app.ui.i18n.AppFormatters
import com.qingjizhang.app.ui.i18n.displayName
import com.qingjizhang.app.ui.i18n.localizedCategoryName
import com.qingjizhang.app.ui.i18n.pieChartOtherCategoryName
import com.qingjizhang.app.ui.theme.Expense
import com.qingjizhang.app.ui.theme.Income
import com.qingjizhang.app.ui.theme.Over
import com.qingjizhang.app.ui.theme.Warn
import kotlin.math.min

@Composable
fun CategoryPieChart(
    slices: List<CategorySlice>,
    modifier: Modifier = Modifier,
    centerLabel: String = stringResource(R.string.chart_expense),
) {
    if (slices.isEmpty()) {
        EmptyHint(stringResource(R.string.chart_no_data))
        return
    }
    val otherName = pieChartOtherCategoryName()
    val top = slices.take(7)
    val rest = slices.drop(7)
    val display = if (rest.isEmpty()) top else {
        val otherAmount = rest.sumOf { it.amountCents }
        val total = slices.sumOf { it.amountCents }.coerceAtLeast(1)
        top + CategorySlice(
            category = top.last().category.copy(id = -1, name = otherName, colorArgb = 0xFF9A948A.toInt(), emoji = "…"),
            amountCents = otherAmount,
            ratio = otherAmount.toFloat() / total,
        )
    }
    val total = slices.sumOf { it.amountCents }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(160.dp)) {
                val stroke = 28.dp.toPx()
                var start = -90f
                display.forEach { slice ->
                    val sweep = slice.ratio * 360f
                    drawArc(
                        color = Color(slice.category.colorArgb),
                        startAngle = start,
                        sweepAngle = sweep.coerceAtLeast(1.2f),
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                        size = Size(size.width - stroke, size.height - stroke),
                        topLeft = Offset(stroke / 2, stroke / 2),
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centerLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(AppFormatters.formatYuan(total), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            display.forEach { slice ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    ColorDot(slice.category.colorArgb, size = 8)
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            localizedCategoryName(slice.category.name),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            lineHeight = 16.sp,
                        )
                        Text(
                            AppFormatters.formatYuan(slice.amountCents),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrendChart(months: List<MonthSummary>, modifier: Modifier = Modifier) {
    if (months.isEmpty()) {
        EmptyHint(stringResource(R.string.chart_no_trend))
        return
    }
    val max = months.maxOf { maxOf(it.incomeCents, it.expenseCents) }.coerceAtLeast(1)
    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 4.dp),
        ) {
            val n = months.size
            val groupW = size.width / n
            val barW = groupW * 0.28f
            months.forEachIndexed { i, m ->
                val cx = groupW * i + groupW / 2f
                val incH = (m.incomeCents.toFloat() / max) * (size.height - 8.dp.toPx())
                val expH = (m.expenseCents.toFloat() / max) * (size.height - 8.dp.toPx())
                drawRoundRect(
                    color = Income,
                    topLeft = Offset(cx - barW - 3.dp.toPx(), size.height - incH),
                    size = Size(barW, incH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                )
                drawRoundRect(
                    color = Expense,
                    topLeft = Offset(cx + 3.dp.toPx(), size.height - expH),
                    size = Size(barW, expH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            months.forEach { m ->
                Text(
                    AppFormatters.shortMonth(m.yearMonth),
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendSwatch(Income, stringResource(R.string.chart_income))
            LegendSwatch(Expense, stringResource(R.string.chart_expense))
        }
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BudgetBar(status: BudgetStatus) {
    val color = when {
        status.over -> Over
        status.warn -> Warn
        else -> Income
    }
    val name = status.category?.displayName() ?: stringResource(R.string.monthly_total_budget)
    val emoji = status.category?.emoji ?: "🎯"
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (status.category != null) {
                ColorDot(status.category.colorArgb)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                "$emoji $name",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                lineHeight = 18.sp,
            )
            Text(
                "${AppFormatters.formatYuan(status.spentCents)} / ${AppFormatters.formatYuan(status.budget.amountCents)}",
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { min(status.ratio, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.16f),
        )
        val hint = when {
            status.over -> stringResource(
                R.string.budget_over,
                AppFormatters.formatYuan(status.spentCents - status.budget.amountCents),
            )
            status.warn -> stringResource(R.string.budget_warn, (status.ratio * 100).toInt())
            status.budget.amountCents > 0 -> stringResource(
                R.string.budget_remaining,
                AppFormatters.formatYuan((status.budget.amountCents - status.spentCents).coerceAtLeast(0)),
            )
            else -> stringResource(R.string.budget_unset)
        }
        Spacer(Modifier.height(4.dp))
        Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
