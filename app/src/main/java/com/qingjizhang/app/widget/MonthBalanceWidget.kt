package com.qingjizhang.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.qingjizhang.app.MainActivity
import com.qingjizhang.app.QingJiZhangApp
import com.qingjizhang.app.R
import com.qingjizhang.app.domain.Dates
import com.qingjizhang.app.domain.Money
import com.qingjizhang.app.domain.TxnKind
import java.time.YearMonth
import java.util.concurrent.Executors

class MonthBalanceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        io.execute {
            try {
                render(context, appWidgetManager, appWidgetIds)
            } finally {
                Handler(Looper.getMainLooper()).post { pending.finish() }
            }
        }
    }

    companion object {
        private val io = Executors.newSingleThreadExecutor()

        fun refresh(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, MonthBalanceWidget::class.java))
            if (ids.isEmpty()) return
            io.execute { render(context.applicationContext, mgr, ids) }
        }

        private fun render(context: Context, mgr: AppWidgetManager, ids: IntArray) {
            val ym = YearMonth.now()
            val start = Dates.monthStart(ym)
            val end = Dates.monthEndExclusive(ym)
            val txns = try {
                val app = context.applicationContext as? QingJiZhangApp
                app?.container?.db?.transactions()?.getRangeSync(start, end).orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
            val income = txns.filter { it.kind == TxnKind.INCOME.name }.sumOf { it.amountCents }
            val expense = txns.filter { it.kind == TxnKind.EXPENSE.name }.sumOf { it.amountCents }
            val balance = income - expense
            val launch = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_month_balance)
                views.setTextViewText(R.id.widget_month, ym.format(Dates.ymCn))
                views.setTextViewText(R.id.widget_income, Money.formatYuan(income))
                views.setTextViewText(R.id.widget_expense, Money.formatYuan(expense))
                views.setTextViewText(R.id.widget_balance, Money.formatYuan(balance, withSign = true))
                views.setTextColor(
                    R.id.widget_balance,
                    if (balance >= 0) 0xFF1B8A5A.toInt() else 0xFFD45A3C.toInt(),
                )
                views.setOnClickPendingIntent(R.id.widget_root, launch)
                mgr.updateAppWidget(id, views)
            }
        }
    }
}
