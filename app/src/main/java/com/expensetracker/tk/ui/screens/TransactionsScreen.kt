package com.expensetracker.tk.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.expensetracker.tk.ui.components.rememberHaptic
import com.expensetracker.tk.data.SavedFilter
import com.expensetracker.tk.data.SyncState
import com.expensetracker.tk.UndoHost
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.tk.R
import com.expensetracker.tk.data.Categories
import java.time.YearMonth
import com.expensetracker.tk.ui.components.MonthCalendarHeatmap
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChevronLeft
import com.expensetracker.tk.data.Expense
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.components.CategoryIcon
import com.expensetracker.tk.ui.components.EmptyState
import com.expensetracker.tk.ui.components.SectionCard
import com.expensetracker.tk.ui.components.TransactionRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class TxnSort(val label: String) {
    DATE_DESC("Newest first"), DATE_ASC("Oldest first"),
    AMT_DESC("Highest amount"), AMT_ASC("Lowest amount")
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(state: AppState, onEdit: (String, androidx.compose.ui.geometry.Rect?) -> Unit) {
    val currency = state.prefs.currencySymbol
    val prefs = state.prefs
    val exps = state.calcExpenses()

    var search by remember { mutableStateOf("") }
    var selectedCats by remember { mutableStateOf(setOf<String>()) }
    var amtMin by remember { mutableStateOf<Double?>(null) }
    var amtMax by remember { mutableStateOf<Double?>(null) }
    var sort by remember { mutableStateOf(TxnSort.DATE_DESC) }
    var preset by remember { mutableStateOf("all") } // all, month, 30d, year
    var startD by remember { mutableStateOf<LocalDate?>(null) }
    var endD by remember { mutableStateOf<LocalDate?>(null) }
    var bulkMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(true) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf(0) } // 0 list, 1 calendar
    var calMonth by remember { mutableStateOf(YearMonth.now()) }
    var calDay by remember { mutableStateOf<String?>(null) }
    val haptic = rememberHaptic()
    val uiState = state.ui.collectAsState().value
    val wide = LocalConfiguration.current.screenWidthDp >= 700

    val filtered = remember(exps, search, selectedCats, amtMin, amtMax, sort, preset, startD, endD) {
        val lower = search.lowercase()
        val now = LocalDate.now()
        var list = exps.filter { e ->
            val matchSearch = e.title.lowercase().contains(lower) || e.notes.lowercase().contains(lower)
            val matchCat = selectedCats.isEmpty() || e.category in selectedCats
            val matchAmount = (amtMin == null || e.amount >= amtMin!!) && (amtMax == null || e.amount <= amtMax!!)
            val matchDate = when (preset) {
                "month" -> e.date.startsWith(now.toString().substring(0, 7))
                "30d" -> e.date >= now.minusDays(30).toString()
                "year" -> e.date.startsWith(now.toString().substring(0, 4))
                "custom" -> {
                    val s = startD?.toString() ?: "0000"
                    val en = endD?.toString() ?: "9999"
                    e.date >= s && e.date <= en
                }
                else -> true
            }
            matchSearch && matchCat && matchAmount && matchDate
        }
        list = when (sort) {
            TxnSort.DATE_DESC -> list.sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAt })
            TxnSort.DATE_ASC -> list.sortedWith(compareBy<Expense> { it.date }.thenBy { it.createdAt })
            TxnSort.AMT_DESC -> list.sortedByDescending { it.amount }
            TxnSort.AMT_ASC -> list.sortedBy { it.amount }
        }
        list
    }

    val total = filtered.sumOf { it.amount }

    // grouped by day for sticky headers: "Today · ৳340"
    val groups = remember(filtered) { filtered.groupBy { it.date }.toSortedMap(compareByDescending { it }) }
    val todayStr = AppState.todayStr()
    val yesterdayStr = java.time.LocalDate.now().minusDays(1).toString()

    val body: @Composable (Modifier) -> Unit = { bodyMod ->
    Column(bodyMod) {
        // ---------- header ----------
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (bulkMode) "${selected.size} selected" else "Transactions",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("${filtered.size} txns · $currency${AppState.fmtInt(total)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (bulkMode) {
                TextButton(onClick = { selected = filtered.map { it.id }.toSet() }) { Text("All") }
                TextButton(onClick = { selected = emptySet() }) { Text("None") }
                TextButton(onClick = {
                    if (selected.isNotEmpty()) {
                        haptic()
                        val ids = selected.toList()
                        val count = ids.size
                        val stash = state.stashExpensesForDelete(ids)
                        if (stash.isNotEmpty()) {
                            UndoHost.post("Deleted $count transactions",
                                undo = { state.undoBulkDelete(stash) },
                                onDismissed = { state.finalizeBulkDelete(stash) })
                        }
                        selected = emptySet(); bulkMode = false
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                TextButton(onClick = { bulkMode = false; selected = emptySet() }) { Text("Done") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = viewMode == 0,
                        onClick = { viewMode = 0 },
                        label = { Text("List", maxLines = 1) }
                    )
                    FilterChip(
                        selected = viewMode == 1,
                        onClick = { viewMode = 1; bulkMode = false },
                        label = { Text("Calendar", maxLines = 1) }
                    )
                }
                IconButton(onClick = { bulkMode = true }, enabled = viewMode == 0) {
                    Icon(Icons.Default.Checklist, "Bulk select", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ---------- search ----------
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search transactions…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (search.isNotEmpty()) IconButton({ search = "" }) { Icon(Icons.Default.Close, null) } },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        // ---------- filter chips ----------
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(selected = preset == "all", onClick = { preset = "all" }, label = { Text("All") })
            FilterChip(selected = preset == "month", onClick = { preset = "month" }, label = { Text("Month") })
            FilterChip(selected = preset == "30d", onClick = { preset = "30d" }, label = { Text("30d") })
            FilterChip(selected = preset == "year", onClick = { preset = "year" }, label = { Text("Year") })
            FilterChip(
                selected = preset == "custom",
                onClick = { pickingStart = true; showDatePicker = true },
                label = { Text(if (preset == "custom" && startD != null) "${startD!!} → ${endD ?: "…"}" else "Custom") }
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // multi-category filter dropdown
            var catMenu by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = selectedCats.isNotEmpty(),
                    onClick = { catMenu = true },
                    label = {
                        Text(
                            if (selectedCats.isEmpty()) "Category"
                            else if (selectedCats.size == 1) Categories.byId(selectedCats.first()).name
                            else Categories.byId(selectedCats.first()).name + " +" + (selectedCats.size - 1)
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.DoneAll, null, Modifier.size(14.dp)) }
                )
                DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("All categories", fontWeight = FontWeight.Bold) },
                        onClick = { selectedCats = emptySet(); catMenu = false }
                    )
                    Categories.all.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.name) },
                            leadingIcon = {
                                Checkbox(checked = c.id in selectedCats, onCheckedChange = null)
                            },
                            onClick = {
                                selectedCats = if (c.id in selectedCats) selectedCats - c.id else selectedCats + c.id
                            }
                        )
                    }
                }
            }
            // amount range
            var amtDialog by remember { mutableStateOf(false) }
            FilterChip(
                selected = amtMin != null || amtMax != null,
                onClick = { amtDialog = true },
                label = {
                    Text(
                        when {
                            amtMin != null && amtMax != null -> "৳${AppState.fmtInt(amtMin!!)}–${AppState.fmtInt(amtMax!!)}"
                            amtMin != null -> "৳${AppState.fmtInt(amtMin!!)}+"
                            amtMax != null -> "≤ ৳${AppState.fmtInt(amtMax!!)}"
                            else -> "Amount"
                        }
                    )
                }
            )
            if (amtDialog) {
                var minText by remember { mutableStateOf(amtMin?.let { if (it % 1.0 == 0.0) "%.0f".format(it) else it.toString() } ?: "") }
                var maxText by remember { mutableStateOf(amtMax?.let { if (it % 1.0 == 0.0) "%.0f".format(it) else it.toString() } ?: "") }
                AlertDialog(
                    onDismissRequest = { amtDialog = false },
                    title = { Text("Amount range", fontWeight = FontWeight.Bold) },
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = minText, onValueChange = { minText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) },
                                label = { Text("Min $currency") }, singleLine = true, modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = maxText, onValueChange = { maxText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) },
                                label = { Text("Max $currency") }, singleLine = true, modifier = Modifier.weight(1f)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            amtMin = minText.toDoubleOrNull()
                            amtMax = maxText.toDoubleOrNull()
                            amtDialog = false
                        }) { Text("Apply") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            amtMin = null; amtMax = null; amtDialog = false
                        }) { Text("Clear") }
                    }
                )
            }
            // sort dropdown
            var sortMenu by remember { mutableStateOf(false) }
            Box {
                FilterChip(selected = false, onClick = { sortMenu = true }, label = { Text("⇅ ${sort.label}") })
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    TxnSort.entries.forEach { s ->
                        DropdownMenuItem(text = { Text(s.label, fontWeight = if (s == sort) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { sort = s; sortMenu = false })
                    }
                }
            }
        }

        // ---------- saved filters ----------
        val savedList = remember(prefs) { prefs.getSavedFilterList() }
        if (savedList.isNotEmpty() || selectedCats.isNotEmpty() || amtMin != null || amtMax != null) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                savedList.forEach { f ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable {
                                selectedCats = f.cats.toSet(); amtMin = f.min; amtMax = f.max
                            }
                            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(f.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = {
                            prefs.setSavedFilterList(savedList.filter { it.name != f.name })
                        }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(12.dp))
                        }
                    }
                }
                if (selectedCats.isNotEmpty() || amtMin != null || amtMax != null) {
                    var saveDialog by remember { mutableStateOf(false) }
                    FilterChip(
                        selected = false,
                        onClick = { saveDialog = true },
                        label = { Text("Save filter") },
                        leadingIcon = { Icon(Icons.Default.DoneAll, null, Modifier.size(13.dp)) }
                    )
                    if (saveDialog) {
                        var name by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { saveDialog = false },
                            title = { Text("Save filter", fontWeight = FontWeight.Bold) },
                            text = {
                                OutlinedTextField(
                                    value = name, onValueChange = { name = it },
                                    label = { Text("Filter name") }, singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (name.isNotBlank()) {
                                        prefs.setSavedFilterList(
                                            savedList + SavedFilter(name.trim(), selectedCats.toList(), amtMin, amtMax)
                                        )
                                    }
                                    saveDialog = false
                                }) { Text("Save") }
                            },
                            dismissButton = { TextButton(onClick = { saveDialog = false }) { Text("Cancel") } }
                        )
                    }
                }
            }
        }

        // ---------- list ----------
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (viewMode == 1) {
                // ---------- calendar month view ----------
                item {
                    val current = YearMonth.now()
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { calMonth = calMonth.minusMonths(1) }, enabled = calMonth.isAfter(YearMonth.of(2015, 1))) {
                                Icon(Icons.Default.ChevronLeft, "Previous month")
                            }
                            Text(
                                "${AppState.MONTHS[calMonth.monthValue - 1]} ${calMonth.year}",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { calMonth = calMonth.plusMonths(1) }, enabled = calMonth.isBefore(current)) {
                                Icon(Icons.Default.ChevronRight, "Next month")
                            }
                        }
                        val monthStr = "%04d-%02d".format(calMonth.year, calMonth.monthValue)
                        val dayTotals = remember(exps, calMonth) {
                            exps.filter { it.date.startsWith(monthStr) }
                                .groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.amount } }
                        }
                        MonthCalendarHeatmap(dayTotals, calMonth.year, calMonth.monthValue, currency,
                            onDayClick = { d -> calDay = if (calDay == d) null else d })
                    }
                }
                calDay?.let { dstr ->
                    item {
                        val dayExps = exps.filter { it.date == dstr }
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
                                    Text(
                                        "$currency${AppState.fmtInt(dayExps.sumOf { it.amount })} · ${dayExps.size} transactions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { calDay = null }) { Text("Close") }
                            }
                            Spacer(Modifier.height(6.dp))
                            if (dayExps.isEmpty()) {
                                Text("No expenses that day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    dayExps.sortedByDescending { it.amount }.forEach { e ->
                                        TransactionRow(e, currency, onClick = { onEdit(e.id, null) }, onDelete = {
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
                }
                item { Spacer(Modifier.height(80.dp)) }
            } else if (filtered.isEmpty()) {
                item {
                    SectionCard { EmptyState(Icons.Default.Search, "No transactions found", "Try changing the search or filters", illustration = R.drawable.ill_wallet) }
                }
            } else {
                groups.forEach { (date, dayItems) ->
                    stickyHeader(key = "h_" + date) {
                        val dayTotal = dayItems.sumOf { it.amount }
                        val label = when (date) {
                            todayStr -> "Today"
                            yesterdayStr -> "Yesterday"
                            else -> try {
                                val d = AppState.parseDate(date)
                                "${d.dayOfMonth} ${AppState.MONTHS[d.monthValue - 1].take(3)} ${d.year}"
                            } catch (e: Exception) { date }
                        }
                        Surface(color = MaterialTheme.colorScheme.background) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                                Spacer(Modifier.width(8.dp))
                                Text("$currency${AppState.fmtInt(dayTotal)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    items(dayItems.size, key = { dayItems[it].id }) { i ->
                        val e = dayItems[i]
                        var rowBounds by remember(e.id) { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (bulkMode) {
                                Checkbox(
                                    checked = e.id in selected,
                                    onCheckedChange = { on ->
                                        selected = if (on) selected + e.id else selected - e.id
                                    }
                                )
                            }
                            TransactionRow(
                                e, currency,
                                onBoundsChange = { rowBounds = it },
                                onClick = {
                                    if (bulkMode) { selected = if (e.id in selected) selected - e.id else selected + e.id }
                                    else if (wide) selectedId = e.id
                                    else onEdit(e.id, rowBounds)
                                },
                                onDelete = {
                                    val stash = state.stashExpenseForDelete(e.id)
                                    if (stash != null) {
                                        UndoHost.post("Deleted " + stash.title.take(24),
                                            undo = { state.undoDeleteExpense(stash) },
                                            onDismissed = { state.finalizeExpenseDelete(stash) })
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                // bulk recategorize bar
                if (bulkMode && selected.isNotEmpty()) {
                    item {
                        SectionCard {
                            Text("Change category of ${selected.size} selected", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Categories.all.take(4).forEach { c ->
                                    FilterChip(selected = false, onClick = {
                                        state.bulkRecategorize(selected.toList(), c.id)
                                        selected = emptySet(); bulkMode = false
                                    }, label = { Text(c.name) })
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Categories.all.drop(4).forEach { c ->
                                    FilterChip(selected = false, onClick = {
                                        state.bulkRecategorize(selected.toList(), c.id)
                                        selected = emptySet(); bulkMode = false
                                    }, label = { Text(c.name) })
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
    } // body lambda

    PullToRefreshBox(
        isRefreshing = uiState.sync == SyncState.SYNCING,
        onRefresh = { state.manualSync() },
        modifier = Modifier.fillMaxSize()
    ) {
        if (wide && !bulkMode) {
            Row(Modifier.fillMaxSize()) {
                body(Modifier.weight(1.35f))
                selectedId?.let { sid ->
                    val sel = filtered.firstOrNull { it.id == sid }
                    if (sel == null) {
                        selectedId = null
                    } else {
                        TxnDetailPane(
                            expense = sel,
                            currency = currency,
                            onEdit = { onEdit(sel.id, null) },
                            onDelete = {
                                val stash = state.stashExpenseForDelete(sel.id)
                                if (stash != null) {
                                    UndoHost.post("Deleted " + stash.title.take(24),
                                        undo = { state.undoDeleteExpense(stash) },
                                        onDismissed = { state.finalizeExpenseDelete(stash) })
                                }
                                selectedId = null
                            },
                            onClose = { selectedId = null },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } ?: SectionCard(Modifier.weight(1f).padding(16.dp)) {
                    EmptyState(Icons.AutoMirrored.Filled.ReceiptLong, "Select a transaction", "Tap an entry to see its details here")
                }
            }
        } else {
            body(Modifier.fillMaxSize())
        }
    }

    // ---------- custom date range picker ----------
    if (showDatePicker) {
        val dpState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val ms = dpState.selectedDateMillis
                    if (ms != null) {
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                        if (pickingStart) {
                            startD = d
                            pickingStart = false  // dialog stays open to pick the end date
                        } else {
                            endD = d
                            preset = "custom"
                            showDatePicker = false
                        }
                    } else {
                        showDatePicker = false
                    }
                }) { Text(if (pickingStart) "Set start" else "Set end") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = dpState)
        }
    }
}


/** Right-hand detail pane shown next to the history list on wide screens. */
@Composable
fun TxnDetailPane(
    expense: Expense,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
        }
        Spacer(Modifier.height(6.dp))
        val cat = Categories.byId(expense.category)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(cat.color).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                CategoryIcon(expense.category, 22.dp, Color(cat.color))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("-$currency${AppState.fmtAmount(expense.amount)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        DetailRow("Date", expense.date)
        DetailRow("Category", cat.name)
        if (expense.notes.isNotBlank()) DetailRow("Notes", expense.notes)
        DetailRow("Recurring", if (expense.isRecurring) "Yes" else "No")
        DetailRow("Sync", if (expense.dirty) "Pending upload" else "Synced")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete")
            }
            Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Text("Edit transaction", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(90.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
