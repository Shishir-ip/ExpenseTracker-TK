package com.expensetracker.tk.data

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.expensetracker.tk.state.AppState
import java.io.File
import java.time.LocalDate

/** One-page A4 monthly statement: stats, top categories, recent transactions. */
object ReportGenerator {

    /**
     * Square share card (1080x1080 PNG): donut + top categories + credit.
     * slices = Triple(label, colorLong, value).
     */
    fun chartImage(
        context: Context,
        title: String,
        subtitle: String,
        slices: List<Triple<String, Long, Double>>,
        total: Double,
        currency: String
    ): File? {
        return try {
            val W = 1080
            val H = 1080
            val bmp = android.graphics.Bitmap.createBitmap(W, H, android.graphics.Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(bmp)
            c.drawColor(Color.parseColor("#0F172A"))

            fun paint(sizePx: Float, bold: Boolean, color: Int): Paint = Paint().apply {
                isAntiAlias = true; textSize = sizePx; this.color = color
                typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            }
            val white = paint(46f, true, Color.WHITE)
            val sub = paint(30f, false, Color.parseColor("#94A3B8"))
            val label = paint(34f, true, Color.WHITE)
            val money = paint(34f, true, Color.WHITE)
            val center = paint(30f, true, Color.parseColor("#94A3B8"))
            val big = paint(72f, true, Color.WHITE)

            c.drawText(title, 60f, 110f, white)
            c.drawText(subtitle, 60f, 170f, sub)

            // donut
            val donut = Paint().apply {
                style = Paint.Style.STROKE; strokeWidth = 100f; isAntiAlias = true
            }
            var start = -90f
            if (total > 0) {
                slices.forEach { (_, colorLong, value) ->
                    val a = (value / total).toFloat() * 360f
                    val drawn = a - 6f
                    if (drawn > 1f) {
                        donut.color = colorLong.toInt()
                        c.drawArc(240f, 200f, 840f, 800f, start + 3f, drawn, false, donut)
                    }
                    start += a
                }
            } else {
                donut.color = Color.parseColor("#334155")
                c.drawArc(240f, 200f, 840f, 800f, 0f, 360f, false, donut)
            }
            c.drawText("TOTAL", 540f, 470f, center)
            c.drawText("$currency${"%,.0f".format(total)}", 540f, 555f, big)

            // legend
            var y = 760f
            slices.take(5).forEach { (name, colorLong, value) ->
                val dot = Paint().apply { color = colorLong.toInt(); isAntiAlias = true }
                c.drawCircle(90f, y - 11f, 16f, dot)
                c.drawText(name, 130f, y, label)
                val amt = paint(34f, true, Color.WHITE).apply {
                    textAlign = Paint.Align.RIGHT
                    c.drawText("$currency${"%,.0f".format(value)}", W - 80f, y, this)
                }
                y += 62f
            }

            // footer
            c.drawText("Developed by SHISHIR  ·  github.com/Shishir-ip", 60f, H - 50f, sub)

            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "spending-share-" + System.currentTimeMillis() + ".png")
            file.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
            file
        } catch (e: Exception) {
            null
        }
    }

