package com.expensetracker.tk.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.expensetracker.tk.TrackerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class SyncState { OFFLINE, SYNCING, SYNCED, ERROR }

/**
 * Local-first sync: every write lands in SQLite immediately (marked dirty) and is
 * replayed to Supabase when a connection is available. Remote rows are then pulled
 * back and merged (last-write-wins on updated_at, local dirty rows always win).
 */
class SyncEngine(
    private val context: Context,
    private val db: Db,
    private val prefs: Prefs
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = AtomicBoolean(false)
    private val rerun = AtomicBoolean(false)

    var onState: ((SyncState, Int, String?) -> Unit)? = null

    fun pendingCount(): Int {
        val e = db.dirtyExpenses().size
        val b = db.dirtyBorrows().size
        return e + b
    }

    fun requestSync(manual: Boolean = false) {
        if (!prefs.cloudConfigured) {
            onState?.invoke(SyncState.OFFLINE, pendingCount(), null)
            return
        }
        scope.launch {
            syncNow()
        }
    }

    suspend fun syncNow(): Boolean = withContext(Dispatchers.IO) {
        if (!running.compareAndSet(false, true)) {
            rerun.set(true)  // a sync is already running; it will pick up new changes
            return@withContext true
        }
        var ok = false
        var err: String? = null
        try {
            onState?.invoke(SyncState.SYNCING, pendingCount(), null)
            val client = SupabaseRest.fromPrefs(prefs) ?: return@withContext false
            val uk = prefs.userKey

            // ---- PUSH expenses ----
            val dirty = db.dirtyExpenses()
            val alive = JSONArray()
            val dead = ArrayList<String>()
            dirty.forEach { e ->
                if (e.deleted) dead.add(e.id) else alive.put(e.toJson(uk))
            }
            if (alive.length() > 0) client.upsert("expenses", alive)
            dead.forEach { id -> client.delete("expenses", "id=eq.$id") }
            dirty.forEach { e ->
                if (e.deleted) db.purgeExpense(e.id) else db.markExpenseSynced(e.id)
            }

            // ---- PUSH borrow_lend ----
            val dirtyBl = db.dirtyBorrows()
            val aliveBl = JSONArray()
            val deadBl = ArrayList<String>()
            dirtyBl.forEach { b ->
                if (b.deleted) deadBl.add(b.id) else aliveBl.put(b.toJson(uk))
            }
            if (aliveBl.length() > 0) client.upsert("borrow_lend", aliveBl)
            deadBl.forEach { id -> client.delete("borrow_lend", "id=eq.$id") }
            dirtyBl.forEach { b ->
                if (b.deleted) db.purgeBorrow(b.id) else db.markBorrowSynced(b.id)
            }

            // ---- PUSH budgets (full replace — small table, mirrors web app behaviour) ----
            val localBudgets = db.allBudgets()
            client.delete("budgets", "user_pin=eq.$uk")
            if (localBudgets.isNotEmpty()) {
                val arr = JSONArray()
                localBudgets.forEach { b ->
                    arr.put(JSONObject().apply {
                        put("user_pin", uk)
                        put("category", b.category)
                        put("amount", b.amount)
                        put("year_month", if (b.yearMonth.isBlank()) JSONObject.NULL else b.yearMonth)
                    })
                }
                client.upsert("budgets", arr)
            }

            // ---- PULL expenses ----
            val remoteExp = client.select("expenses", "select=*&user_pin=eq.$uk")
            val localById = db.allExpenses(includeDeleted = true).associateBy { it.id }
            val remoteIds = HashSet<String>()
            for (i in 0 until remoteExp.length()) {
                val o = remoteExp.getJSONObject(i)
                val id = o.optString("id")
                if (id.isBlank()) continue
                remoteIds.add(id)
                val local = localById[id]
                if (local != null && local.dirty) continue  // local pending change wins
                val remoteUpdated = o.optLong("updated_at", 0L)
                // rows with no remote timestamp always refresh; timestamped ones only if newer
                if (local != null && remoteUpdated > 0 && local.updatedAt >= remoteUpdated && !local.dirty) continue
                db.upsertExpense(
                    Expense(
                        id = id,
                        title = o.optString("title", ""),
                        amount = o.optDouble("amount", 0.0),
                        category = o.optString("category", "other"),
                        date = o.optString("date", "").take(10),
                        notes = o.optString("notes", ""),
                        isRecurring = o.optBoolean("is_recurring", false),
                        createdAt = o.optLong("created_at", 0L) * if (o.opt("created_at") is String) 1 else 1000L,
                        updatedAt = remoteUpdated,
                        dirty = false, deleted = false,
                        recurDays = local?.recurDays ?: 0,
                        recurParent = local?.recurParent
                    )
                )
            }
            // remove clean local rows that no longer exist remotely
            localById.values.forEach { l ->
                if (!l.dirty && l.id !in remoteIds && !l.deleted) db.purgeExpense(l.id)
            }
            db.purgeDeletedExpenses()

            // ---- PULL borrow_lend ----
            val remoteBl = client.select("borrow_lend", "select=*&user_pin=eq.$uk")
            val localBlById = db.allBorrows(includeDeleted = true).associateBy { it.id }
            val remoteBlIds = HashSet<String>()
            for (i in 0 until remoteBl.length()) {
                val o = remoteBl.getJSONObject(i)
                val id = o.optString("id")
                if (id.isBlank()) continue
                remoteBlIds.add(id)
                val local = localBlById[id]
                if (local != null && local.dirty) continue
                val remoteUpdated = o.optLong("updated_at", 0L)
                if (local != null && remoteUpdated > 0 && local.updatedAt >= remoteUpdated && !local.dirty) continue
                db.upsertBorrow(
                    BorrowLend(
                        id = id,
                        type = o.optString("type", "borrow"),
                        personName = o.optString("person_name", ""),
                        amount = o.optDouble("amount", 0.0),
                        date = o.optString("date", "").take(10),
                        reminderDate = if (o.isNull("reminder_date")) null else o.optString("reminder_date")?.take(10),
                        status = o.optString("status", "pending"),
                        notes = o.optString("notes", ""),
                        updatedAt = remoteUpdated,
                        dirty = false, deleted = false
                    )
                )
            }
            localBlById.values.forEach { l ->
                if (!l.dirty && l.id !in remoteBlIds && !l.deleted) db.purgeBorrow(l.id)
            }
            db.purgeDeletedBorrows()

            // ---- PULL budgets ----
            val remoteBud = client.select("budgets", "select=*&user_pin=eq.$uk")
            val pulledBudgets = ArrayList<BudgetRow>()
            for (i in 0 until remoteBud.length()) {
                val o = remoteBud.getJSONObject(i)
                val cat = o.optString("category", "")
                if (cat.isBlank()) continue
                pulledBudgets.add(BudgetRow(o.optString("id", UUID.randomUUID().toString()), cat, o.optDouble("amount", 0.0), o.optString("year_month", "")))
            }
            db.replaceBudgets(pulledBudgets)

            prefs.lastSync = System.currentTimeMillis()
            ok = true
            onState?.invoke(SyncState.SYNCED, 0, null)
        } catch (e: Exception) {
            err = e.message
            onState?.invoke(SyncState.ERROR, pendingCount(), e.message)
        } finally {
            running.set(false)
        }
        // changes queued while we were syncing → run one more pass
        if (ok && rerun.compareAndSet(true, false) && pendingCount() > 0) {
            return@withContext syncNow()
        }
        ok
    }

    fun scheduleBackground() {
        val wm = WorkManager.getInstance(context)
        val req = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork("tracker_sync", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    class SyncWorker(ctx: Context, params: androidx.work.WorkerParameters) : CoroutineWorker(ctx, params) {
        override suspend fun doWork(): Result {
            val app = applicationContext as TrackerApp
            if (!app.prefs.cloudConfigured) return Result.success()
            return if (app.sync.syncNow()) Result.success() else Result.retry()
        }
    }
}
