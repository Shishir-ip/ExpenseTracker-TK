package com.expensetracker.tk.state

import android.content.Context
import com.expensetracker.tk.TrackerApp
import com.expensetracker.tk.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Central state: everything the UI renders, plus all mutations (local-first). */
class AppState(private val app: TrackerApp) {

    private val db = app.db
    val prefs = app.prefs

    // ---------- observable state ----------
    data class Ui(
        val expenses: List<Expense> = emptyList(),
        val budgets: Map<String, Double> = emptyMap(),      // category -> amount
        val dailyLimit: Double = 0.0,
        val monthlyTarget: Double = 0.0,                    // daily avg target
        val monthlyTargetMonth: String = "",
        val borrows: List<BorrowLend> = emptyList(),
        val sync: SyncState = SyncState.OFFLINE,
        val pending: Int = 0,
        val syncError: String? = null,
        val includeBills: Boolean = false,
        val themeMode: Int = 0,
        val accentIndex: Int = 0,
        val onboarded: Boolean = true,
        val cardOrder: List<String> = Prefs.DEFAULT_ORDER,
        val presets: List<QuickPreset> = emptyList(),
        val reloadKey: Int = 0
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui

    /** Set by app shortcuts / quick tile ("add", "insights"); consumed by MainActivity. */
    val pendingUiAction = androidx.compose.runtime.mutableStateOf<String?>(null)

    init {
        app.sync.onState = { s, p, e ->
            _ui.value = _ui.value.copy(sync = s, pending = p, syncError = e)
            // surface pulled-from-cloud data immediately after a successful sync
            if (s == SyncState.SYNCED) reload()
        }
        reload()
        app.sync.requestSync()
    }

    fun reload() {
        val expenses = db.allExpenses()
        val budgets = HashMap<String, Double>()
        var dailyLimit = 0.0
        var monthlyTarget = 0.0
        var monthlyTargetMonth = ""
        db.allBudgets().forEach { b ->
            when {
                b.category == "__daily_limit__" -> dailyLimit = b.amount
                b.category == "__monthly_target__" -> { monthlyTarget = b.amount; monthlyTargetMonth = b.yearMonth }
                else -> budgets[b.category] = b.amount
            }
        }
        _ui.value = _ui.value.copy(
            expenses = expenses,
            budgets = budgets,
            dailyLimit = dailyLimit,
            monthlyTarget = monthlyTarget,
            monthlyTargetMonth = monthlyTargetMonth,
            borrows = db.allBorrows(),
            includeBills = prefs.includeBills,
            themeMode = prefs.themeMode,
            accentIndex = prefs.accentIndex,
            onboarded = prefs.onboarded,
            cardOrder = prefs.cardOrder,
            presets = prefs.presets,
            sync = if (prefs.cloudConfigured) _ui.value.sync else SyncState.OFFLINE,
            pending = syncPending(),
            reloadKey = _ui.value.reloadKey + 1
        )
        try { com.expensetracker.tk.widget.SpendWidgetProvider.push(app) } catch (e: Exception) { /* widget absent */ }
    }

    fun setAccent(index: Int) {
        prefs.accentIndex = index
        _ui.value = _ui.value.copy(accentIndex = index)
    }

    fun setOnboarded(v: Boolean) {
        prefs.onboarded = v
        _ui.value = _ui.value.copy(onboarded = v)
    }

    /** Theme is applied instantly — MainActivity observes this state. */
    fun setTheme(mode: Int) {
        prefs.themeMode = mode
        _ui.value = _ui.value.copy(themeMode = mode)
    }

    /** Undo a delete: flip the soft-delete flags back and re-push on next sync. */
    fun restoreExpense(id: String) {
        if (db.restoreExpense(id)) {
            reload()
            app.sync.requestSync()
        }
    }

    fun restoreExpenseMany(ids: List<String>) {
        var any = false
        ids.forEach { if (db.restoreExpense(it)) any = true }
        if (any) {
            reload()
            app.sync.requestSync()
        }
    }

    fun restoreBorrow(id: String) {
        if (db.restoreBorrow(id)) {
            reload()
            app.sync.requestSync()
        }
    }

    private fun syncPending() = app.sync.pendingCount()

    // ---------- mutations ----------

    fun addExpense(title: String, amount: Double, category: String, date: String, notes: String, recurring: Boolean, recurDays: Int = 0) {
        val now = System.currentTimeMillis()
        db.insertExpense(
            Expense(UUID.randomUUID().toString(), title, amount, category, date, notes, recurring, now, now, dirty = true, recurDays = if (recurring) recurDays else 0)
        )
        reload()
        app.sync.requestSync()
    }

    fun updateExpense(e: Expense) {
        db.upsertExpense(e.copy(dirty = true, updatedAt = System.currentTimeMillis()))
        reload()
        app.sync.requestSync()
    }

    // ---------- pending-delete flow (makes Undo reliable) ----------
    // Swipe only removes the row locally; the cloud delete happens only after the
    // snackbar dismisses without Undo. If the app dies in between, the next pull
    // resurrects the row (safe default).

    fun stashExpenseForDelete(id: String): Expense? {
        val e = db.getExpense(id) ?: return null
        db.purgeExpense(id)
        reload()
        return e
    }

    fun undoDeleteExpense(stash: Expense) {
        db.upsertExpense(stash.copy(dirty = true))
        reload()
    }

    fun finalizeExpenseDelete(stash: Expense) {
        // keep a copy in the 30-day trash before the tombstone is pushed and purged
        val payload = org.json.JSONObject().apply {
            put("id", stash.id); put("title", stash.title); put("amount", stash.amount)
            put("category", stash.category); put("date", stash.date); put("notes", stash.notes)
            put("is_recurring", stash.isRecurring); put("recur_days", stash.recurDays)
            put("recur_parent", stash.recurParent ?: ""); put("created_at", stash.createdAt)
            put("updated_at", stash.updatedAt)
        }
        db.trashInsert(stash.id, "expense", payload.toString(), System.currentTimeMillis())
        db.upsertExpense(stash.copy(deleted = true, dirty = true, updatedAt = System.currentTimeMillis()))
        reload()
        app.sync.requestSync()
    }

    fun stashExpensesForDelete(ids: List<String>): List<Expense> = ids.mapNotNull { stashExpenseForDelete(it) }

    fun undoBulkDelete(stash: List<Expense>) {
        stash.forEach { db.upsertExpense(it.copy(dirty = true)) }
        reload()
    }

    fun finalizeBulkDelete(stash: List<Expense>) {
        val now = System.currentTimeMillis()
        stash.forEach { e ->
            val payload = org.json.JSONObject().apply {
                put("id", e.id); put("title", e.title); put("amount", e.amount)
                put("category", e.category); put("date", e.date); put("notes", e.notes)
                put("is_recurring", e.isRecurring); put("recur_days", e.recurDays)
                put("recur_parent", e.recurParent ?: ""); put("created_at", e.createdAt)
                put("updated_at", e.updatedAt)
            }
            db.trashInsert(e.id, "expense", payload.toString(), now)
            db.upsertExpense(e.copy(deleted = true, dirty = true, updatedAt = now))
        }
        reload()
        app.sync.requestSync()
    }

    fun stashBorrowForDelete(id: String): BorrowLend? {
        val b = db.getBorrow(id) ?: return null
        db.purgeBorrow(id)
        reload()
        return b
    }

    fun undoDeleteBorrow(stash: BorrowLend) {
        db.upsertBorrow(stash.copy(dirty = true))
        reload()
    }

    fun finalizeBorrowDelete(stash: BorrowLend) {
        val payload = org.json.JSONObject().apply {
            put("id", stash.id); put("type", stash.type); put("person_name", stash.personName)
            put("amount", stash.amount); put("date", stash.date)
            put("reminder_date", stash.reminderDate ?: ""); put("status", stash.status)
            put("notes", stash.notes); put("updated_at", stash.updatedAt)
        }
        db.trashInsert(stash.id, "borrow", payload.toString(), System.currentTimeMillis())
        db.upsertBorrow(stash.copy(deleted = true, dirty = true, updatedAt = System.currentTimeMillis()))
        reload()
        app.sync.requestSync()
    }

    // ---------- trash ----------

    fun trashItems(): List<TrashItem> = db.trashList()

    fun purgeOldTrash() {
        db.trashPurgeOlderThan(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
    }

    /** Re-create a deleted item from the trash and sync it back to the cloud. */
    fun restoreFromTrash(item: TrashItem): Boolean {
        return try {
            val o = org.json.JSONObject(item.payload)
            val now = System.currentTimeMillis()
            when (item.type) {
                "expense" -> db.upsertExpense(
                    Expense(
                        id = o.getString("id"), title = o.getString("title"),
                        amount = o.getDouble("amount"), category = o.optString("category", "other"),
                        date = o.optString("date", AppState.todayStr()), notes = o.optString("notes", ""),
                        isRecurring = o.optBoolean("is_recurring", false),
                        createdAt = o.optLong("created_at", now), updatedAt = now,
                        dirty = true, deleted = false,
                        recurDays = o.optInt("recur_days", 0),
                        recurParent = o.optString("recur_parent", "").ifBlank { null }
                    )
                )
                "borrow" -> db.upsertBorrow(
                    BorrowLend(
                        id = o.getString("id"), type = o.optString("type", "borrow"),
                        personName = o.optString("person_name", "?"), amount = o.getDouble("amount"),
                        date = o.optString("date", AppState.todayStr()),
                        reminderDate = o.optString("reminder_date", "").ifBlank { null },
                        status = o.optString("status", "pending"), notes = o.optString("notes", ""),
                        updatedAt = now, dirty = true, deleted = false
                    )
                )
            }
            db.trashDelete(item.id)
            reload()
            app.sync.requestSync()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteFromTrashForever(item: TrashItem) {
        db.trashDelete(item.id)
        reload()
    }

    /** Restore every item currently in the trash. Returns how many were restored. */
    fun restoreAllFromTrash(): Int {
        var count = 0
        db.trashList().forEach { item ->
            val o = runCatching { org.json.JSONObject(item.payload) }.getOrNull() ?: return@forEach
            val now = System.currentTimeMillis()
            when (item.type) {
                "expense" -> db.upsertExpense(
                    Expense(
                        id = o.getString("id"), title = o.getString("title"),
                        amount = o.getDouble("amount"), category = o.optString("category", "other"),
                        date = o.optString("date", AppState.todayStr()), notes = o.optString("notes", ""),
                        isRecurring = o.optBoolean("is_recurring", false),
                        createdAt = o.optLong("created_at", now), updatedAt = now,
                        dirty = true, deleted = false,
                        recurDays = o.optInt("recur_days", 0),
                        recurParent = o.optString("recur_parent", "").ifBlank { null }
                    )
                )
                "borrow" -> db.upsertBorrow(
                    BorrowLend(
                        id = o.getString("id"), type = o.optString("type", "borrow"),
                        personName = o.optString("person_name", "?"), amount = o.getDouble("amount"),
                        date = o.optString("date", AppState.todayStr()),
                        reminderDate = o.optString("reminder_date", "").ifBlank { null },
                        status = o.optString("status", "pending"), notes = o.optString("notes", ""),
                        updatedAt = now, dirty = true, deleted = false
                    )
                )
            }
            db.trashDelete(item.id)
            count++
        }
        if (count > 0) {
            reload()
            app.sync.requestSync()
        }
        return count
    }

    /** Reopen a settled borrow/lend record. */
    fun reopenBorrow(id: String) {
        val b = db.getBorrow(id) ?: return
        db.upsertBorrow(b.copy(status = "pending", dirty = true, updatedAt = System.currentTimeMillis()))
        reload()
        app.sync.requestSync()
    }

    /** Partial settlement: reduce remaining amount; full settlement flips status. */
    fun settleBorrow(id: String, settledAmount: Double) {
        val b = db.getBorrow(id) ?: return
        if (settledAmount <= 0) return
        val remaining = (b.amount - settledAmount).coerceAtLeast(0.0)
        val note = "Settled ${prefs.currencySymbol}${fmtInt(settledAmount)} on ${todayStr()}"
        val newNotes = if (b.notes.isBlank()) note else b.notes + " · " + note
        val now = System.currentTimeMillis()
        db.upsertBorrow(
            b.copy(
                amount = remaining,
                status = if (remaining <= 0.005) "settled" else "pending",
                notes = newNotes, dirty = true, updatedAt = now
            )
        )
        reload()
        app.sync.requestSync()
    }

    fun deleteExpense(id: String) {
        db.deleteExpenseLocal(id)
        reload()
        app.sync.requestSync()
    }

    fun bulkDelete(ids: List<String>) {
        ids.forEach { db.deleteExpenseLocal(it) }
        reload()
        app.sync.requestSync()
    }

    fun bulkRecategorize(ids: List<String>, category: String) {
        val now = System.currentTimeMillis()
        ids.forEach { id ->
            db.getExpense(id)?.let { db.upsertExpense(it.copy(category = category, dirty = true, updatedAt = now)) }
        }
        reload()
        app.sync.requestSync()
    }

    fun setBudget(category: String, amount: Double) {
        db.saveBudget(category, amount, "")
        reload()
        app.sync.requestSync()
    }

    fun setDailyLimit(amount: Double) {
        db.saveBudget("__daily_limit__", amount, "")
        reload()
        app.sync.requestSync()
    }

    fun setMonthlyTarget(dailyAvg: Double) {
        db.saveBudget("__monthly_target__", dailyAvg, todayMonth())
        reload()
        app.sync.requestSync()
    }

    fun addBorrow(type: String, person: String, amount: Double, date: String, reminder: String?, notes: String) {
        val now = System.currentTimeMillis()
        val b = BorrowLend(UUID.randomUUID().toString(), type, person, amount, date, reminder, "pending", notes, now, dirty = true)
        db.upsertBorrow(b)
        reload()
        com.expensetracker.tk.notify.Notify.scheduleBorrowReminders(app, listOf(b))
        app.sync.requestSync()
    }

    fun setBorrowStatus(id: String, status: String) {
        db.allBorrows(includeDeleted = true).firstOrNull { it.id == id }?.let {
            db.upsertBorrow(it.copy(status = status, dirty = true, updatedAt = System.currentTimeMillis()))
        }
        reload()
        app.sync.requestSync()
    }

    fun deleteBorrow(id: String) {
        db.deleteBorrowLocal(id)
        reload()
        app.sync.requestSync()
    }

    fun toggleBills() {
        prefs.includeBills = !prefs.includeBills
        _ui.value = _ui.value.copy(includeBills = prefs.includeBills)
    }

    fun setCardOrder(order: List<String>) {
        prefs.cardOrder = order
        _ui.value = _ui.value.copy(cardOrder = order)
    }

    fun savePresets(list: List<QuickPreset>) {
        prefs.presets = list
        _ui.value = _ui.value.copy(presets = list)
    }

    fun manualSync() {
        app.sync.requestSync(manual = true)
    }

    fun importJson(root: JSONObject): Int {
        val n = db.importJson(root, generateIds = true)
        reload()
        app.sync.requestSync()
        return n
    }

    fun exportJson(): JSONObject = db.exportJson(prefs.currencySymbol)

    /** CSV for Excel/Google Sheets: date, title, category, amount, notes, recurring. */
    fun exportCsv(): String = exportCsvFrom(calcExpenses())

    /** CSV for an arbitrary (e.g. currently filtered) list of expenses. */
    fun exportCsvFrom(list: List<Expense>): String {
        val sb = StringBuilder()
        sb.append("Date,Title,Category,Amount,Notes,Recurring\n")
        list.sortedWith(compareBy({ it.date }, { it.createdAt })).forEach { e ->
            val cat = Categories.byId(e.category).name
            sb.append(escapeCsv(e.date)).append(',')
                .append(escapeCsv(e.title)).append(',')
                .append(escapeCsv(cat)).append(',')
                .append(String.format(Locale.ENGLISH, "%.2f", e.amount)).append(',')
                .append(escapeCsv(e.notes)).append(',')
                .append(if (e.isRecurring) "yes" else "no")
                .append('\n')
        }
        return sb.toString()
    }

    private fun escapeCsv(v: String): String =
        if (v.contains(',') || v.contains('"') || v.contains('\n')) "\"" + v.replace("\"", "\"\"") + "\"" else v

    // ---------- derived (pure helpers for screens) ----------

    /** bills excluded unless included — same as web app */
    fun calcExpenses(): List<Expense> =
        if (_ui.value.includeBills) _ui.value.expenses else _ui.value.expenses.filter { it.category != "bills" }

    companion object {
        val DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val MONTHS = arrayOf("January","February","March","April","May","June","July","August","September","October","November","December")

        fun todayStr(): String = LocalDate.now().format(DTF)
        fun todayMonth(): String = todayStr().substring(0, 7)
        fun daysInMonth(): Int = YearMonth.now().lengthOfMonth()

        fun fmtAmount(v: Double): String {
            val whole = kotlin.math.abs(v).toLong()
            val frac = ((kotlin.math.abs(v) - whole) * 100).toInt()
            return String.format(Locale.ENGLISH, "%,d.%02d", whole, frac)
        }

        /** Full amount, no decimals: 1428 → "1,428" */
        fun fmtInt(v: Double): String = String.format(Locale.ENGLISH, "%,.0f", v)

        fun fmtShort(v: Double): String {
            return when {
                kotlin.math.abs(v) >= 1_000_000 -> String.format(Locale.ENGLISH, "%.1fM", v / 1_000_000)
                kotlin.math.abs(v) >= 1_000 -> String.format(Locale.ENGLISH, "%.1fk", v / 1_000)
                else -> String.format(Locale.ENGLISH, "%.0f", v)
            }
        }

        fun parseDate(s: String): LocalDate = try { LocalDate.parse(s.take(10)) } catch (e: Exception) { LocalDate.now() }
    }
}
