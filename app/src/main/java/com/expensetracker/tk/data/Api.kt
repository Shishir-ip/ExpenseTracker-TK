package com.expensetracker.tk.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Minimal Supabase (PostgREST) client over plain REST. */
class SupabaseRest(private val baseUrl: String, private val anonKey: String) {

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = "application/json".toMediaType()

    @Volatile
    private var schemaCache: MutableMap<String, Set<String>>? = null

    @Volatile
    private var excludedCache: MutableMap<String, MutableSet<String>>? = null

    /** Columns learned to be missing from a table (from PGRST204 responses). */
    private fun excludedFor(table: String): MutableSet<String> {
        val cache = excludedCache ?: HashMap<String, MutableSet<String>>().also { excludedCache = it }
        synchronized(cache) { return cache.getOrPut(table) { HashSet() } }
    }

    private fun headers(extra: Map<String, String> = emptyMap()) = mutableMapOf(
        "apikey" to anonKey,
        "Authorization" to "Bearer $anonKey",
        "Content-Type" to "application/json"
    ).apply { putAll(extra) }

    class RestException(val code: Int, message: String) : Exception(message)

    private fun execute(request: Request): String {
        http.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RestException(resp.code, "HTTP ${resp.code}: ${body.take(300)}")
            return body
        }
    }

    /** SELECT rows. `query` example: expenses?select=*&user_pin=eq.main&order=date.desc */
    fun select(table: String, query: String): JSONArray {
        val req = Request.Builder()
            .url("$baseUrl/rest/v1/$table?$query")
            .get()
            .apply { headers().forEach { (k, v) -> header(k, v) } }
            .build()
        val body = execute(req)
        return if (body.isBlank()) JSONArray() else JSONArray(body)
    }

    /**
     * Real table columns from the PostgREST OpenAPI spec — web-app tables may lack
     * columns like updated_at, so pushes must be adapted to what actually exists.
     */
    fun tableColumns(table: String): Set<String>? {
        schemaCache?.get(table)?.let { return it }
        return try {
            val req = Request.Builder()
                .url("$baseUrl/rest/v1/")
                .get()
                .apply { headers().forEach { (k, v) -> header(k, v) } }
                .build()
            val root = JSONObject(execute(req))
            val defs = root.optJSONObject("definitions")
                ?: root.optJSONObject("components")?.optJSONObject("schemas")
                ?: return null
            val props = defs.optJSONObject(table)?.optJSONObject("properties") ?: return null
            val cols = HashSet<String>()
            props.keys().forEach { cols.add(it) }
            val cache = schemaCache ?: HashMap<String, Set<String>>().also { schemaCache = it }
            synchronized(cache) { cache[table] = cols }
            cols
        } catch (e: Exception) {
            null
        }
    }

    /** Drop payload keys that don't exist as table columns (keeps the row if nothing matched). */
    private fun adaptRow(table: String, row: JSONObject): JSONObject {
        val cols = tableColumns(table) ?: return row
        val out = JSONObject()
        row.keys().forEach { k -> if (cols.contains(k)) out.put(k, row.get(k)) }
        return if (out.length() == 0) row else out
    }

    /**
     * Bulk upsert via POST + merge-duplicates. Adapts the payload to the table:
     * 1. drops columns that don't exist (OpenAPI introspection),
     * 2. on "Could not find the 'x' column" (PGRST204) strips that column and retries,
     *    remembering it for future pushes,
     * 3. if the table rejects client-provided ids, retries once as a plain insert.
     */
    fun upsert(table: String, rows: JSONArray) {
        if (rows.length() == 0) return
        val prepared = JSONArray()
        for (i in 0 until rows.length()) {
            val o = adaptRow(table, rows.getJSONObject(i))
            excludedFor(table).forEach { c -> if (o.has(c)) o.remove(c) }
            if (o.length() > 0) prepared.put(o)
        }
        if (prepared.length() == 0) return

        var attempt = prepared
        var useIds = true
        var guard = 0
        while (guard++ < 12) {
            val payload = if (useIds) attempt else stripCol(attempt, "id")
            val prefer = if (useIds) "resolution=merge-duplicates" else "return=minimal"
            val req = Request.Builder()
                .url("$baseUrl/rest/v1/$table")
                .apply { headers(mapOf("Prefer" to prefer)).forEach { (k, v) -> header(k, v) } }
                .post(payload.toString().toRequestBody(json))
                .build()
            try {
                execute(req)
                return
            } catch (e: RestException) {
                val missing = Regex("Could not find the '([A-Za-z0-9_]+)' column")
                    .find(e.message ?: "")?.groupValues?.get(1)
                if (e.code == 400 && missing != null) {
                    excludedFor(table).add(missing)
                    attempt = stripCol(attempt, missing)
                    if (attempt.length() == 0) return
                    continue
                }
                if (useIds) {
                    useIds = false  // server-generated PK: retry as plain insert without ids
                    continue
                }
                throw e
            }
        }
    }

    private fun stripCol(rows: JSONArray, col: String): JSONArray {
        val out = JSONArray()
        for (i in 0 until rows.length()) {
            val o = JSONObject(rows.getJSONObject(i).toString())
            o.remove(col)
            if (o.length() > 0) out.put(o)
        }
        return out
    }

    fun delete(table: String, filter: String) {
        val req = Request.Builder()
            .url("$baseUrl/rest/v1/$table?$filter")
            .delete()
            .apply { headers().forEach { (k, v) -> header(k, v) } }
            .build()
        execute(req)
    }

    /** Lightweight connectivity + credentials test. */
    fun testConnection(): String {
        val req = Request.Builder()
            .url("$baseUrl/rest/v1/expenses?select=*&limit=1")
            .get()
            .apply { headers().forEach { (k, v) -> header(k, v) } }
            .build()
        execute(req)
        return "ok"
    }

    companion object {
        fun fromPrefs(p: Prefs): SupabaseRest? =
            if (p.cloudConfigured) SupabaseRest(p.supabaseUrl, p.supabaseKey) else null
    }
}

/** OpenRouter chat completions. */
class OpenRouter(private val apiKey: String, private val model: String) {

    fun chat(messages: List<Pair<String, String>>, maxTokens: Int = 1600): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val arr = JSONArray()
        messages.forEach { (role, content) -> arr.put(JSONObject().put("role", role).put("content", content)) }
        val payload = JSONObject()
            .put("model", model.ifBlank { "openrouter/auto" })
            .put("messages", arr)
            .put("max_tokens", maxTokens)
        val req = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://expense-tracker.app")
            .header("X-Title", "Expense Tracker TK")
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw SupabaseRest.RestException(resp.code, "HTTP ${resp.code}: ${body.take(300)}")
            val root = JSONObject(body)
            return root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    fun testKey(): String {
        return chat(listOf("user" to "Reply with exactly: OK"), 8)
    }
}
