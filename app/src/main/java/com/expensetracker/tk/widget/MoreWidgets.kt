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

/**
 * Large 4×2 widget: today + month totals, monthly-target progress bar.
 * Pushed together with the small widget on every data change.
 */
class SpendWidgetLargeProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = SpendWidgetProvider.buildLargeViews(context)
        ids.forEach { id -> manager.updateAppWidget(id, views) }
    }
}

/** Tiny 2×1 widget: one-tap path to the add-expense form. */
class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_expense_quick).apply {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = "com.expensetracker.tk.ADD"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context, 43, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setOnClickPendingIntent(R.id.wq_root, pi)
        }
        ids.forEach { id -> manager.updateAppWidget(id, views) }
    }
}
