package com.expensetracker.tk.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expensetracker.tk.TrackerApp
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Recurring expenses: an expense marked recurring with an interval (days) acts as a
 * template. A daily job copies it to today when today is an occurrence day.
 * recur_days / recur_parent are local-only columns and never pushed to Supabase.
 */
object Recur {

    const val INTERVAL_DAILY = 1
    const val INTERVAL_WEEKLY = 7
    const val INTERVAL_MONTHLY = 30   // ~monthly, day-aligned every 30 days

    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<RecurWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("recur_job", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Create today's copies for any due recurring templates. Returns count created. */
    fun runForToday(app: TrackerApp): Int {
        val db = app.db
        val today = LocalDate.now()
        val todayStr = today.toString()
        var created = 0
        val templates = db.allExpenses().filter { it.isRecurring && it.recurDays > 0 && it.recurParent == null }
        val all = db.allExpenses()
        for (t in templates) {
            val step = t.recurDays.toLong()
            if (step <= 0) continue
            val start = try { LocalDate.parse(t.date.take(10)) } catch (e: Exception) { continue }
            if (!start.isBefore(today)) continue  // template day itself already logged

            var d = start
            while (d.isBefore(today)) d = d.plusDays(step)
            if (d != today) continue  // not an occurrence day

            val already = all.any { it.recurParent == t.id && it.date == todayStr }
            if (already) continue

            val now = System.currentTimeMillis()
            db.insertExpense(
                t.copy(
                    id = UUID.randomUUID().toString(),
                    date = todayStr,
                    isRecurring = false,
                    recurDays = 0,
                    recurParent = t.id,
                    dirty = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
            created++
        }
        return created
    }

    fun exportInfo(expenses: List<Expense>): List<Pair<String, Int>> =
        expenses.filter { it.isRecurring && it.recurDays > 0 }.map { it.title to it.recurDays }

    fun infoJson(expenses: List<Expense>): JSONObject = JSONObject().apply {
        exportInfo(expenses).forEach { (title, days) -> put(title, days) }
    }

    class RecurWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
        override suspend fun doWork(): Result {
            val app = applicationContext as? TrackerApp ?: return Result.success()
            val created = runForToday(app)
            if (created > 0) {
                app.sync.requestSync()
            }
            return Result.success()
        }
    }
}
