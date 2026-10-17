package com.expensetracker.tk.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import java.time.YearMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.expensetracker.tk.UndoHost
import com.expensetracker.tk.data.AiContext
import kotlinx.coroutines.withTimeout
import com.expensetracker.tk.ui.components.SuccessBurst
import com.expensetracker.tk.ui.components.BurnDownChart
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import java.time.LocalTime
import com.expensetracker.tk.ui.components.shimmer
import com.expensetracker.tk.ui.components.Stagger
import com.expensetracker.tk.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.expensetracker.tk.data.Categories
import com.expensetracker.tk.data.Expense
import com.expensetracker.tk.data.OpenRouter
import com.expensetracker.tk.data.QuickPreset
import com.expensetracker.tk.data.SyncState
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.components.AnimatedAmount
import com.expensetracker.tk.ui.components.BarChart
import com.expensetracker.tk.ui.components.BarEntry
import com.expensetracker.tk.ui.components.CardHeader
import com.expensetracker.tk.ui.components.CategoryIcon
import com.expensetracker.tk.ui.components.DonutChart
import com.expensetracker.tk.ui.components.DonutSlice
import com.expensetracker.tk.ui.components.EmptyState
import com.expensetracker.tk.ui.components.MonthCalendarHeatmap
import com.expensetracker.tk.ui.components.SectionCard
import com.expensetracker.tk.ui.components.rememberHaptic
import com.expensetracker.tk.ui.components.SimpleMarkdown
import com.expensetracker.tk.ui.components.SyncBadge
import com.expensetracker.tk.ui.components.TransactionRow
import com.expensetracker.tk.ui.theme.Cyan
import com.expensetracker.tk.ui.theme.Danger
import com.expensetracker.tk.ui.theme.Success
import com.expensetracker.tk.ui.theme.Warning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: AppState, onAdd: () -> Unit, onEdit: (String, androidx.compose.ui.geometry.Rect?) -> Unit, onOpenSettings: () -> Unit = {}) {
    val ui = state.ui.collectAsState().value
    val currency = state.prefs.currencySymbol
    val exps = remember(ui.expenses, ui.includeBills) { state.calcExpenses() }
    var reorderMode by remember { mutableStateOf(false) }
    var showMonthSheet by remember { mutableStateOf(false) }
    var showTodaySheet by remember { mutableStateOf(false) }
    var showBudgetsSheet by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }

    // daily safe limit for the celebration trigger
    val todaySafe = remember(ui.expenses, ui.monthlyTarget, ui.includeBills) {
        val today = LocalDate.now()
        val dim = today.lengthOfMonth()
        val monthExps = exps.filter { it.date.startsWith(AppState.todayMonth()) }
        val monthTotal = monthExps.sumOf { it.amount }
        val todaySpent = monthExps.filter { it.date == today.toString() }.sumOf { it.amount }
        val target = ui.monthlyTarget
        if (target > 0 && ui.monthlyTargetMonth == AppState.todayMonth())
            (target * dim - (monthTotal - todaySpent) - (dim - today.dayOfMonth) * target).coerceAtLeast(0.0)
        else Double.MAX_VALUE
    }

    // assistant shortcut / quick tile: "voice" opens the mic sheet right away
    LaunchedEffect(state.pendingUiAction.value) {
        if (state.pendingUiAction.value == "voice") {
            state.pendingUiAction.value = null
            showVoiceSheet = true
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > 700.dp

        Column(Modifier.fillMaxSize()) {
            // ---------- greeting header ----------
            val hour = remember { LocalTime.now().hour }
            val greeting = when {
                hour < 5 -> "Up late?"
                hour < 12 -> "Good morning"
                hour < 17 -> "Good afternoon"
                else -> "Good evening"
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // avatar → Settings (row 1: identity, full width for any font scale)
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF8B5CF6), Color(0xFF06B6D4))))
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_splash),
                        contentDescription = "Settings",
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            java.time.LocalDate.now().let { d ->
                                d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3) +
                                        ", ${d.dayOfMonth} ${AppState.MONTHS[d.monthValue - 1]}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        SyncBadge(ui.sync, ui.pending)
                    }
                }
            }
            // row 2: actions get their own line so nothing ever squeezes the greeting
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // bills pill
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (ui.includeBills) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                        .clickable { state.toggleBills() }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.ReceiptLong, null,
                        tint = if (ui.includeBills) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Bills " + if (ui.includeBills) "on" else "off",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (ui.includeBills) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showVoiceSheet = true }) {
                    Icon(Icons.Default.Mic, "Log by voice", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { reorderMode = !reorderMode }) {
                    Icon(Icons.Default.Reorder, "Reorder cards", tint = if (reorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (reorderMode) {
                ReorderGrid(state, ui.cardOrder, onDone = { reorderMode = false })
            } else {
                val card: @Composable (String) -> Unit = { id ->
                    when (id) {
                        "status" -> StatusBanner(state, exps, currency)
                        "stats" -> StatsGrid(state, exps, currency, wide, onMonth = { showMonthSheet = true }, onToday = { showTodaySheet = true })
                        "quick" -> QuickAddCard(state, currency, underLimit = { it <= todaySafe })
                        "chart" -> ChartCard(state, exps, currency)
                        "transactions" -> RecentTxnsCard(state, exps, currency, onEdit, onAdd)
                        "streak" -> StreakCard(state, exps)
                        "budgets" -> BudgetsCard(state, exps, currency, onOpen = { showBudgetsSheet = true })
                        "summary" -> SummaryCard(state, exps, currency)
                        "insights" -> AiInsightsCard(state, exps, currency)
                    }
                }
                PullToRefreshBox(
                    isRefreshing = ui.sync == SyncState.SYNCING,
                    onRefresh = { state.manualSync() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (ui.expenses.isEmpty() && ui.sync == SyncState.SYNCING) {
                        // first-load skeleton
                        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(20.dp)).shimmer())
                                Box(Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(20.dp)).shimmer())
                            }
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(20.dp)).shimmer())
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp)).shimmer())
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)).shimmer())
                        }
                    } else if (wide) {
                        // two-pane: alternate cards side by side
                        val order = ui.cardOrder
                        Row(Modifier.fillMaxSize()) {
                            val left = order.filterIndexed { i, _ -> i % 2 == 0 }
                            val right = order.filterIndexed { i, _ -> i % 2 == 1 }
                            Column(Modifier.weight(1f)) {
                                TwoPaneCards(left, 0, card)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                TwoPaneCards(right, 1, card)
                            }
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val order = ui.cardOrder
                            items(order.size, key = { order[it] }) { idx ->
                                Stagger(idx) { Box { card(order[idx]) } }
                            }
                            item { Spacer(Modifier.height(4.dp)) }
                        }
                    }
                }
            }
        }

        // ---------- expandable sheets ----------
        if (showVoiceSheet) {
            ModalBottomSheet(onDismissRequest = { showVoiceSheet = false }) {
                VoiceExpenseSheet(state, currency) { showVoiceSheet = false }
            }
        }
        if (showMonthSheet) {
            ModalBottomSheet(onDismissRequest = { showMonthSheet = false }) {
                MonthSheetContent(state, exps, currency)
            }
        }
        if (showTodaySheet) {
            ModalBottomSheet(onDismissRequest = { showTodaySheet = false }) {
                TodaySheetContent(state, exps, currency, onEdit)
            }
        }
        if (showBudgetsSheet) {
            ModalBottomSheet(onDismissRequest = { showBudgetsSheet = false }) {
                BudgetsSheetContent(state, exps, currency)
            }
        }

        SuccessBurst(visible = celebrate, onDone = { celebrate = false })
    }
}

