package com.expensetracker.tk.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.expensetracker.tk.MainActivity
import com.expensetracker.tk.R
import com.expensetracker.tk.data.Db
import com.expensetracker.tk.data.Prefs
import java.time.LocalDate
import java.time.YearMonth

/** Home-screen widget: today + this month totals. Updates with every data change. */
class SpendWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = buildViews(context)
        ids.forEach { id -> manager.updateAppWidget(id, views) }
    }

    companion object {
        fun push(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                // small 2×1
                manager.getAppWidgetIds(ComponentName(context, SpendWidgetProvider::class.java)).takeIf { it.isNotEmpty() }?.forEach { id ->
                    manager.updateAppWidget(id, buildViews(context))
                }
                // large 4×2
                manager.getAppWidgetIds(ComponentName(context, SpendWidgetLargeProvider::class.java)).takeIf { it.isNotEmpty() }?.forEach { id ->
                    manager.updateAppWidget(id, buildLargeViews(context))
                }
                // quick-add 2×1 (static content, refresh tap target anyway)
                manager.getAppWidgetIds(ComponentName(context, QuickAddWidgetProvider::class.java)).takeIf { it.isNotEmpty() }?.forEach { id ->
                    manager.updateAppWidget(id, RemoteViews(context.packageName, R.layout.widget_expense_quick))
                }
            } catch (e: Exception) {
                // widget not placed / system busy — never crash the app over this
            }
        }

        /** Large widget: totals + monthly-target progress. */
        fun buildLargeViews(context: Context): RemoteViews {
            var today = 0.0
            var month = 0.0
            var pending = 0
            var currency = "৳"
            var includeBills = false
            var targetTotal = 0.0
            val prefs = Prefs(context)
            try {
                currency = prefs.currencySymbol
                includeBills = prefs.includeBills
                val db = Db(context)
                val exps = db.allExpenses()
                val todayStr = LocalDate.now().toString()
                val monthStr = todayStr.substring(0, 7)
                val relevant = if (includeBills) exps else exps.filter { it.category != "bills" }
                today = relevant.filter { it.date == todayStr }.sumOf { it.amount }
                month = relevant.filter { it.date.startsWith(monthStr) }.sumOf { it.amount }
                pending = exps.count { it.dirty }
                db.allBudgets().firstOrNull { it.category == "__monthly_target__" }?.let {
                    if (it.yearMonth == monthStr) targetTotal = it.amount * YearMonth.now().lengthOfMonth()
                }
            } catch (e: Exception) {
                // leave zeros on failure
            }

            // theme colours (shared with the small widget)
            val dark = when (prefs.themeMode) {
                1 -> false
                2, 4 -> true
                else -> (context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
            val bgRes = if (dark) R.drawable.widget_bg else R.drawable.widget_bg_light
            val cTitle = if (dark) 0xFF818CF8.toInt() else 0xFF6366F1.toInt()
            val cLabel = if (dark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()
            val cToday = if (dark) 0xFFF1F5F9.toInt() else 0xFF1E293B.toInt()
            val cMonth = if (dark) 0xFFA5B4FC.toInt() else 0xFF4F46E5.toInt()

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val progressPct = if (targetTotal > 0) ((month / targetTotal) * 100).toInt().coerceIn(0, 100) else 0
            val progressText = when {
                targetTotal <= 0.0 -> "No monthly target set"
                month > targetTotal -> "Over target by $currency${String.format("%.0f", month - targetTotal)}"
                else -> "$currency${String.format("%.0f", month)} of $currency${String.format("%.0f", targetTotal)} · $progressPct%"
            }

            return RemoteViews(context.packageName, R.layout.widget_expense_large).apply {
                setInt(R.id.wl_root, "setBackgroundResource", bgRes)
                setTextColor(R.id.wl_title, cTitle)
                setTextColor(R.id.wl_today_label, cLabel)
                setTextColor(R.id.wl_today_value, cToday)
                setTextColor(R.id.wl_month_title, cLabel)
                setTextColor(R.id.wl_month_value, cMonth)
                setTextColor(R.id.wl_month_label, cLabel)
                setTextColor(R.id.wl_progress_text, cLabel)
                setTextViewText(R.id.wl_today_value, "$currency${String.format("%.0f", today)}")
                setTextViewText(R.id.wl_month_value, "$currency${String.format("%.0f", month)}")
                setTextViewText(
                    R.id.wl_month_label,
                    YearMonth.now().month.name.lowercase().replaceFirstChar { it.uppercase() } +
                            if (pending > 0) "  ·  $pending pending" else ""
                )
                setProgressBar(R.id.wl_progress, 100, progressPct, false)
                setTextViewText(R.id.wl_progress_text, progressText)
                setOnClickPendingIntent(R.id.wl_root, pi)
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            var today = 0.0
            var month = 0.0
            var pending = 0
            var currency = "৳"
            var includeBills = false
            val prefs = Prefs(context)
            try {
                currency = prefs.currencySymbol
                includeBills = prefs.includeBills
                val exps = Db(context).allExpenses()
                val todayStr = LocalDate.now().toString()
                val monthStr = todayStr.substring(0, 7)
                val relevant = if (includeBills) exps else exps.filter { it.category != "bills" }
                today = relevant.filter { it.date == todayStr }.sumOf { it.amount }
                month = relevant.filter { it.date.startsWith(monthStr) }.sumOf { it.amount }
                pending = exps.count { it.dirty }
            } catch (e: Exception) {
                // leave zeros on failure
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // follow the app theme: light / dark / black / system
            val dark = when (prefs.themeMode) {
                1 -> false
                2, 4 -> true
                else -> (context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
            val bgRes = if (dark) R.drawable.widget_bg else R.drawable.widget_bg_light
            val cTitle = if (dark) 0xFF818CF8.toInt() else 0xFF6366F1.toInt()
            val cLabel = if (dark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()
            val cToday = if (dark) 0xFFF1F5F9.toInt() else 0xFF1E293B.toInt()
            val cMonth = if (dark) 0xFFA5B4FC.toInt() else 0xFF4F46E5.toInt()

            return RemoteViews(context.packageName, R.layout.widget_expense).apply {
                setInt(R.id.w_root, "setBackgroundResource", bgRes)
                setTextColor(R.id.w_title, cTitle)
                setTextColor(R.id.w_today_label, cLabel)
                setTextColor(R.id.w_today_value, cToday)
                setTextColor(R.id.w_month_label, cLabel)
                setTextColor(R.id.w_month_value, cMonth)
                setTextViewText(R.id.w_today_value, "$currency${String.format("%.0f", today)}")
                setTextViewText(R.id.w_month_value, "$currency${String.format("%.0f", month)}")
                setTextViewText(
                    R.id.w_month_label,
                    java.time.YearMonth.now().month.name.lowercase()
                        .replaceFirstChar { it.uppercase() } + if (pending > 0) "  ·  $pending pending" else ""
                )
                setOnClickPendingIntent(R.id.w_root, pi)
            }
        }
    }
}
