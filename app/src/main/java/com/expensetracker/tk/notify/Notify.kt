package com.expensetracker.tk.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expensetracker.tk.MainActivity
import com.expensetracker.tk.R
import com.expensetracker.tk.TrackerApp
import com.expensetracker.tk.data.BorrowLend
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object Notify {
    const val CH_DAILY = "daily_summary"
    const val CH_BORROW = "borrow_reminders"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_DAILY, "Daily spend summary", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A daily recap of what you spent today"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_BORROW, "Borrow / lend reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders for money you borrowed or lent"
            }
        )
    }

    fun canNotify(ctx: Context): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun show(ctx: Context, id: Int, channel: String, title: String, text: String) {
        if (!canNotify(ctx)) return
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notif = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_taka)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notif)
    }

    fun scheduleDaily(ctx: Context) {
        val app = ctx.applicationContext as TrackerApp
        val wm = WorkManager.getInstance(ctx)
        if (!app.prefs.dailyReminder) {
            wm.cancelUniqueWork("daily_summary")
            return
        }
        val hour = app.prefs.dailyReminderHour
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(hour, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next)
        val req = PeriodicWorkRequestBuilder<DailyWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay.toMinutes() + 1, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork("daily_summary", ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun scheduleBorrowReminders(ctx: Context, all: List<BorrowLend>) {
        val wm = WorkManager.getInstance(ctx)
        wm.cancelUniqueWork("borrow_reminder_batch")
        val pending = all.filter { it.status == "pending" && it.reminderDate != null }
        if (pending.isEmpty()) return
        val target = LocalDate.parse(pending.map { it.reminderDate!! }.min(), DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        val delayDays = if (target.isAfter(today)) Duration.between(LocalDate.now().atStartOfDay(), target.atStartOfDay()).toDays() else 0L
        val req = OneTimeWorkRequestBuilder<BorrowWorker>()
            .setInitialDelay(delayDays, TimeUnit.DAYS)
            .build()
        wm.enqueueUniqueWork("borrow_reminder_batch", androidx.work.ExistingWorkPolicy.REPLACE, req)
    }

    class DailyWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
        override suspend fun doWork(): Result {
            val app = applicationContext as TrackerApp
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val exps = app.db.allExpenses().filter { it.date == today && (app.prefs.includeBills || it.category != "bills") }
            val total = exps.sumOf { it.amount }
            val sym = app.prefs.currencySymbol
            val msg = if (exps.isEmpty()) "No expenses recorded today. Keep it up! 🎯"
            else "You spent $sym${String.format("%.0f", total)} across ${exps.size} transaction(s) today."
            show(applicationContext, 1001, CH_DAILY, "Today's spending", msg)
            return Result.success()
        }
    }

    class BorrowWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
        override suspend fun doWork(): Result {
            val app = applicationContext as TrackerApp
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val sym = app.prefs.currencySymbol
            app.db.allBorrows().filter { it.status == "pending" && it.reminderDate != null && it.reminderDate!! <= today }
                .forEachIndexed { i, b ->
                    val verb = if (b.type == "borrow") "You owe" else "You are owed"
                    show(applicationContext, 2000 + i, CH_BORROW,
                        "Reminder: ${b.personName}",
                        "$verb $sym${String.format("%.0f", b.amount)} (${if (b.type == "borrow") "borrowed" else "lent"} on ${b.date})")
                }
            return Result.success()
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? TrackerApp ?: return
            app.sync.scheduleBackground()
            Notify.scheduleDaily(context)
            Notify.scheduleBorrowReminders(context, app.db.allBorrows())
            com.expensetracker.tk.data.Recur.schedule(context)
            com.expensetracker.tk.data.Backup.schedule(context)
            com.expensetracker.tk.widget.SpendWidgetProvider.push(context)
        }
    }
}
