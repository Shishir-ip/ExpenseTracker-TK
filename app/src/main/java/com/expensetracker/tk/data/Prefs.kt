package com.expensetracker.tk.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/** Local settings. API keys are stored here once entered — the user never has to set them up again. */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)

    // ---- cloud credentials ----
    var supabaseUrl: String
        get() = sp.getString("sb_url", "")!!.trim().removeSuffix("/")
        set(v) = sp.edit().putString("sb_url", v.trim()).apply()
    var supabaseKey: String
        get() = sp.getString("sb_key", "")!!.trim()
        set(v) = sp.edit().putString("sb_key", v.trim()).apply()
    var openRouterKey: String
        get() = sp.getString("or_key", "")!!.trim()
        set(v) = sp.edit().putString("or_key", v.trim()).apply()
    var openRouterModel: String
        get() = sp.getString("or_model", "openrouter/auto")!!.trim()
        set(v) = sp.edit().putString("or_model", v.trim()).apply()

    /** profile key = value stored in the user_pin column; change it to your web app PIN to load that data */
    var userKey: String
        get() = sp.getString("user_key", "main")!!.trim()
        set(v) = sp.edit().putString("user_key", v.trim()).apply()

    val cloudConfigured: Boolean get() = supabaseUrl.startsWith("http") && supabaseKey.length > 20

    // ---- appearance ----
    /** 0 = system, 1 = light, 2 = dark */
    var themeMode: Int
        get() = sp.getInt("theme_mode", 0)
        set(v) = sp.edit().putInt("theme_mode", v).apply()

    var currencySymbol: String
        get() = sp.getString("currency", "\u09F3")!!
        set(v) = sp.edit().putString("currency", v).apply()

    var includeBills: Boolean
        get() = sp.getBoolean("include_bills", false)
        set(v) = sp.edit().putBoolean("include_bills", v).apply()

    // ---- home card order ----
    var cardOrder: List<String>
        get() {
            val s = sp.getString("card_order", null) ?: return DEFAULT_ORDER
            val list = s.split(",").filter { it.isNotBlank() }
            // append any new cards introduced in updates
            val missing = DEFAULT_ORDER.filter { it !in list }
            return list + missing
        }
        set(v) = sp.edit().putString("card_order", v.joinToString(",")).apply()

    // ---- quick presets ----
    var presets: List<QuickPreset>
        get() {
            val s = sp.getString("presets", null) ?: return defaultPresets()
            return try {
                val arr = JSONArray(s)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    QuickPreset(o.getString("id"), o.getString("label"), o.getString("title"), o.getDouble("amount"), o.getString("category"))
                }
            } catch (e: Exception) { defaultPresets() }
        }
        set(v) = sp.edit().putString("presets", JSONArray().apply { v.forEach { p -> put(JSONObject().apply {
            put("id", p.id); put("label", p.label); put("title", p.title); put("amount", p.amount); put("category", p.category)
        }) } }.toString()).apply()

    private fun defaultPresets() = listOf(
        QuickPreset("p1", "Khichuri", "Khichuri", 100.0, "food"),
        QuickPreset("p2", "Porota 3, Dal Vaji, Dim", "Porota, Dal Vaji, Dim", 60.0, "food"),
        QuickPreset("p3", "Bus Ticket", "Bus Ticket", 550.0, "transport")
    )

    // ---- app lock (optional, default OFF) ----
    var lockEnabled: Boolean
        get() = sp.getBoolean("lock_enabled", false)
        set(v) = sp.edit().putBoolean("lock_enabled", v).apply()
    var lockBiometric: Boolean
        get() = sp.getBoolean("lock_biometric", false)
        set(v) = sp.edit().putBoolean("lock_biometric", v).apply()
    var lockPinHash: String
        get() = sp.getString("lock_pin", "")!!
        set(v) = sp.edit().putString("lock_pin", v).apply()

    fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(("tk$salt$pin").toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private val salt: String
        get() = sp.getString("lock_salt", null) ?: UUID.randomUUID().toString().also {
            sp.edit().putString("lock_salt", it).apply()
        }

    // ---- notifications ----
    var dailyReminder: Boolean
        get() = sp.getBoolean("daily_reminder", false)
        set(v) = sp.edit().putBoolean("daily_reminder", v).apply()
    var dailyReminderHour: Int
        get() = sp.getInt("daily_reminder_hour", 21)
        set(v) = sp.edit().putInt("daily_reminder_hour", v).apply()

    // ---- auto local backup ----
    var autoBackup: Boolean
        get() = sp.getBoolean("auto_backup", false)
        set(v) = sp.edit().putBoolean("auto_backup", v).apply()
    var backupDir: String
        get() = sp.getString("backup_dir", "")!!
        set(v) = sp.edit().putString("backup_dir", v).apply()
    var lastBackup: Long
        get() = sp.getLong("last_backup", 0)
        set(v) = sp.edit().putLong("last_backup", v).apply()

    // ---- accent colour (index into Theme.ACCENTS, 0 = default indigo) ----
    var accentIndex: Int
        get() = sp.getInt("accent_index", 0)
        set(v) = sp.edit().putInt("accent_index", v).apply()

    // ---- collapsible quick add ----
    var quickCollapsed: Boolean
        get() = sp.getBoolean("quick_collapsed", false)
        set(v) = sp.edit().putBoolean("quick_collapsed", v).apply()

    // ---- saved search filters (JSON) ----
    var savedFilters: String
        get() = sp.getString("saved_filters", "[]")!!
        set(v) = sp.edit().putString("saved_filters", v).apply()

    fun getSavedFilterList(): List<SavedFilter> = try {
        val arr = org.json.JSONArray(savedFilters)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val cats = mutableListOf<String>()
            val ca = o.optJSONArray("cats")
            if (ca != null) for (j in 0 until ca.length()) cats.add(ca.getString(j))
            SavedFilter(
                name = o.getString("name"),
                cats = cats,
                min = if (o.isNull("min")) null else o.optDouble("min"),
                max = if (o.isNull("max")) null else o.optDouble("max")
            )
        }
    } catch (e: Exception) { emptyList() }

    fun setSavedFilterList(list: List<SavedFilter>) {
        val arr = org.json.JSONArray()
        list.forEach { f ->
            arr.put(org.json.JSONObject().apply {
                put("name", f.name)
                put("cats", org.json.JSONArray(f.cats))
                put("min", f.min ?: org.json.JSONObject.NULL)
                put("max", f.max ?: org.json.JSONObject.NULL)
            })
        }
        savedFilters = arr.toString()
    }

    // ---- misc ----
    var lastSync: Long
        get() = sp.getLong("last_sync", 0)
        set(v) = sp.edit().putLong("last_sync", v).apply()

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()

    companion object {
        val DEFAULT_ORDER = listOf(
            "status", "stats", "quick", "chart", "transactions", "budgets", "summary", "insights"
        )
    }
}


/** A named, reusable History filter. */
data class SavedFilter(
    val name: String,
    val cats: List<String>,
    val min: Double?,
    val max: Double?
)
