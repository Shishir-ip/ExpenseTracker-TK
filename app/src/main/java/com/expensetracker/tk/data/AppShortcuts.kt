package com.expensetracker.tk.data

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.expensetracker.tk.MainActivity
import com.expensetracker.tk.TrackerApp
import com.expensetracker.tk.R

/**
 * Maintains a dynamic launcher shortcut that one-tap logs the user's
 * most-logged quick preset ("Log Khichuri"). Refreshed on app start.
 */
object AppShortcuts {

    fun updateTopPreset(context: Context) {
        try {
            val app = context.applicationContext as? TrackerApp ?: return
            val presets = app.prefs.presets
            if (presets.isEmpty()) return
            val recent = app.db.allExpenses().take(80)
            val best = presets.maxByOrNull { p ->
                recent.count { it.title.equals(p.title, ignoreCase = true) || it.category == p.category }
            } ?: presets.first()

            val intent = Intent(context, MainActivity::class.java).apply {
                action = "com.expensetracker.tk.LOG_PRESET"
                putExtra("preset_id", best.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val si = ShortcutInfoCompat.Builder(context, "log_" + best.id)
                .setShortLabel(("Log " + best.label).take(18))
                .setLongLabel("Log " + best.label + " (" + app.prefs.currencySymbol + String.format("%.0f", best.amount) + ")")
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_add))
                .setIntent(intent)
                .build()
            ShortcutManagerCompat.setDynamicShortcuts(context, listOf(si))
        } catch (e: Exception) {
            // shortcuts are a bonus — never fail the app over them
        }
    }
}
