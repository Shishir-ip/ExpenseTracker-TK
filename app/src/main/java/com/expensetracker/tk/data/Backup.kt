package com.expensetracker.tk.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expensetracker.tk.TrackerApp
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Weekly automatic JSON snapshot to a user-picked folder (SAF tree URI). */
object Backup {

    fun schedule(ctx: Context) {
        val app = ctx.applicationContext as TrackerApp
        val wm = WorkManager.getInstance(ctx)
        if (!app.prefs.autoBackup || app.prefs.backupDir.isBlank()) {
            wm.cancelUniqueWork("auto_backup")
            return
        }
        val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build()
        wm.enqueueUniquePeriodicWork("auto_backup", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Writes expense-tracker-backup-<date>.json into the chosen folder. */
    fun writeBackup(ctx: Context): Boolean {
        return try {
            val app = ctx.applicationContext as TrackerApp
            if (app.prefs.backupDir.isBlank()) return false
            val treeUri = Uri.parse(app.prefs.backupDir)
            val resolver = ctx.contentResolver

            val name = "expense-backup-" + LocalDate.now().toString() + ".json"
            val docUri = DocumentsContract.createDocument(resolver, treeUri, "application/json", name)
                ?: return false
            val json = app.db.exportJson(app.prefs.currencySymbol).toString(2)
            resolver.openOutputStream(docUri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) } ?: return false
            app.prefs.lastBackup = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            false
        }
    }

    class AutoBackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
        override suspend fun doWork(): Result {
            val app = applicationContext as? TrackerApp ?: return Result.success()
            if (!app.prefs.autoBackup || app.prefs.backupDir.isBlank()) return Result.success()
            return if (writeBackup(applicationContext)) Result.success() else Result.retry()
        }
    }
}
