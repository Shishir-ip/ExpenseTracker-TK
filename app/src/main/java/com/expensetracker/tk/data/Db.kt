package com.expensetracker.tk.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local-first SQLite store. Mirrors the Supabase schema so the same rows can be
 * pushed/pulled with the cloud. `dirty` marks rows that still need a push;
 * `deleted` is a soft delete that is pushed then purged.
 */
class Db(context: Context) : SQLiteOpenHelper(context, "tracker.db", null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE expenses(
            id TEXT PRIMARY KEY, title TEXT NOT NULL, amount REAL NOT NULL,
            category TEXT NOT NULL, date TEXT NOT NULL, notes TEXT,
            is_recurring INTEGER DEFAULT 0, created_at INTEGER, updated_at INTEGER,
            dirty INTEGER DEFAULT 1, deleted INTEGER DEFAULT 0,
            recur_days INTEGER DEFAULT 0, recur_parent TEXT DEFAULT '')"""
        )
        db.execSQL("CREATE INDEX idx_exp_date ON expenses(date)")
        db.execSQL("CREATE INDEX idx_exp_dirty ON expenses(dirty)")

        db.execSQL(
            """CREATE TABLE borrow_lend(
            id TEXT PRIMARY KEY, type TEXT NOT NULL, person_name TEXT NOT NULL,
            amount REAL NOT NULL, date TEXT NOT NULL, reminder_date TEXT,
            status TEXT DEFAULT 'pending', notes TEXT, updated_at INTEGER,
            dirty INTEGER DEFAULT 1, deleted INTEGER DEFAULT 0)"""
        )

        db.execSQL(
            """CREATE TABLE budgets(
            category TEXT PRIMARY KEY, amount REAL NOT NULL, year_month TEXT DEFAULT '')"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        if (oldV < 2) {
            db.execSQL("ALTER TABLE expenses ADD COLUMN recur_days INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE expenses ADD COLUMN recur_parent TEXT DEFAULT ''")
        }
    }

    companion object { const val DB_VERSION = 2 }

    // ---------- expenses ----------

    fun insertExpense(e: Expense) {
        writableDatabase.insertWithOnConflict("expenses", null, e.toCv(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun Expense.toCv() = ContentValues().apply {
        put("id", id); put("title", title); put("amount", amount); put("category", category)
        put("date", date); put("notes", notes); put("is_recurring", if (isRecurring) 1 else 0)
        put("created_at", createdAt); put("updated_at", updatedAt)
        put("dirty", if (dirty) 1 else 0); put("deleted", if (deleted) 1 else 0)
        put("recur_days", recurDays); put("recur_parent", recurParent ?: "")
    }

    private fun cursorToExpense(c: android.database.Cursor) = Expense(
        id = c.getString(0), title = c.getString(1), amount = c.getDouble(2),
        category = c.getString(3), date = c.getString(4),
        notes = c.getString(5) ?: "", isRecurring = c.getInt(6) == 1,
        createdAt = c.getLong(7), updatedAt = c.getLong(8),
        dirty = c.getInt(9) == 1, deleted = c.getInt(10) == 1,
        recurDays = c.getInt(11), recurParent = c.getString(12)?.ifBlank { null }
    )

    private val EXP_COLS = "id,title,amount,category,date,notes,is_recurring,created_at,updated_at,dirty,deleted,recur_days,recur_parent"

    fun allExpenses(includeDeleted: Boolean = false): List<Expense> {
        val db = readableDatabase
        val q = if (includeDeleted) "SELECT $EXP_COLS FROM expenses ORDER BY date DESC, created_at DESC"
        else "SELECT $EXP_COLS FROM expenses WHERE deleted=0 ORDER BY date DESC, created_at DESC"
        return db.rawQuery(q, null).use { c ->
            ArrayList<Expense>(c.count).apply { while (c.moveToNext()) add(cursorToExpense(c)) }
        }
    }

    fun dirtyExpenses(): List<Expense> {
        val db = readableDatabase
        return db.rawQuery("SELECT $EXP_COLS FROM expenses WHERE dirty=1", null).use { c ->
            ArrayList<Expense>().apply { while (c.moveToNext()) add(cursorToExpense(c)) }
        }
    }

    fun getExpense(id: String): Expense? {
        val db = readableDatabase
        return db.rawQuery("SELECT $EXP_COLS FROM expenses WHERE id=?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) cursorToExpense(c) else null
        }
    }

    fun upsertExpense(e: Expense) {
        val db = writableDatabase
        db.insertWithOnConflict("expenses", null, e.toCv(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun markExpenseSynced(id: String) {
        val db = writableDatabase
        db.execSQL("UPDATE expenses SET dirty=0 WHERE id=?", arrayOf(id))
    }

    /** soft delete: kept locally until the delete is pushed */
    fun deleteExpenseLocal(id: String) {
        val db = writableDatabase
        val cv = ContentValues().apply { put("dirty", 1); put("deleted", 1) }
        db.update("expenses", cv, "id=?", arrayOf(id))
    }

    /** hard delete of already-synced rows (undo / purge) */
    fun purgeExpense(id: String) {
        writableDatabase.delete("expenses", "id=?", arrayOf(id))
    }

    fun purgeDeletedExpenses() {
        writableDatabase.execSQL("DELETE FROM expenses WHERE deleted=1 AND dirty=0")
    }

    /** Undo a soft delete. Returns false if the row was already purged. */
    fun restoreExpense(id: String): Boolean {
        val cv = ContentValues().apply { put("deleted", 0); put("dirty", 1) }
        return writableDatabase.update("expenses", cv, "id=? AND deleted=1", arrayOf(id)) > 0
    }

    // ---------- budgets ----------

    fun allBudgets(): List<BudgetRow> {
        val db = readableDatabase
        return db.rawQuery("SELECT category, amount, year_month FROM budgets", null).use { c ->
            ArrayList<BudgetRow>().apply {
                while (c.moveToNext()) add(BudgetRow(c.getString(0), c.getString(0), c.getDouble(1), c.getString(2) ?: ""))
            }
        }
    }

    fun saveBudget(category: String, amount: Double, yearMonth: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("category", category); put("amount", amount); put("year_month", yearMonth)
        }
        db.insertWithOnConflict("budgets", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeBudget(category: String) {
        writableDatabase.delete("budgets", "category=?", arrayOf(category))
    }

    fun replaceBudgets(rows: List<BudgetRow>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("budgets", null, null)
            rows.forEach { r ->
                val cv = ContentValues().apply {
                    put("category", r.category); put("amount", r.amount); put("year_month", r.yearMonth)
                }
                db.insertWithOnConflict("budgets", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    // ---------- borrow / lend ----------

    private fun BorrowLend.toCv() = ContentValues().apply {
        put("id", id); put("type", type); put("person_name", personName); put("amount", amount)
        put("date", date); put("reminder_date", reminderDate); put("status", status)
        put("notes", notes); put("updated_at", updatedAt)
        put("dirty", if (dirty) 1 else 0); put("deleted", if (deleted) 1 else 0)
    }

    private val BL_COLS = "id,type,person_name,amount,date,reminder_date,status,notes,updated_at,dirty,deleted"

    private fun cursorToBl(c: android.database.Cursor) = BorrowLend(
        id = c.getString(0), type = c.getString(1), personName = c.getString(2),
        amount = c.getDouble(3), date = c.getString(4),
        reminderDate = if (c.isNull(5)) null else c.getString(5),
        status = c.getString(6), notes = c.getString(7) ?: "", updatedAt = c.getLong(8),
        dirty = c.getInt(9) == 1, deleted = c.getInt(10) == 1
    )

    fun upsertBorrow(b: BorrowLend) {
        writableDatabase.insertWithOnConflict("borrow_lend", null, b.toCv(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getBorrow(id: String): BorrowLend? {
        return readableDatabase.rawQuery("SELECT $BL_COLS FROM borrow_lend WHERE id=?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) cursorToBl(c) else null
        }
    }

    fun allBorrows(includeDeleted: Boolean = false): List<BorrowLend> {
        val db = readableDatabase
        val q = if (includeDeleted) "SELECT $BL_COLS FROM borrow_lend ORDER BY date DESC"
        else "SELECT $BL_COLS FROM borrow_lend WHERE deleted=0 ORDER BY date DESC"
        return db.rawQuery(q, null).use { c ->
            ArrayList<BorrowLend>().apply { while (c.moveToNext()) add(cursorToBl(c)) }
        }
    }

    fun dirtyBorrows(): List<BorrowLend> {
        val db = readableDatabase
        return db.rawQuery("SELECT $BL_COLS FROM borrow_lend WHERE dirty=1", null).use { c ->
            ArrayList<BorrowLend>().apply { while (c.moveToNext()) add(cursorToBl(c)) }
        }
    }

    fun markBorrowSynced(id: String) {
        writableDatabase.execSQL("UPDATE borrow_lend SET dirty=0 WHERE id=?", arrayOf(id))
    }

    fun deleteBorrowLocal(id: String) {
        val cv = ContentValues().apply { put("dirty", 1); put("deleted", 1) }
        writableDatabase.update("borrow_lend", cv, "id=?", arrayOf(id))
    }

    fun purgeBorrow(id: String) {
        writableDatabase.delete("borrow_lend", "id=?", arrayOf(id))
    }

    fun purgeDeletedBorrows() {
        writableDatabase.execSQL("DELETE FROM borrow_lend WHERE deleted=1 AND dirty=0")
    }

    /** Undo a soft delete. Returns false if the row was already purged. */
    fun restoreBorrow(id: String): Boolean {
        val cv = ContentValues().apply { put("deleted", 0); put("dirty", 1) }
        return writableDatabase.update("borrow_lend", cv, "id=? AND deleted=1", arrayOf(id)) > 0
    }

    // ---------- export / import ----------

    fun exportJson(currencyNote: String): JSONObject {
        val arr = JSONArray()
        allExpenses().forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("title", e.title); put("amount", e.amount)
                put("category", e.category); put("date", e.date); put("notes", e.notes)
                put("is_recurring", e.isRecurring)
                put("recur_days", e.recurDays)
            })
        }
        val buds = JSONArray()
        allBudgets().forEach { b ->
            buds.put(JSONObject().apply {
                put("category", b.category); put("amount", b.amount); put("year_month", b.yearMonth)
            })
        }
        val bls = JSONArray()
        allBorrows().forEach { b ->
            bls.put(b.toJson("local"))
        }
        return JSONObject().apply {
            put("app", "Expense Tracker TK")
            put("version", 1)
            put("exported_at", System.currentTimeMillis())
            put("expenses", arr)
            put("budgets", buds)
            put("borrow_lend", bls)
        }
    }

    fun importJson(root: JSONObject, generateIds: Boolean): Int {
        var count = 0
        val now = System.currentTimeMillis()
        val exps = root.optJSONArray("expenses") ?: JSONArray()
        for (i in 0 until exps.length()) {
            val o = exps.getJSONObject(i)
            val id = if (generateIds || o.optString("id").isBlank()) UUID.randomUUID().toString() else o.optString("id")
            upsertExpense(
                Expense(
                    id = id,
                    title = o.optString("title", "Untitled"),
                    amount = o.optDouble("amount", 0.0),
                    category = o.optString("category", "other"),
                    date = o.optString("date", ""),
                    notes = o.optString("notes", ""),
                    isRecurring = o.optBoolean("is_recurring", false),
                    createdAt = now, updatedAt = now, dirty = true,
                    recurDays = o.optInt("recur_days", 0)
                )
            )
            count++
        }
        val buds = root.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until buds.length()) {
            val o = buds.getJSONObject(i)
            saveBudget(o.optString("category"), o.optDouble("amount", 0.0), o.optString("year_month", ""))
        }
        val bls = root.optJSONArray("borrow_lend") ?: JSONArray()
        for (i in 0 until bls.length()) {
            val o = bls.getJSONObject(i)
            val id = if (generateIds || o.optString("id").isBlank()) UUID.randomUUID().toString() else o.optString("id")
            upsertBorrow(
                BorrowLend(
                    id = id, type = o.optString("type", "borrow"),
                    personName = o.optString("person_name", "?"),
                    amount = o.optDouble("amount", 0.0),
                    date = o.optString("date", ""),
                    reminderDate = if (o.isNull("reminder_date")) null else o.optString("reminder_date"),
                    status = o.optString("status", "pending"),
                    notes = o.optString("notes", ""), updatedAt = now, dirty = true
                )
            )
        }
        return count
    }
}
