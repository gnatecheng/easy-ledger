package com.qingjizhang.app.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.qingjizhang.app.domain.CategorySlice
import com.qingjizhang.app.domain.Dates
import com.qingjizhang.app.domain.Money
import com.qingjizhang.app.domain.MonthSummary
import java.io.File
import java.io.FileOutputStream

object MonthShare {
    fun shareImage(
        context: Context,
        summary: MonthSummary,
        slices: List<CategorySlice>,
    ) {
        val bmp = renderBitmap(summary, slices)
        val file = File(context.cacheDir, "轻记账-${summary.yearMonth}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        shareFile(context, file, "image/png", "分享月报图片")
    }

    fun sharePdf(
        context: Context,
        summary: MonthSummary,
        slices: List<CategorySlice>,
    ) {
        val bmp = renderBitmap(summary, slices)
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, 1).create()
        val page = doc.startPage(pageInfo)
        page.canvas.drawBitmap(bmp, 0f, 0f, null)
        doc.finishPage(page)
        val file = File(context.cacheDir, "轻记账-${summary.yearMonth}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        shareFile(context, file, "application/pdf", "导出月报 PDF")
    }

    private fun shareFile(context: Context, file: File, mime: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    internal fun renderBitmap(summary: MonthSummary, slices: List<CategorySlice>): Bitmap {
        val width = 1080
        val topCats = slices.take(6)
        val height = 720 + topCats.size * 72
        val bmp = createBitmap(width, height)
        val c = Canvas(bmp)
        c.drawColor(0xFFFAF7F2.toInt())

        val title = paint(48f, true, 0xFF1F2A2A.toInt())
        val muted = paint(28f, false, 0xFF5C6868.toInt())
        val value = paint(40f, true, 0xFF1F2A2A.toInt())
        val expense = paint(36f, true, 0xFFD45A3C.toInt())
        val income = paint(36f, true, 0xFF1B8A5A.toInt())
        val transfer = paint(32f, true, 0xFF3D7EA6.toInt())
        val brand = paint(26f, false, 0xFF1F6F6A.toInt())

        var y = 80f
        c.drawText("轻记账 · 月报", 64f, y, brand)
        y += 80f
        c.drawText(summary.yearMonth.format(Dates.ymCn), 64f, y, title)
        y += 36f
        c.drawText("本地记账，仅保存在这台手机", 64f, y + 24f, muted)

        y += 80f
        val card = Paint().apply { color = 0xFFFFFFFF.toInt(); isAntiAlias = true }
        val gap = 24f
        val cardW = (width - 64f * 2 - gap) / 2
        val cardH = 160f
        c.drawRoundRect(RectF(64f, y, 64f + cardW, y + cardH), 28f, 28f, card)
        c.drawRoundRect(RectF(64f + cardW + gap, y, width - 64f, y + cardH), 28f, 28f, card)
        c.drawText("本月支出", 88f, y + 52f, muted)
        c.drawText(Money.formatYuan(summary.expenseCents), 88f, y + 112f, expense)
        c.drawText("本月收入", 64f + cardW + gap + 24f, y + 52f, muted)
        c.drawText(Money.formatYuan(summary.incomeCents), 64f + cardW + gap + 24f, y + 112f, income)

        y += cardH + 24f
        c.drawRoundRect(RectF(64f, y, width - 64f, y + 150f), 28f, 28f, card)
        c.drawText("结余", 88f, y + 52f, muted)
        val balPaint = if (summary.balanceCents >= 0) income else expense
        c.drawText(Money.formatYuan(summary.balanceCents, withSign = true), 88f, y + 112f, balPaint)
        if (summary.transferCents > 0) {
            c.drawText("转账 ${Money.formatYuan(summary.transferCents)}（不计入收支）", 420f, y + 112f, transfer)
        }

        y += 190f
        c.drawText("支出分类 TOP", 64f, y, value)
        y += 28f
        if (topCats.isEmpty()) {
            c.drawText("本月暂无支出分类", 64f, y + 48f, muted)
        } else {
            val total = topCats.sumOf { it.amountCents }.coerceAtLeast(1)
            topCats.forEach { slice ->
                y += 72f
                c.drawRoundRect(RectF(64f, y - 40f, width - 64f, y + 20f), 20f, 20f, card)
                val bar = Paint().apply {
                    color = slice.category.colorArgb
                    isAntiAlias = true
                }
                val ratio = slice.amountCents.toFloat() / total
                c.drawRoundRect(RectF(88f, y - 8f, 88f + (width - 220f) * ratio, y + 8f), 10f, 10f, bar)
                val name = "${slice.category.emoji} ${slice.category.name}"
                c.drawText(name, 88f, y - 16f, paint(26f, true, 0xFF1F2A2A.toInt()))
                c.drawText(Money.formatYuan(slice.amountCents), width - 280f, y - 16f, muted)
            }
        }
        return bmp
    }

    private fun paint(size: Float, bold: Boolean, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
    }
}
