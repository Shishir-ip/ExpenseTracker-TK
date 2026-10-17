package com.expensetracker.tk.ui.screens

import androidx.compose.foundation.background
import com.expensetracker.tk.ui.components.rememberHaptic
import com.expensetracker.tk.data.SyncState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.tk.R
import com.expensetracker.tk.data.AiContext
import com.expensetracker.tk.data.Categories
import com.expensetracker.tk.data.LocalInsights
import com.expensetracker.tk.data.OpenRouter
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.components.BarChart
import com.expensetracker.tk.ui.components.BarEntry
import com.expensetracker.tk.ui.components.CardHeader
import com.expensetracker.tk.ui.components.CategoryIcon
import com.expensetracker.tk.ui.components.EmptyState
import com.expensetracker.tk.ui.components.Heatmap30
import com.expensetracker.tk.ui.components.LineChart
import com.expensetracker.tk.ui.components.SectionCard
import com.expensetracker.tk.ui.components.SimpleMarkdown
import com.expensetracker.tk.ui.theme.AiGlow
import com.expensetracker.tk.ui.theme.Danger
import com.expensetracker.tk.ui.theme.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(state: AppState) {
    val ui = state.ui.collectAsState().value
    val currency = state.prefs.currencySymbol
    val exps = state.calcExpenses()
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    val uiSync = state.ui.collectAsState().value.sync

    var analysis by remember { mutableStateOf<String?>(null) }
    var analysisLoading by remember { mutableStateOf(false) }
    var analysisError by remember { mutableStateOf<String?>(null) }

    var chatInput by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<Pair<String, String>>()) } // role to text
    var chatLoading by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = uiSync == SyncState.SYNCING,
        onRefresh = { state.manualSync() },
        modifier = Modifier.fillMaxSize()
    ) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(AiGlow, Color(0xFF6366F1), Color(0xFF06B6D4)))),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Insights, null, tint = Color.White) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("Deep analytics for your spending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (exps.isEmpty()) {
            item {
                SectionCard { EmptyState(Icons.Default.Insights, "Not enough data", "Add some expenses first and come back", illustration = R.drawable.ill_chart) }
            }
        } else {
            // ---------- predictions ----------
            item {
                val p = remember(exps) { LocalInsights.predictions(exps) }
                SectionCard {
                    CardHeader("Predictions", Icons.Default.TrendingUp, MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PredCell(Modifier.weight(1f), "Next 7 days", "$currency${AppState.fmtInt(p.weekPred)}")
                        PredCell(Modifier.weight(1f), "Month projection", "$currency${AppState.fmtInt(p.monthPred)}")
                        PredCell(Modifier.weight(1f), "Days left", "${p.daysLeft}")
                    }
                    val monthTotal = exps.filter { it.date.startsWith(AppState.todayMonth()) }.sumOf { it.amount }
                    if (ui.monthlyTarget > 0) {
                        val totalTarget = ui.monthlyTarget * AppState.daysInMonth()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (monthTotal > totalTarget) "⚠ Projected over target by $currency${AppState.fmtInt(monthTotal - totalTarget)}"
                            else "✓ Projected under target by $currency${AppState.fmtInt(totalTarget - monthTotal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (monthTotal > totalTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ---------- 30 day heatmap ----------
            item {
                val days = remember(exps) {
                    (29 downTo 0).map { i ->
                        val d = LocalDate.now().minusDays(i.toLong()).toString()
                        d to exps.filter { it.date == d }.sumOf { it.amount }
                    }
                }
                var dayDetail by remember { mutableStateOf<String?>(null) }
                SectionCard {
                    CardHeader("Last 30 Days Intensity", Icons.Default.CalendarMonth, AiGlow)
                    Heatmap30(days, onDayClick = { d -> dayDetail = if (dayDetail == d) null else d })
                    dayDetail?.let { dstr ->
                        Spacer(Modifier.height(10.dp))
                        val dayExps = exps.filter { it.date == dstr }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    try {
                                        val d = AppState.parseDate(dstr)
                                        "${d.dayOfMonth} ${AppState.MONTHS[d.monthValue - 1]}"
                                    } catch (e: Exception) { dstr },
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "$currency${AppState.fmtInt(dayExps.sumOf { it.amount })} · ${dayExps.size} transactions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { dayDetail = null }) { Text("Close") }
                        }
                        if (dayExps.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                dayExps.sortedByDescending { it.amount }.take(5).forEach { e ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryIcon(e.category, 14.dp, Color(Categories.byId(e.category).color))
                                        Spacer(Modifier.width(8.dp))
                                        Text(e.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Text("$currency${AppState.fmtInt(e.amount)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------- weekday pattern ----------
            item {
                val pattern = remember(exps) { LocalInsights.weekdayPattern(exps) }
                val names = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                val best = pattern.indices.maxByOrNull { pattern[it] } ?: 0
                SectionCard {
                    CardHeader("Weekly Pattern", Icons.Default.QueryStats, MaterialTheme.colorScheme.primary)
                    BarChart(names.mapIndexed { i, n -> BarEntry(n, pattern[i], i == best) })
                    Text(
                        "You spend the most on ${names[best]}s (avg $currency${AppState.fmtInt(pattern[best])}).",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---------- monthly trend ----------
            item {
                val trend = remember(exps) { LocalInsights.monthlyTrend(exps) }
                val thisMonth = AppState.todayMonth()
                SectionCard {
                    CardHeader("6-Month Trend", Icons.Default.CalendarMonth, MaterialTheme.colorScheme.primary)
                    LineChart(
                        values = trend.map { it.second },
                        labels = trend.map { it.first.take(3) },
                        color = MaterialTheme.colorScheme.primary,
                        currency = currency
                    )
                }
            }

            // ---------- year overview ----------
            item {
                val currentYear = java.time.Year.now().value
                var year by remember { mutableStateOf(currentYear) }
                val monthTotals = remember(exps, year) {
                    (1..12).map { m -> exps.filter { it.date.startsWith("%04d-%02d".format(year, m)) }.sumOf { it.amount } }
                }
                val yearTotal = monthTotals.sum()
                val lastYearTotal = remember(exps, year) {
                    exps.filter { it.date.startsWith((year - 1).toString()) }.sumOf { it.amount }
                }
                val yoyDelta = if (lastYearTotal > 0) ((yearTotal - lastYearTotal) / lastYearTotal * 100).toInt() else null
                val maxMonth = (monthTotals.maxOrNull() ?: 0.0).coerceAtLeast(1.0)

                SectionCard {
                    CardHeader("Year Overview", Icons.Default.CalendarMonth, MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { year -= 1 }, enabled = year > 2015) {
                            Icon(Icons.Default.ChevronLeft, "Previous year")
                        }
                        Text("$year", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
                        IconButton(onClick = { year += 1 }, enabled = year < currentYear) {
                            Icon(Icons.Default.ChevronRight, "Next year")
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        monthTotals.chunked(3).forEachIndexed { rowIdx, chunk ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                chunk.forEachIndexed { colIdx, total ->
                                    val m = rowIdx * 3 + colIdx
                                    val ratio = (total / maxMonth).coerceIn(0.0, 1.0)
                                    val bg = if (total <= 0.0) MaterialTheme.colorScheme.surfaceVariant
                                             else com.expensetracker.tk.ui.theme.Primary.copy(alpha = (0.18f + 0.72f * ratio).toFloat())
                                    val fg = if (total <= 0.0) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(bg)
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(AppState.MONTHS[m].take(3), style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold, color = fg)
                                        Text(if (total > 0) "$currency${AppState.fmtInt(total)}" else "—",
                                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold,
                                            color = fg, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Year total: $currency${AppState.fmtInt(yearTotal)} · Avg ${AppState.fmtInt(yearTotal / 12)}/mo",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (yoyDelta != null) {
                        Text(
                            (if (yoyDelta >= 0) "\u25B2 $yoyDelta% vs ${year - 1}" else "\u25BC ${-yoyDelta}% vs ${year - 1}") +
                                    " ($currency${AppState.fmtInt(lastYearTotal)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (yoyDelta >= 0) Danger else Success,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ---------- category deep dive ----------
            item {
                val thisMonth = AppState.todayMonth()
                val byCat = remember(exps) {
                    exps.filter { it.date.startsWith(thisMonth) }.groupBy { it.category }
                        .map { (k, v) -> Triple(k, v.sumOf { it.amount }, v.size) }
                        .sortedByDescending { it.second }
                }
                if (byCat.isNotEmpty()) {
                    SectionCard {
                        CardHeader("This Month by Category", Icons.Default.Flag, MaterialTheme.colorScheme.primary)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            byCat.forEach { (cat, amount, count) ->
                                val c = Categories.byId(cat)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color(c.color).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                        CategoryIcon(cat, 18.dp, Color(c.color))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row {
                                            Text(c.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                            Text("$currency${AppState.fmtInt(amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                                        }
                                        Text("$count transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------- AI analysis ----------
            item {
                SectionCard {
                    CardHeader("AI Analysis", Icons.Default.AutoAwesome, AiGlow)
                    if (state.prefs.openRouterKey.isBlank()) {
                        Text("Add your OpenRouter API key in Settings → AI to get a personalised monthly analysis.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        analysis?.let { SimpleMarkdown(it) }
                        if (analysisLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = AiGlow)
                                Text("Reading your data…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        analysisError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            enabled = !analysisLoading,
                            onClick = {
                                scope.launch {
                                    analysisLoading = true; analysisError = null
                                    try {
                                        val ctx = AiContext.buildContext(exps, ui.budgets, ui.monthlyTarget, currency)
                                        val ai = OpenRouter(state.prefs.openRouterKey, state.prefs.openRouterModel)
                                        analysis = withContext(Dispatchers.IO) { ai.chat(listOf("system" to AiContext.insightsSystemPrompt(ctx)), 1200) }
                                    } catch (e: Exception) {
                                        analysisError = "AI failed: ${e.message?.take(160)}"
                                    } finally { analysisLoading = false }
                                }
                            }
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (analysis == null) "Generate monthly analysis" else "Regenerate")
                        }
                    }
                }
            }

            // ---------- AI chat ----------
            item {
                SectionCard {
                    CardHeader("Ask Your Financial AI", Icons.Default.Chat, AiGlow)
                    if (state.prefs.openRouterKey.isBlank()) {
                        Text("Needs an OpenRouter API key (Settings → AI).", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        if (chatHistory.isEmpty()) {
                            listOf(
                                "Am I on track with my budget?",
                                "Where is my money going?",
                                "How can I save more?"
                            ).forEach { q ->
                                Text(
                                    q,
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .clickable {
                                            if (!chatLoading) { chatInput = q; sendChat(state, exps, currency, chatInput, chatHistory,
                                                { chatHistory = it; chatInput = "" }, { chatLoading = it }) }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                        chatHistory.forEach { (role, text) ->
                            val isUser = role == "user"
                            Box(Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                                Box(
                                    Modifier
                                        .widthInMax(300.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(10.dp)
                                ) {
                                    if (isUser) Text(text, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyMedium)
                                    else SimpleMarkdown(text)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        if (chatLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = AiGlow)
                                Text("Thinking…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("Ask about your expenses…") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                maxLines = 3
                            )
                            Button(
                                enabled = chatInput.isNotBlank() && !chatLoading,
                                onClick = {
                                    haptic()
                                    sendChat(state, exps, currency, chatInput, chatHistory, { chatHistory = it; chatInput = "" }, { chatLoading = it })
                                }
                            ) { Icon(Icons.Default.Send, null, Modifier.size(16.dp)) }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(10.dp)) }
        }
    }
    }
}

private fun Modifier.widthInMax(w: androidx.compose.ui.unit.Dp) = this.then(Modifier.width(w))

private fun sendChat(
    state: AppState,
    exps: List<com.expensetracker.tk.data.Expense>,
    currency: String,
    question: String,
    history: List<Pair<String, String>>,
    onUpdate: (List<Pair<String, String>>) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    val ui = state.ui.value
    val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
    scope.launch {
        setLoading(true)
        val newHistory = history + ("user" to question)
        onUpdate(newHistory)
        try {
            val ctx = AiContext.buildContext(exps, ui.budgets, ui.monthlyTarget, currency)
            val ai = OpenRouter(state.prefs.openRouterKey, state.prefs.openRouterModel)
            val msgs = mutableListOf<Pair<String, String>>()
            msgs.add("system" to AiContext.chatSystemPrompt(ctx))
            newHistory.takeLast(6).forEach { (r, t) -> msgs.add(r to t) }
            val answer = withContext(Dispatchers.IO) { ai.chat(msgs, 1000) }
            onUpdate(newHistory + ("assistant" to answer))
        } catch (e: Exception) {
            onUpdate(newHistory + ("assistant" to "⚠ Sorry, I couldn't respond: ${e.message?.take(120)}"))
        } finally { setLoading(false) }
    }
}

@Composable
fun PredCell(modifier: Modifier, label: String, value: String) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(12.dp)
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, maxLines = 1)
    }
}