    fun generate(context: Context, state: AppState): File? {
        return try {
            val ui = state.ui.value
            val currency = state.prefs.currencySymbol
            val exps = state.calcExpenses()
            val today = LocalDate.now()
            val monthStr = today.toString().substring(0, 7)
            val monthName = "${AppState.MONTHS[today.monthValue - 1]} ${today.year}"

            val monthExps = exps.filter { it.date.startsWith(monthStr) }
            val monthTotal = monthExps.sumOf { it.amount }
            val todayTotal = monthExps.filter { it.date == today.toString() }.sumOf { it.amount }
            val dailyAvg = monthTotal / today.dayOfMonth.coerceAtLeast(1)
            val topCats = monthExps.groupBy { it.category }
                .map { (k, v) -> Categories.byId(k).name to v.sumOf { it.amount } }
                .sortedByDescending { it.second }
                .take(5)
            val recent = exps.take(12)

            val doc = PdfDocument()
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val c = page.canvas
            val W = 595f

            fun paint(sizePx: Float, bold: Boolean, color: Int): Paint = Paint().apply {
                isAntiAlias = true; textSize = sizePx; this.color = color
                typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            }
            val white = paint(14f, true, Color.WHITE)
            val h1 = paint(22f, true, Color.parseColor("#4338CA"))
            val sub = paint(11f, false, Color.parseColor("#64748B"))
            val label = paint(11f, true, Color.parseColor("#334155"))
            val body = paint(11f, false, Color.parseColor("#475569"))
            val money = paint(13f, true, Color.parseColor("#EF4444"))
            val green = paint(13f, true, Color.parseColor("#10B981"))
            val line = Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 1f }

            // header band
            val band = Paint().apply { color = Color.parseColor("#4338CA") }
            c.drawRect(0f, 0f, W, 110f, band)
            c.drawText("Expense Report", 32f, 52f, paint(26f, true, Color.WHITE))
            c.drawText("$monthName  ·  generated ${today}", 32f, 78f, paint(12f, false, Color.parseColor("#C7D2FE")))

            // stat row
            var y = 150f
            c.drawText("TODAY", 32f, y, sub)
            c.drawText("$currency${"%,.0f".format(todayTotal)}", 32f, y + 20f, label)
            c.drawText("THIS MONTH", 232f, y, sub)
            c.drawText("$currency${"%,.0f".format(monthTotal)}", 232f, y + 20f, green)
            c.drawText("DAILY AVG", 432f, y, sub)
            c.drawText("$currency${"%,.0f".format(dailyAvg)}", 432f, y + 20f, label)
            y += 52f
            c.drawLine(32f, y, W - 32f, y, line)
            y += 24f

            // target status
            val target = ui.monthlyTarget
            if (target > 0 && ui.monthlyTargetMonth == monthStr) {
                val totalTarget = target * today.lengthOfMonth()
                val status = if (monthTotal > totalTarget) "OVER TARGET" else "ON TRACK"
                c.drawText("Monthly target: $currency${"%,.0f".format(target)}/day  ·  $currency${"%,.0f".format(totalTarget)} total", 32f, y, body)
                c.drawText(status, W - 130f, y, if (monthTotal > totalTarget) money else green)
                y += 24f
            }

            // top categories with bars
            c.drawText("TOP CATEGORIES", 32f, y, label)
            y += 16f
            val maxCat = topCats.maxOfOrNull { it.second } ?: 0.0
            topCats.forEach { (name, amount) ->
                c.drawText(name, 32f, y + 12f, body)
                val barW = if (maxCat > 0) ((amount / maxCat) * 280.0).toFloat() else 0f
                val bar = Paint().apply { color = Color.parseColor("#818CF8") }
                c.drawRect(150f, y + 2f, 150f + barW, y + 14f, bar)
                c.drawText("$currency${"%,.0f".format(amount)}", 445f, y + 12f, label)
                y += 24f
            }
            y += 10f
            c.drawLine(32f, y, W - 32f, y, line)
            y += 24f

            // recent transactions
            c.drawText("RECENT TRANSACTIONS", 32f, y, label)
            y += 18f
            recent.forEach { e ->
                val d = try { LocalDate.parse(e.date.take(10)) } catch (ex: Exception) { today }
                c.drawText("${d.dayOfMonth} ${AppState.MONTHS[d.monthValue - 1].take(3)}", 32f, y + 11f, sub)
                c.drawText(e.title.take(34), 90f, y + 11f, body)
                c.drawText("-$currency${"%,.2f".format(e.amount)}", W - 125f, y + 11f, money)
                y += 20f
            }

            // footer
            c.drawLine(32f, 790f, W - 32f, 790f, line)
            c.drawText("Developed by SHISHIR  ·  github.com/Shishir-ip", 32f, 812f, sub)

            doc.finishPage(page)
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "expense-report-${today.toString().substring(0, 7)}.pdf")
            file.outputStream().use { doc.writeTo(it) }
            doc.close()
            file
        } catch (e: Exception) {
            null
        }
    }
}