@Composable
fun TwoPaneCards(ids: List<String>, startIndex: Int, card: @Composable (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(ids.size, key = { ids[it] }) { idx ->
            Stagger(startIndex + idx * 2) { Box { card(ids[idx]) } }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

// ============================================================
// STATUS BANNER — same logic as web app (avg till yesterday vs target)
// ============================================================
@Composable
fun StatusBanner(state: AppState, exps: List<Expense>, currency: String) {
    val ui = state.ui.collectAsState().value
    val today = LocalDate.now()
    val todayStr = today.toString()
    val monthStr = todayStr.substring(0, 7)
    val daysInMonth = today.lengthOfMonth()
    val dayOfMonth = today.dayOfMonth
    val remainingAfterToday = daysInMonth - dayOfMonth

    val monthTotal = exps.filter { it.date.startsWith(monthStr) }.sumOf { it.amount }
    val todaySpent = exps.filter { it.date == todayStr }.sumOf { it.amount }
    val tillYesterday = monthTotal - todaySpent
    val daysTillYesterday = dayOfMonth - 1
    val avg = if (daysTillYesterday > 0) tillYesterday / daysTillYesterday else 0.0

    val target = ui.monthlyTarget
    val targetSet = target > 0 && ui.monthlyTargetMonth == monthStr

    data class Style(val bg: Color, val fg: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector,
                     val title: String, val badge: String, val badgeColor: Color)

    val style: Style
    var detailLines = ArrayList<Pair<String, Color>>()

    if (!targetSet) {
        style = Style(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.TrendingUp, "Set Your Monthly Target", "NO TARGET", MaterialTheme.colorScheme.onSurfaceVariant)
        detailLines.add("Open the Budgets card to set a daily-avg target for ${AppState.MONTHS[today.monthValue - 1]}" to MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        val totalTarget = target * daysInMonth
        val over = avg > target
        val close = avg > target * 0.85 && !over
        val safeToday = (totalTarget - tillYesterday - remainingAfterToday * target).coerceAtLeast(0.0)
        style = when {
            over -> {
                val overBy = avg - target
                val maxDaily = if (remainingAfterToday >= 0) (totalTarget - monthTotal) / (remainingAfterToday + 1) else 0.0
                Style(Color(0x1AEF4444), Danger, Icons.Default.Warning, "Over Monthly Target!",
                    "OVER BY $currency${"%.0f".format(overBy)}", Danger)
            }
            close -> Style(Color(0x1AF59E0B), Warning, Icons.Default.Warning, "Getting Close to Limit", "CLOSE", Warning)
            else -> Style(Color(0x1410B981), Success, Icons.Default.CheckCircle, "On Track", "ON TRACK", Success)
        }
        detailLines.add("Avg till yesterday: $currency${"%.0f".format(avg)}/day · Target: $currency${"%.0f".format(target)}/day" to MaterialTheme.colorScheme.onSurfaceVariant)
        if (todaySpent > 0) {
            val left = safeToday - todaySpent
            detailLines.add(
                (if (left > 0) "Spent $currency${"%.0f".format(todaySpent)} today · $currency${"%.0f".format(left)} more is safe"
                else "Spent $currency${"%.0f".format(todaySpent)} today · safe limit was $currency${"%.0f".format(safeToday)}")
                        to (if (left > 0) Success else Warning)
            )
        } else {
            detailLines.add("You can spend up to $currency${"%.0f".format(safeToday)} today safely" to Success)
        }
    }

    // gradient hero card
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    if (targetSet) Brush.verticalGradient(listOf(style.bg.copy(alpha = 0.30f), style.bg.copy(alpha = 0.08f)))
                    else Brush.verticalGradient(listOf(style.bg, style.bg))
                )
                .border(1.dp, style.badgeColor.copy(alpha = if (targetSet) 0.4f else 0.25f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(style.badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(style.icon, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(style.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    detailLines.forEach { (text, _) ->
                        Text(
                            text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
                Text(style.badge, style = MaterialTheme.typography.labelSmall, color = if (targetSet) Color.White else style.badgeColor, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(style.badgeColor.copy(alpha = if (targetSet) 1f else 0.14f)).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

// ============================================================
// STATS GRID
// ============================================================
@Composable
fun StatsGrid(state: AppState, exps: List<Expense>, currency: String, wide: Boolean, onMonth: () -> Unit, onToday: () -> Unit) {
    val today = LocalDate.now().toString()
    val monthStr = today.substring(0, 7)
    val lastMonth = LocalDate.now().minusMonths(1).toString().substring(0, 7)
    val dayOfMonth = LocalDate.now().dayOfMonth

    val total = exps.sumOf { it.amount }
    val month = exps.filter { it.date.startsWith(monthStr) }.sumOf { it.amount }
    val lastMonthTotal = exps.filter { it.date.startsWith(lastMonth) }.sumOf { it.amount }
    val todayTotal = exps.filter { it.date == today }.sumOf { it.amount }
    val dailyAvg = month / dayOfMonth.coerceAtLeast(1)
    val monthDelta = if (lastMonthTotal > 0) ((month - lastMonthTotal) / lastMonthTotal * 100).toInt() else null

    // all-time facts for the Total Spent card
    val txnCount = exps.size
    val topCat = exps.groupBy { it.category }.maxByOrNull { it.value.sumOf { e -> e.amount } }

    // last 7 days for the sparkline
    val last7 = remember(exps) {
        (6 downTo 0).map { i ->
            val d = LocalDate.now().minusDays(i.toLong()).toString()
            exps.filter { it.date == d }.sumOf { it.amount }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "Total Spent", total, currency, Icons.Default.Savings,
                Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5))), tint = Color(0xFF6366F1),
                sub = buildString {
                    append("$txnCount transactions")
                    topCat?.let { append(" · ${Categories.byId(it.key).name} top") }
                },
                subColor = MaterialTheme.colorScheme.onSurfaceVariant)
            StatCard(Modifier.weight(1f), "This Month", month, currency, Icons.Default.CalendarMonth,
                Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))), tint = Color(0xFF8B5CF6),
                sub = monthDelta?.let { (if (it >= 0) "▲ $it% vs last month" else "▼ ${-it}% vs last month") } ?: "${AppState.MONTHS[LocalDate.now().monthValue - 1]}",
                subColor = if ((monthDelta ?: 0) > 0) Danger else Success, onClick = onMonth,
                sparkline = last7)
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "Today", todayTotal, currency, Icons.Default.Today,
                Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF0891B2))), tint = Color(0xFF06B6D4), onClick = onToday)
            StatCard(Modifier.weight(1f), "Daily Average", dailyAvg, currency, Icons.Default.TrendingUp,
                Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))), tint = Color(0xFF10B981))
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier, label: String, value: Double, currency: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, gradient: Brush,
    tint: Color = Color(0xFF6366F1),
    sub: String? = null, subColor: Color = Success, onClick: (() -> Unit)? = null,
    sparkline: List<Double>? = null
) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(value) { played = true }
    val scale by animateFloatAsState(if (played) 1f else 0.92f, tween(350), label = "card")
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        // soft tint wash unique to each card
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.14f), Color.Transparent)))
        )
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(gradient),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(8.dp))
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            AnimatedAmount(value, currency, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 19.sp))
            if (sub != null) {
                Text(sub, style = MaterialTheme.typography.labelSmall, color = subColor, fontWeight = FontWeight.SemiBold)
            }
            if (sparkline != null) {
                Spacer(Modifier.height(6.dp))
                Sparkline(sparkline, tint, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Tiny 7-day trend line drawn inside the stat card. */
@Composable
fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.height(30.dp)) {
        if (values.size < 2) return@Canvas
        val maxV = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0).toFloat()
        val stepX = size.width / (values.size - 1)
        val pts = values.mapIndexed { i, v ->
            androidx.compose.ui.geometry.Offset(i * stepX, size.height * (1f - (v.toFloat() / maxV) * 0.8f))
        }
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]; val cur = pts[i]
                val midX = (prev.x + cur.x) / 2f
                cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
            }
        }
        val fill = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), Color.Transparent)))
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

