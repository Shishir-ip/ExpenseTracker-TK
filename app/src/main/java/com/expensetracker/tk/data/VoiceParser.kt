package com.expensetracker.tk.data

import org.json.JSONArray
import java.time.LocalDate

/**
 * Turns a spoken sentence ("lunch 120 and bus 50 yesterday") into expense drafts.
 * Two engines: local keyword parsing (offline, deterministic) and an LLM parse via
 * the user's OpenRouter/Gemini key (handles messy natural language when online).
 */
object VoiceParser {

    data class Draft(
        var title: String,
        var amount: Double,
        var category: String,
        var date: String
    )

    private val KEYWORDS = mapOf(
        "food" to listOf("lunch", "dinner", "breakfast", "khichuri", "rice", "bhat", "biryani", "coffee", "cha", "tea",
            "snack", "singara", "samosa", "porota", "dal", "vaji", "dim", "egg", "chicken", "beef", "burger", "pizza",
            "canteen", "food", "fuchka", "juice", "cold drinks", "water", "milk", "banana", "apple", "meal", "iftar"),
        "transport" to listOf("bus", "rickshaw", "ricksha", "cng", "uber", "pathao", "obhai", "fuel", "petrol", "octane",
            "gas station", "train", "ticket", "launch", "fare", "van", "auto"),
        "shopping" to listOf("shirt", "pant", "tshirt", "clothes", "bag", "shoe", "sandal", "cosmetic", "shopping",
            "mall", "grocery", "bazar", "sabji", "vegetables", "fish market"),
        "entertainment" to listOf("movie", "cinema", "game", "netflix", "cricket", "match", "tour", "hangout", "trip",
            "concert", "steam"),
        "bills" to listOf("bill", "gas bill", "electricity", "wifi", "internet", "recharge", "mobile bill", "rent",
            "eload", "top up", "topup", "bkash charge", "send money charge"),
        "health" to listOf("medicine", "doctor", "hospital", "pharmacy", "clinic", "test", "saline", "vitamin", "syrup"),
        "education" to listOf("book", "exam", "fee", "tuition", "print", "xerox", "photocopy", "pen", "note", "course",
            "admission", "form fill")
    )

    private val FILLERS = listOf("paid", "spend", "spent", "gave", "give", "add", "expense", "taka", "tk", "poisa",
        "for", "on", "of", "the", "a", "an", "was", "cost", "costs", "bought", "did", "do", "please", "track", "log")

    /** Deterministic offline parser. */
    fun parseLocal(text: String, today: LocalDate = LocalDate.now()): List<Draft> {
        val normalized = text.lowercase()
            .replace("৳", " ")
            .replace(Regex("[\\u09E6-\\u09EF]")) { ch -> (ch.value[0] - '০').toString() } // Bengali digits
        val date = if (normalized.contains("yesterday") || normalized.contains(" kal ") || normalized.endsWith(" kal"))
            today.minusDays(1).toString() else today.toString()

        val segments = normalized.split(Regex("\\s+and\\s+|,|\\s+then\\s+|\\s+plus\\s+|\\s+&\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val drafts = ArrayList<Draft>()
        for (seg in segments) {
            val amountMatch = Regex("(\\d+(?:\\.\\d+)?)").find(seg.replace(",", ""))
            val amount = amountMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: continue
            if (amount <= 0) continue

            var words = seg
                .replace(amountMatch.value, " ")
                .split(Regex("[^a-z]+"))
                .filter { it.isNotBlank() && it !in FILLERS }
            words = words.filterNot { it == "yesterday" || it == "kal" }

            val category = KEYWORDS.entries.firstOrNull { (_, keys) -> keys.any { k -> seg.contains(k) } }?.key ?: "other"
            val title = words.joinToString(" ").ifBlank {
                Categories.byId(category).name
            }

            drafts.add(Draft(title.replaceFirstChar { it.uppercase() }, amount, category, date))
        }
        return drafts
    }

    fun aiSystemPrompt(today: String): String = """
        You extract expenses from a voice note for a personal expense tracker.
        Today's date is $today. Currency is BDT (taka). Users may speak in English or Banglish.
        Allowed categories (use exactly one per expense): food, transport, shopping, entertainment, bills, health, education, other.
        Rules:
        - Multiple expenses may be in one sentence ("lunch 120 and bus 50") — return one object each.
        - Default missing dates to today's date, "yesterday" to the day before.
        - If the year is omitted use the current year.
        - Titles: short, capitalized, no amounts in them.
        Respond with ONLY a JSON array, no markdown, no commentary:
        [{"title":"Lunch","amount":120,"category":"food","date":"$today"}]
        If there are no expenses, respond with []
    """.trimIndent()

    /** LLM parse via the user's configured OpenRouter/Gemini model. Throws on failure. */
    fun parseAi(text: String, prefs: Prefs, today: LocalDate = LocalDate.now()): List<Draft> {
        val ai = OpenRouter(prefs.openRouterKey, prefs.openRouterModel)
        val reply = ai.chat(
            listOf("system" to aiSystemPrompt(today.toString()), "user" to text),
            maxTokens = 500
        )
        return fromJson(reply, today)
    }

    fun fromJson(reply: String, today: LocalDate = LocalDate.now()): List<Draft> {
        var cleaned = reply.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        cleaned = cleaned.substring(start, end + 1)

        val arr = JSONArray(cleaned)
        val valid = Categories.all.map { it.id }.toSet()
        val drafts = ArrayList<Draft>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val amount = o.optDouble("amount", 0.0)
            if (amount <= 0) continue
            val cat = o.optString("category", "other").lowercase()
            val dateRaw = o.optString("date", "").take(10)
            val date = try {
                LocalDate.parse(dateRaw).toString()
            } catch (e: Exception) { today.toString() }
            drafts.add(
                Draft(
                    title = o.optString("title", "Voice expense").replaceFirstChar { it.uppercase() },
                    amount = amount,
                    category = if (cat in valid) cat else "other",
                    date = date
                )
            )
        }
        return drafts
    }
}
