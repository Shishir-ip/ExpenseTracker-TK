package com.expensetracker.tk.tile

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.expensetracker.tk.MainActivity
import com.expensetracker.tk.data.Db
import com.expensetracker.tk.data.Prefs
import java.time.LocalDate

/** Quick-settings tile: shows today's total, opens the app on tap. */
class QuickTileService : TileService() {

    override fun onStartListening() {
        val tile = qsTile ?: return
        var today = 0.0
        var currency = "৳"
        var includeBills = false
        try {
            val prefs = Prefs(this)
            currency = prefs.currencySymbol
            includeBills = prefs.includeBills
            val exps = Db(this).allExpenses()
            val todayStr = LocalDate.now().toString()
            val relevant = if (includeBills) exps else exps.filter { it.category != "bills" }
            today = relevant.filter { it.date == todayStr }.sumOf { it.amount }
        } catch (e: Exception) { /* show zeros */ }

        tile.label = "Quick expense"
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = "Today: $currency${"%.0f".format(today)}"
        }
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java)
            .setAction("com.expensetracker.tk.ADD")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            val pi = android.app.PendingIntent.getActivity(
                this, 42, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