// ============================================================
// QUICK ADD
// ============================================================
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun QuickAddCard(state: AppState, currency: String, underLimit: (Double) -> Boolean = { true }, onCelebrate: () -> Unit = {}) {
    val ui = state.ui.collectAsState().value
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    var justAdded by remember { mutableStateOf<String?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var collapsed = remember { mutableStateOf(state.prefs.quickCollapsed) }
    val celebrate: (Boolean) -> Unit = { if (it) onCelebrate() }
    val visiblePresets = if (collapsed.value) ui.presets.take(3) else ui.presets

    SectionCard {
        CardHeader("Quick Add", Icons.Default.Bolt, MaterialTheme.colorScheme.primary,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("tap = log · hold = edit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = {
                        collapsed.value = !collapsed.value
                        state.prefs.quickCollapsed = collapsed.value
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (collapsed.value) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            if (collapsed.value) "Expand" else "Collapse",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            })
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            visiblePresets.forEach { p ->
                val cat = Categories.byId(p.category)
                val highlight = justAdded == p.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (highlight) Color(cat.color).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .combinedClickable(
                            onClick = {
                                haptic()
                                state.addExpense(p.title, p.amount, p.category, AppState.todayStr(), "", false)
                                if (underLimit(p.amount)) celebrate(true)
                                justAdded = p.id
                                scope.launch {
                                    kotlinx.coroutines.delay(1200)
                                    if (justAdded == p.id) justAdded = null
                                }
                            },
                            onLongClick = { editingId = p.id }
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(cat.color).copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        CategoryIcon(p.category, 16.dp, Color(cat.color))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                        Text(cat.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AnimatedVisibility(visible = highlight, enter = fadeIn(), exit = fadeOut()) {
                        Icon(Icons.Default.CheckCircle, null, tint = Success)
                    }
                    Text("$currency${AppState.fmtAmount(p.amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (collapsed.value && ui.presets.size > 3) {
                TextButton(onClick = {
                    collapsed.value = false
                    state.prefs.quickCollapsed = false
                }) { Text("Show all ${ui.presets.size} presets") }
            } else if (!collapsed.value && ui.presets.size > 3) {
                TextButton(onClick = {
                    collapsed.value = true
                    state.prefs.quickCollapsed = true
                }) { Text("Show less") }
            }
            Text("Tap a preset to log it instantly. Long-press to edit · full editor in Settings → Quick presets.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    // preset editor sheet (long-press)
    editingId?.let { id ->
        val preset = ui.presets.firstOrNull { it.id == id }
        if (preset == null) {
            editingId = null
        } else {
            ModalBottomSheet(onDismissRequest = { editingId = null }) {
                PresetEditor(
                    initial = preset,
                    currency = currency,
                    canDelete = ui.presets.size > 1,
                    onSave = { updated ->
                        val idx = state.ui.value.presets.indexOfFirst { it.id == id }
                        if (idx >= 0) {
                            val list = state.ui.value.presets.toMutableList()
                            list[idx] = updated
                            state.savePresets(list)
                        }
                        editingId = null
                    },
                    onDelete = {
                        state.savePresets(state.ui.value.presets.filter { it.id != id })
                        editingId = null
                    }
                )
            }
        }
    }
}

/** Editor for one quick-add preset (label, amount, category). */
@Composable
fun PresetEditor(initial: QuickPreset, currency: String, canDelete: Boolean, onSave: (QuickPreset) -> Unit, onDelete: () -> Unit) {
    var label by remember(initial.id) { mutableStateOf(initial.label) }
    var amount by remember(initial.id) { mutableStateOf(if (initial.amount % 1.0 == 0.0) "%.0f".format(initial.amount) else initial.amount.toString()) }
    var category by remember(initial.id) { mutableStateOf(initial.category) }

    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Edit quick action", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(
            value = label, onValueChange = { label = it },
            label = { Text("Name shown on the button") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) },
            label = { Text("Amount ($currency)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
        )
        Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Categories.all.chunked(4).forEach { rowCats ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowCats.forEach { c ->
                        val selected = category == c.id
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color(c.color).copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { category = c.id }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CategoryIcon(c.id, 14.dp, Color(c.color))
                            Text(c.name, style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color(c.color) else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (canDelete) {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.height(50.dp)
                ) { Text("Delete") }
            }
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: initial.amount
                    onSave(initial.copy(label = label.ifBlank { initial.label }, title = label.ifBlank { initial.label }, amount = amt, category = category))
                },
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text("Save preset", fontWeight = FontWeight.Bold) }
        }
    }
}

// ============================================================
// CHART CARD — donut with period chips
// ============================================================
@Composable
fun ChartCard(state: AppState, exps: List<Expense>, currency: String) {
    var period by remember { mutableStateOf(0) } // 0 month, 1 30d, 2 all
    var selected by remember { mutableStateOf<String?>(null) }
    val filtered = remember(exps, period) {
        when (period) {
            0 -> exps.filter { it.date.startsWith(AppState.todayMonth()) }
            1 -> exps.filter { it.date >= LocalDate.now().minusDays(30).toString() }
            else -> exps
        }
    }
    val slices = remember(filtered) {
        filtered.groupBy { it.category }
            .map { (k, v) -> DonutSlice(Categories.byId(k).name, v.sumOf { it.amount }, Color(Categories.byId(k).color)) }
            .sortedByDescending { it.value }
    }
    val total = filtered.sumOf { it.amount }

    SectionCard {
        CardHeader("Spending by Category", Icons.Default.PieChart, MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
            listOf("This month", "30 days", "All time").forEachIndexed { i, label ->
                FilterChip(
                    selected = period == i,
                    onClick = { period = i },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
        if (total <= 0) {
            EmptyState(Icons.Default.PieChart, "Nothing to chart yet", "Add a few expenses and the breakdown appears here", illustration = R.drawable.ill_chart)
        } else {
            val selSlice = slices.firstOrNull { it.label == selected }
            DonutChart(
                slices, periodLabel(period), "$currency${AppState.fmtInt(total)}",
                onSliceClick = { sl -> selected = if (selected == sl.label) null else sl.label },
                selectedSlice = selSlice,
                currency = currency
            )
            selSlice?.let { sl ->
                val count = filtered.count { Categories.byId(it.category).name == sl.label }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "$count transactions in ${sl.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { selected = null }) { Text("Clear") }
                }
            } ?: run {
                Text(
                    "Tap a slice or legend row to inspect a category",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

private fun periodLabel(p: Int) = when (p) { 0 -> "THIS MONTH"; 1 -> "30 DAYS"; else -> "ALL TIME" }

// ============================================================
// RECENT TRANSACTIONS
// ============================================================
@Composable
fun RecentTxnsCard(state: AppState, exps: List<Expense>, currency: String, onEdit: (String, androidx.compose.ui.geometry.Rect?) -> Unit, onAdd: () -> Unit) {
    SectionCard {
        CardHeader("Recent Transactions", Icons.Default.ReceiptLong, MaterialTheme.colorScheme.primary,
            trailing = { Text("swipe to delete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) })
        if (exps.isEmpty()) {
            EmptyState(Icons.Default.ReceiptLong, "No transactions yet", "Tap + to add your first expense", illustration = R.drawable.ill_wallet)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exps.take(5).forEach { e ->
                    var rowBounds by remember(e.id) { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                    TransactionRow(e, currency,
                        onBoundsChange = { rowBounds = it },
                        onClick = { onEdit(e.id, rowBounds) },
                        onDelete = {
                            val stash = state.stashExpenseForDelete(e.id)
                            if (stash != null) {
                                UndoHost.post("Deleted " + stash.title.take(24),
                                    undo = { state.undoDeleteExpense(stash) },
                                    onDismissed = { state.finalizeExpenseDelete(stash) })
                            }
                        })
                }
            }
        }
    }
}

// ============================================================
// BUDGETS
// ============================================================
@Composable
fun BudgetsCard(state: AppState, exps: List<Expense>, currency: String, onOpen: () -> Unit) {
    val ui = state.ui.collectAsState().value
    val monthStr = AppState.todayMonth()
    val monthExps = exps.filter { it.date.startsWith(monthStr) }

    SectionCard(onClick = onOpen) {
        CardHeader("Budgets", Icons.Default.Savings, MaterialTheme.colorScheme.primary,
            trailing = { Text("tap to edit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) })
        if (ui.budgets.isEmpty() && ui.monthlyTarget <= 0) {
            Text("No budgets set yet — tap to add category budgets and a monthly target.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ui.monthlyTarget > 0 && ui.monthlyTargetMonth == monthStr) {
                    val totalTarget = ui.monthlyTarget * AppState.daysInMonth()
                    val spent = monthExps.sumOf { it.amount }
                    BudgetBar("Monthly target ($currency${"%.0f".format(ui.monthlyTarget)}/day)", spent, totalTarget, currency, Success)
                    // pace vs target burn-down
                    Spacer(Modifier.height(2.dp))
                    Text("Pace vs target", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val cumulative = remember(exps) {
                        var acc = 0.0
                        (1..AppState.daysInMonth()).map { d ->
                            acc += monthExps.filter { (it.date.substringAfterLast('-').toIntOrNull() ?: 0) == d }.sumOf { it.amount }
                            acc
                        }
                    }
                    BurnDownChart(cumulative, AppState.daysInMonth(), totalTarget, MaterialTheme.colorScheme.primary)
                }
                ui.budgets.forEach { (cat, limit) ->
                    if (limit <= 0) return@forEach
                    val spent = monthExps.filter { it.category == cat }.sumOf { it.amount }
                    val c = Color(Categories.byId(cat).color)
                    BudgetBar(Categories.byId(cat).name, spent, limit, currency, c)
                }
            }
        }
    }
}

@Composable
fun BudgetBar(label: String, spent: Double, limit: Double, currency: String, color: Color) {
    val pct = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0) else 0.0
    val over = spent > limit
    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("$currency${AppState.fmtInt(spent)} / $currency${AppState.fmtInt(limit)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (over) Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { pct.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = if (over) Danger else color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ============================================================
// SUMMARY
// ============================================================
@Composable
fun SummaryCard(state: AppState, exps: List<Expense>, currency: String) {
    val monthStr = AppState.todayMonth()
    val yearStr = todayYear()
    // average monthly across months with data
    val monthsWithData = exps.map { it.date.substring(0, 7) }.distinct()
    val avgMonthly = if (monthsWithData.isNotEmpty()) exps.sumOf { it.amount } / monthsWithData.size else 0.0
    val highest = exps.groupBy { it.date.substring(0, 7) }.maxByOrNull { it.value.sumOf { e -> e.amount } }
    val yearSpent = exps.filter { it.date.startsWith(yearStr) }.sumOf { it.amount }

    SectionCard {
        CardHeader("All-Time Summary", Icons.Default.DoneAll, MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCell(Modifier.weight(1f), "Avg / month", "$currency${AppState.fmtInt(avgMonthly)}")
            SummaryCell(Modifier.weight(1f), "Highest month",
                highest?.let {
                    val m = it.key
                    val idx = (m.getOrNull(5)?.toString()?.toIntOrNull() ?: 0) * 10 +
                              (m.getOrNull(6)?.toString()?.toIntOrNull() ?: 0)
                    "${AppState.MONTHS[(idx - 1).coerceIn(0, 11)]} ${m.take(4)}"
                } ?: "—",
                sub = highest?.let { "$currency${AppState.fmtInt(it.value.sumOf { e -> e.amount })}" } ?: "")
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCell(Modifier.weight(1f), "This year", "$currency${AppState.fmtInt(yearSpent)}")
            SummaryCell(Modifier.weight(1f), "Transactions", "${exps.size}")
        }
    }
}

private fun todayYear() = AppState.todayStr().substring(0, 4)

@Composable
fun SummaryCell(modifier: Modifier, label: String, value: String, sub: String? = null) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(12.dp)
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ============================================================
// AI INSIGHTS CARD
// ============================================================
@Composable
fun AiInsightsCard(state: AppState, exps: List<Expense>, currency: String) {
    val ui = state.ui.collectAsState().value
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    SectionCard {
        CardHeader("AI Insights", Icons.Default.AutoAwesome, Color(0xFF8B5CF6))
        if (!state.prefs.cloudConfigured || state.prefs.openRouterKey.isBlank()) {
            Text("Add your OpenRouter API key in Settings → AI to unlock spending analysis and the AI chat.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            result?.let { r ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SimpleMarkdown(r)
                }
            } ?: run {
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF8B5CF6))
                        Text("Analysing your spending…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(error ?: "Get a personalised analysis of your spending, budgets and saving opportunities.",
                        style = MaterialTheme.typography.bodyMedium, color = if (error != null) Danger else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            loading = true; error = null
                            try {
                                val ctx = AiContext.buildContext(exps, ui.budgets, ui.monthlyTarget, currency)
                                val ai = OpenRouter(state.prefs.openRouterKey, state.prefs.openRouterModel)
                                result = withContext(Dispatchers.IO) { ai.chat(listOf("system" to AiContext.insightsSystemPrompt(ctx)), 1200) }
                            } catch (e: Exception) {
                                error = "AI failed: ${e.message?.take(160)}"
                            } finally { loading = false }
                        }
                    },
                    enabled = !loading
                ) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (result == null) "Generate insights" else "Regenerate")
                }
            }
        }
    }
}

// ============================================================
// SHEETS
// ============================================================
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MonthSheetContent(state: AppState, exps: List<Expense>, currency: String) {
    val current = YearMonth.now()
    val scope = rememberCoroutineScope()
    // pages: Jan 2015 … current month
    val months = remember {
        val list = ArrayList<YearMonth>()
        var ym = YearMonth.of(2015, 1)
        while (!ym.isAfter(current)) { list.add(ym); ym = ym.plusMonths(1) }
        list
    }
    val pager = rememberPagerState(initialPage = months.lastIndex) { months.size }
    var daySel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pager.currentPage) { daySel = null }
    val shownMonth = months.getOrNull(pager.currentPage) ?: current

    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
        // month navigation (arrows + swipe both work)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) } },
                enabled = pager.currentPage > 0
            ) { Icon(Icons.Default.ChevronLeft, "Previous month") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${AppState.MONTHS[shownMonth.monthValue - 1]} ${shownMonth.year}",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (shownMonth == current) "Current month · swipe for history" else "Back to current month",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(enabled = shownMonth != current) {
                        scope.launch { pager.animateScrollToPage(months.lastIndex) }
                    }
                )
            }
            IconButton(
                onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(months.lastIndex)) } },
                enabled = pager.currentPage < months.lastIndex
            ) { Icon(Icons.Default.ChevronRight, "Next month") }
        }
        Spacer(Modifier.height(8.dp))

        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth()) { page ->
            val month = months[page]
            val monthStr = "%04d-%02d".format(month.year, month.monthValue)
            val dayTotals = exps.filter { it.date.startsWith(monthStr) }
                .groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.amount } }
            val total = dayTotals.values.sum()
            val txnCount = exps.count { it.date.startsWith(monthStr) }
            Column {
                Text("$currency${AppState.fmtAmount(total)} · $txnCount transactions",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                MonthCalendarHeatmap(dayTotals, month.year, month.monthValue, currency,
                    onDayClick = { d -> daySel = d })
            }
        }

        // tapped-day detail
        daySel?.let { dstr ->
            val dayExps = exps.filter { it.date == dstr }
            Spacer(Modifier.height(14.dp))
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            try {
                                val d = AppState.parseDate(dstr)
                                "${d.dayOfMonth} ${AppState.MONTHS[d.monthValue - 1]} ${d.year}"
                            } catch (e: Exception) { dstr },
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                        )
                        Text("$currency${AppState.fmtAmount(dayExps.sumOf { it.amount })} · ${dayExps.size} transactions",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { daySel = null }) { Text("Close") }
                }
                Spacer(Modifier.height(6.dp))
                if (dayExps.isEmpty()) {
                    Text("No expenses that day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayExps.sortedByDescending { it.amount }.take(8).forEach { e ->
                            TransactionRow(e, currency, onClick = {}, onDelete = {
                                val stash = state.stashExpenseForDelete(e.id)
                                if (stash != null) {
                                    com.expensetracker.tk.UndoHost.post("Deleted " + stash.title.take(24),
                                        undo = { state.undoDeleteExpense(stash) },
                                        onDismissed = { state.finalizeExpenseDelete(stash) })
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodaySheetContent(state: AppState, exps: List<Expense>, currency: String, onEdit: (String, androidx.compose.ui.geometry.Rect?) -> Unit) {
    val today = AppState.todayStr()
    val list = exps.filter { it.date == today }.sortedByDescending { it.amount }
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
        Text("Today · ${today.formatPretty()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("$currency${AppState.fmtAmount(list.sumOf { it.amount })} across ${list.size} transactions",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        if (list.isEmpty()) {
            EmptyState(Icons.Default.Today, "No transactions today", "Add an expense to get started", illustration = R.drawable.ill_wallet)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                list.forEach { e ->
                    var rowBounds by remember(e.id) { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                    TransactionRow(e, currency, onBoundsChange = { rowBounds = it }, onClick = { onEdit(e.id, rowBounds) }, onDelete = {
                        val stash = state.stashExpenseForDelete(e.id)
                        if (stash != null) {
                            UndoHost.post("Deleted " + stash.title.take(24),
                                undo = { state.undoDeleteExpense(stash) },
                                onDismissed = { state.finalizeExpenseDelete(stash) })
                        }
                    })
                }
            }
        }
    }
}

private fun String.formatPretty(): String {
    val d = AppState.parseDate(this)
    return "${d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, ${d.dayOfMonth} ${AppState.MONTHS[d.monthValue - 1]}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsSheetContent(state: AppState, exps: List<Expense>, currency: String) {
    val ui = state.ui.collectAsState().value
    val monthStr = AppState.todayMonth()
    val monthExps = remember(exps) { exps.filter { it.date.startsWith(monthStr) } }
    var targetText by remember { mutableStateOf(if (ui.monthlyTarget > 0) "%.0f".format(ui.monthlyTarget) else "") }

    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Budgets & Target", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)

        // monthly target
        Column {
            Text("Monthly daily-avg target (${currency}/day)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Monthly total target: $currency${"%.0f".format((targetText.toDoubleOrNull() ?: 0.0) * AppState.daysInMonth())}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedNumberField(targetText, { targetText = it }, "e.g. 250")
                Button(onClick = {
                    state.setMonthlyTarget(targetText.toDoubleOrNull() ?: 0.0)
                }) { Text("Save target") }
            }
        }

        HorizontalDividerSoft()

        // category budgets
        Text("Category budgets (monthly)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Categories.all.forEach { cat ->
            val limit = ui.budgets[cat.id] ?: 0.0
            val spent = monthExps.filter { it.category == cat.id }.sumOf { it.amount }
            var text by remember(ui.budgets, cat.id) { mutableStateOf(if (limit > 0) "%.0f".format(limit) else "") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color(cat.color).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    CategoryIcon(cat.id, 18.dp, Color(cat.color))
                }
                Column(Modifier.weight(1f)) {
                    Text(cat.name, style = MaterialTheme.typography.titleSmall)
                    Text("$currency${AppState.fmtInt(spent)} spent this month",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (spent > (text.toDoubleOrNull() ?: 0.0) && (text.toDoubleOrNull() ?: 0.0) > 0) Danger else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedNumberField(text, { text = it }, "0", Modifier.width(110.dp))
                TextButton(onClick = { state.setBudget(cat.id, text.toDoubleOrNull() ?: 0.0) }) { Text("Set") }
            }
        }
    }
}

@Composable
fun OutlinedNumberField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = { s -> onChange(s.filter { it.isDigit() || it == '.' }.take(9)) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = modifier.widthInMin(90.dp),
        shape = RoundedCornerShape(12.dp)
    )
}

private fun Modifier.widthInMin(w: androidx.compose.ui.unit.Dp) = this.then(Modifier.width(w))

@Composable
fun HorizontalDividerSoft() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
}

// ============================================================
// REORDER GRID — long-press drag OR arrow buttons; order persists
// ============================================================
@Composable
fun ReorderGrid(state: AppState, initialOrder: List<String>, onDone: () -> Unit) {
    var order by remember { mutableStateOf(initialOrder.toList()) }
    var draggingKey by remember { mutableStateOf<String?>(null) }

    val labels = mapOf(
        "status" to Pair("Status banner", Icons.Default.CheckCircle),
        "stats" to Pair("Stat cards", Icons.Default.TrendingUp),
        "quick" to Pair("Quick add", Icons.Default.Bolt),
        "chart" to Pair("Category chart", Icons.Default.PieChart),
        "transactions" to Pair("Recent transactions", Icons.Default.ReceiptLong),
        "budgets" to Pair("Budgets", Icons.Default.Savings),
        "summary" to Pair("Summary", Icons.Default.DoneAll),
        "insights" to Pair("AI insights", Icons.Default.AutoAwesome)
    )

    fun move(key: String, delta: Int) {
        val cur = order.indexOf(key)
        if (cur < 0) return
        val target = (cur + delta).coerceIn(0, order.size - 1)
        if (target != cur) {
            val mut = order.toMutableList()
            mut.removeAt(cur)
            mut.add(target, key)
            order = mut
            state.setCardOrder(order)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Customise your Home screen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Long-press + drag a card, or use the arrows.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { state.setCardOrder(order); onDone() }) { Text("Done") }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(vertical = 10.dp)
        ) {
            itemsIndexed(order, key = { _, k -> k }) { _, key ->
                val item = labels[key] ?: return@itemsIndexed
                val isDragging = draggingKey == key
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = if (isDragging) 8.dp else 1.dp,
                    shadowElevation = if (isDragging) 12.dp else 2.dp,
                    color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .animateItem()
                        .zIndex(if (isDragging) 10f else 0f)
                        .pointerInput(key) {
                            var accumY = 0f
                            detectDragGesturesAfterLongPress(
                                onDragStart = { accumY = 0f; draggingKey = key },
                                onDragEnd = { draggingKey = null },
                                onDragCancel = { draggingKey = null },
                                onDrag = { change, amount ->
                                    change.consume()
                                    accumY += amount.y
                                    val rowStep = 74.dp.toPx()
                                    val shift = (accumY / rowStep).toInt()
                                    if (shift != 0) {
                                        val cur = order.indexOf(key)
                                        if (cur >= 0) {
                                            val target = (cur + shift * 2).coerceIn(0, order.size - 1)
                                            if (target != cur) {
                                                val mut = order.toMutableList()
                                                mut.removeAt(cur)
                                                mut.add(target, key)
                                                order = mut
                                                state.setCardOrder(order)
                                            }
                                        }
                                        accumY = 0f
                                    }
                                }
                            )
                        }
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(item.second, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(item.first, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.DragIndicator, "Drag", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { move(key, -1) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, "Move up", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { move(key, 1) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, "Move down", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}


/** Consecutive days within the daily target (or zero-spend days when no target is set). */
@Composable
fun StreakCard(state: AppState, exps: List<Expense>) {
    val target = state.ui.collectAsState().value.monthlyTarget
    val streak = remember(exps, target) {
        var count = 0
        var d = java.time.LocalDate.now()
        val todaySpend = exps.filter { it.date == d.toString() }.sumOf { it.amount }
        if (todaySpend > 0) d = d.minusDays(1)
        while (count < 365) {
            val spend = exps.filter { it.date == d.toString() }.sumOf { it.amount }
            val ok = if (target > 0) spend <= target else spend == 0.0
            if (ok) { count++; d = d.minusDays(1) } else break
        }
        count
    }
    if (streak >= 2) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(Color(0x1AF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment, null,
                        tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "$streak-day ${if (target > 0) "under-target" else "no-spend"} streak",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (target > 0) "You stayed within your daily target. Keep the flame alive!"
                        else "Consecutive days with zero spending. Keep it going!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
