package com.expensetracker.tk.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expensetracker.tk.TrackerApp
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Weekly automatic JSON snapshot to a user-picked folder (SAF tree URI). */
object Backup {

    private const val KEEP_SNAPSHOTS = 8

    fun schedule(ctx: Context) {
        val app = ctx.applicationContext as TrackerApp
        val wm = WorkManager.getInstance(ctx)
        if (!app.prefs.autoBackup) {
            wm.cancelUniqueWork("auto_backup")
            return
        }
        val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build()
        wm.enqueueUniquePeriodicWork("auto_backup", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun backupDir(ctx: Context): File = File(ctx.filesDir, "backups")

    /** Writes expense-backup-<date>.json into the app's private backups folder. */
    fun writeBackup(ctx: Context): File? {
        return try {
            val app = ctx.applicationContext as TrackerApp
            val dir = backupDir(ctx).apply { mkdirs() }
            val file = File(dir, "expense-backup-" + LocalDate.now().toString() + ".json")
            file.writeText(app.db.exportJson(app.prefs.currencySymbol).toString(2))
            // keep only the newest snapshots
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(KEEP_SNAPSHOTS)?.forEach { it.delete() }
            app.prefs.lastBackup = System.currentTimeMillis()
            file
        } catch (e: Exception) {
            null
        }
    }

    fun latestBackupFile(ctx: Context): File? =
        backupDir(ctx).listFiles()?.maxByOrNull { it.lastModified() }

    class AutoBackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
        override suspend fun doWork(): Result {
            val app = applicationContext as? TrackerApp ?: return Result.success()
            if (!app.prefs.autoBackup) return Result.success()
            return if (writeBackup(applicationContext) != null) Result.success() else Result.retry()
        }
    }
}
