package com.expensetracker.tk.data

import org.json.JSONObject
import java.time.LocalDate

/** Builds compact financial context for the AI + local (non-AI) insight computations. */
object AiContext {

    fun buildContext(expenses: List<Expense>, budgets: Map<String, Double>, monthlyTarget: Double, currency: String): String {
        if (expenses.isEmpty()) return "The user has no recorded expenses yet."

        val today = LocalDate.now().toString()
        val thisMonth = today.substring(0, 7)
        val lastMonth = LocalDate.now().minusMonths(1).toString().substring(0, 7)

        val monthExp = expenses.filter { it.date.startsWith(thisMonth) }
        val lastMonthExp = expenses.filter { it.date.startsWith(lastMonth) }

        val byCat = monthExp.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
        val topCat = byCat.entries.sortedByDescending { it.value }.take(5)
            .joinToString(", ") { "${Categories.byId(it.key).name} ${currency}${it.value.toInt()}" }

        val recent = expenses.take(15).joinToString("; ") {
            "${it.date}: ${it.title} ${currency}${it.amount.toInt()} (${Categories.byId(it.category).name})"
        }

        val todayTotal = expenses.filter { it.date == today }.sumOf { it.amount }
        val monthTotal = monthExp.sumOf { it.amount }
        val lastTotal = lastMonthExp.sumOf { it.amount }
        val dayOfMonth = LocalDate.now().dayOfMonth
        val dailyAvg = monthTotal / dayOfMonth.coerceAtLeast(1)
        val budg = budgets.entries.joinToString(", ") { "${Categories.byId(it.key).name} ${currency}${it.value.toInt()}" }

        return """
            |FINANCIAL SNAPSHOT:
            |- Currency: $currency (BDT context, Bangladesh)
            |- Today ($today): spent ${currency}${todayTotal.toInt()} in ${expenses.count { it.date == today }} transactions
            |- This month ($thisMonth): ${currency}${monthTotal.toInt()} across ${monthExp.size} transactions (daily avg ${currency}${dailyAvg.toInt()})
            |- Last month: ${currency}${lastTotal.toInt()}
            |- Category split this month: $topCat
            |- Category budgets: ${if (budg.isBlank()) "none set" else budg}
            |- Monthly daily-avg target: ${if (monthlyTarget > 0) "${currency}${monthlyTarget.toInt()}/day" else "not set"}
            |- Recent purchases: $recent
        """.trimMargin()
    }

    fun insightsSystemPrompt(context: String): String = """
        You are an expert financial assistant inside an expense tracker app (user from Bangladesh, currency BDT ৳).
        $context
        INSTRUCTIONS:
        - Give a personal spending analysis: 3-5 short sections with bold headings
        - Cover: overall trend vs last month, top categories with concrete numbers, budget/target status, and 2-4 actionable tips
        - Be encouraging but honest. Use markdown (**bold**, bullet lists). Keep it under 220 words.
        - No disclaimers, no questions back — just the analysis.
    """.trimIndent()

    fun chatSystemPrompt(context: String): String = """
        You are an expert financial assistant for an expense tracker app. The user is from Bangladesh (currency: ৳/BDT).
        $context
        INSTRUCTIONS:
        - Answer questions about spending, budgets, savings tips, and financial advice
        - If asked about product prices, check the recent purchases list
        - If asked "when did I last buy X", search the recent purchases
        - Give practical Bangladesh-specific advice when relevant
        - Keep responses concise but complete (2-5 sentences)
        - Be encouraging but honest about spending habits
        - Use markdown formatting for better readability
        - If the user asks something unrelated to finances, politely redirect to money topics
    """.trimIndent()
}

/** Local computed insights (work fully offline, no AI needed). */
object LocalInsights {

    data class Predictions(
        val weekPred: Double,
        val monthPred: Double,
        val dailyAvg: Double,
        val daysLeft: Int
    )

    fun predictions(expenses: List<Expense>): Predictions {
        val today = LocalDate.now()
        val thisMonth = today.toString().substring(0, 7)
        val monthExp = expenses.filter { it.date.startsWith(thisMonth) }
        val monthTotal = monthExp.sumOf { it.amount }
        val dailyAvg = if (today.dayOfMonth > 0) monthTotal / today.dayOfMonth else 0.0
        val daysLeft = today.lengthOfMonth() - today.dayOfMonth
        return Predictions(
            weekPred = dailyAvg * 7,
            monthPred = dailyAvg * today.lengthOfMonth(),
            dailyAvg = dailyAvg,
            daysLeft = daysLeft
        )
    }

    /** avg spend per weekday over the last 8 weeks — reveals habit patterns */
    fun weekdayPattern(expenses: List<Expense>): List<Double> {
        val cutoff = LocalDate.now().minusDays(56).toString()
        val recent = expenses.filter { it.date >= cutoff }
        return (0..6).map { dow ->
            val days = recent.filter { LocalDate.parse(it.date).dayOfWeek.value % 7 == dow }
            val distinctDays = days.map { it.date }.distinct().size.coerceAtLeast(1)
            days.sumOf { it.amount } / distinctDays
        }
    }

    /** last 6 months totals */
    fun monthlyTrend(expenses: List<Expense>): List<Pair<String, Double>> {
        val out = ArrayList<Pair<String, Double>>()
        for (i in 5 downTo 0) {
            val m = LocalDate.now().minusMonths(i.toLong()).toString().substring(0, 7)
            val label = AppState_MONTHS_LABEL(m)
            out.add(label to expenses.filter { it.date.startsWith(m) }.sumOf { it.amount })
        }
        return out
    }

    private fun AppState_MONTHS_LABEL(m: String): String {
        val idx = m.substring(5, 7).toIntOrNull() ?: 1
        val names = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        return names[(idx - 1).coerceIn(0, 11)]
    }
}
