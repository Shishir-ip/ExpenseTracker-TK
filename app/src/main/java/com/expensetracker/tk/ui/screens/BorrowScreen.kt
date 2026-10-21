package com.expensetracker.tk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expensetracker.tk.UndoHost
import com.expensetracker.tk.R
import com.expensetracker.tk.data.BorrowLend
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.components.EmptyState
import com.expensetracker.tk.ui.components.SectionCard
import com.expensetracker.tk.ui.components.rememberHaptic
import com.expensetracker.tk.ui.theme.Danger
import com.expensetracker.tk.ui.theme.Success
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowScreen(state: AppState) {
    val ui = state.ui.collectAsState().value
    val currency = state.prefs.currencySymbol
    val haptic = rememberHaptic()
    var showAdd by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(0) } // 0 all, 1 pending, 2 settled
    var search by remember { mutableStateOf("") }
    var personFilter by remember { mutableStateOf<String?>(null) }
    var actionId by remember { mutableStateOf<String?>(null) }

    val owedToMe = ui.borrows.filter { it.type == "lend" && it.status == "pending" }.sumOf { it.amount }
    val iOwe = ui.borrows.filter { it.type == "borrow" && it.status == "pending" }.sumOf { it.amount }
    val net = owedToMe - iOwe

    data class PersonBalance(val name: String, val owedToMe: Double, val iOwe: Double)
    val people = remember(ui.borrows) {
        ui.borrows.filter { it.status == "pending" }
            .groupBy { it.personName.trim() }
            .map { (name, list) ->
                PersonBalance(
                    name = name,
                    owedToMe = list.filter { it.type == "lend" }.sumOf { it.amount },
                    iOwe = list.filter { it.type == "borrow" }.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.owedToMe + it.iOwe }
    }

    val lower = search.lowercase()
    val list = ui.borrows.filter { b ->
        val matchSearch = b.personName.lowercase().contains(lower) || b.notes.lowercase().contains(lower)
        val matchStatus = when (filter) { 1 -> b.status == "pending"; 2 -> b.status == "settled"; else -> true }
        val matchPerson = personFilter == null || b.personName.trim().equals(personFilter, ignoreCase = true)
        matchSearch && matchStatus && matchPerson
    }

    Column(Modifier.fillMaxSize()) {
        // ---------- header ----------
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4)))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Handshake, null, tint = Color.White) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Borrow & Lend", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("Track money between you and others", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---------- summary ----------
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryTile(Modifier.weight(1f), "You are owed", owedToMe, currency, Success)
            SummaryTile(Modifier.weight(1f), "You owe", iOwe, currency, Danger)
        }
        SectionCard(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Net balance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(
                    "${if (net >= 0) "+" else ""}$currency${AppState.fmtAmount(net)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (net >= 0) Success else Danger
                )
            }
        }

        // ---------- search ----------
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search people or notes…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (search.isNotEmpty()) IconButton({ search = "" }) { Icon(Icons.Default.Close, null) } },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp)
        )

        // ---------- per-person balances ----------
        if (people.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = personFilter == null,
                    onClick = { personFilter = null },
                    label = { Text("Everyone") }
                )
                people.forEach { p ->
                    val netP = p.owedToMe - p.iOwe
                    FilterChip(
                        selected = personFilter == p.name,
                        onClick = { personFilter = if (personFilter == p.name) null else p.name },
                        label = {
                            Text(
                                (if (netP >= 0) "▲ " else "▼ ") + p.name + " " + currency + AppState.fmtInt(kotlin.math.abs(netP)),
                                color = if (netP >= 0) Success else Danger,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }
            }
        }

        // ---------- status filters + add ----------
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(selected = filter == 0, onClick = { filter = 0 }, label = { Text("All", maxLines = 1) })
            FilterChip(selected = filter == 1, onClick = { filter = 1 }, label = { Text("Pending", maxLines = 1) })
            FilterChip(selected = filter == 2, onClick = { filter = 2 }, label = { Text("Settled", maxLines = 1) })
            Spacer(Modifier.width(2.dp))
            Button(onClick = { showAdd = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)) {
                Text("+ New", maxLines = 1, softWrap = false)
            }
        }

        // ---------- list ----------
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (list.isEmpty()) {
                item {
                    SectionCard { EmptyState(Icons.Default.Handshake, "No records", "Add your first borrow or lend entry", illustration = R.drawable.ill_people) }
                }
            } else {
                items(list.size, key = { list[it].id }) { i ->
                    val b = list[i]
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { v ->
                            if (v == SwipeToDismissBoxValue.EndToStart) {
                                val stash = state.stashBorrowForDelete(b.id)
                                if (stash != null) {
                                    UndoHost.post("Deleted " + stash.personName.take(24),
                                        undo = { state.undoDeleteBorrow(stash) },
                                        onDismissed = { state.finalizeBorrowDelete(stash) })
                                }
                                true
                            } else false
                        },
                        positionalThreshold = { total -> total * 0.62f }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            var bgW by remember { mutableStateOf(1f) }
                            val off = runCatching { dismissState.requireOffset() }.getOrDefault(0f)
                            val progress = (kotlin.math.abs(off) / (bgW * 0.62f)).coerceIn(0f, 1f)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { bgW = it.width.toFloat().coerceAtLeast(1f) }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Danger.copy(alpha = progress))
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete, "Delete",
                                    tint = Color.White.copy(alpha = progress),
                                    modifier = Modifier
                                        .size((18 + 8 * progress).dp)
                                        .graphicsLayer { scaleX = 0.8f + 0.4f * progress; scaleY = 0.8f + 0.4f * progress }
                                )
                            }
                        }
                    ) {
                        BorrowRow(b, currency, onClick = { haptic(); actionId = b.id })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { showAdd = false }) {
            AddBorrowSheet(state, currency) { showAdd = false }
        }
    }

    // ---------- settle / reopen / delete sheet ----------
    actionId?.let { id ->
        val b = ui.borrows.firstOrNull { it.id == id }
        if (b == null) {
            actionId = null
        } else {
            ModalBottomSheet(onDismissRequest = { actionId = null }) {
                BorrowActionSheet(
                    b = b,
                    currency = currency,
                    onSettle = { amount ->
                        state.settleBorrow(b.id, amount)
                        actionId = null
                    },
                    onReopen = {
                        state.reopenBorrow(b.id)
                        actionId = null
                    },
                    onDelete = {
                        val stash = state.stashBorrowForDelete(b.id)
                        if (stash != null) {
                            UndoHost.post("Deleted " + stash.personName.take(24),
                                undo = { state.undoDeleteBorrow(stash) },
                                onDismissed = { state.finalizeBorrowDelete(stash) })
                        }
                        actionId = null
                    },
                    onClose = { actionId = null }
                )
            }
        }
    }
}

@Composable
fun SummaryTile(modifier: Modifier, label: String, amount: Double, currency: String, color: Color) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(14.dp)
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(4.dp))
        Text("$currency${AppState.fmtInt(amount)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
fun BorrowRow(b: BorrowLend, currency: String, onClick: () -> Unit) {
    val isLend = b.type == "lend"
    val accent = if (isLend) Success else Danger
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                androidx.compose.ui.graphics.lerp(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceVariant,
                    0.55f
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // person avatar with type badge
        val avatarColors = listOf(0xFF6366F1, 0xFF8B5CF6, 0xFF06B6D4, 0xFF10B981, 0xFFF59E0B, 0xFFEC4899, 0xFFEF4444)
        val avatarColor = Color(avatarColors[kotlin.math.abs(b.personName.hashCode()) % avatarColors.size])
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    b.personName.trim().firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = avatarColor
                )
            }
            // small arrow badge bottom-end
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isLend) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    null, tint = Color.White, modifier = Modifier.size(10.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(b.personName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (b.status == "settled") {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.CheckCircle, "Settled", Modifier.size(14.dp), tint = Success)
                }
            }
            Text(
                buildString {
                    append(if (isLend) "Lent on " else "Borrowed on ")
                    append(b.date)
                    b.reminderDate?.let { append(" · reminder $it") }
                    if (b.notes.isNotBlank()) append(" · ${b.notes}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${if (isLend) "+" else "-"}$currency${AppState.fmtAmount(b.amount)}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = accent
            )
            Text(if (b.status == "pending") "tap for options" else "settled · tap to reopen",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowActionSheet(
    b: BorrowLend,
    currency: String,
    onSettle: (Double) -> Unit,
    onReopen: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val isLend = b.type == "lend"
    var amount by remember(b.id) {
        mutableStateOf(if (b.amount % 1.0 == 0.0) "%.0f".format(b.amount) else b.amount.toString())
    }
    val inputAmount = amount.toDoubleOrNull() ?: 0.0

    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(b.personName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    (if (isLend) "You lent " else "You borrowed ") + "$currency${AppState.fmtAmount(b.amount)}" +
                            if (b.status == "pending") " · pending" else " · settled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
        }

        if (b.status == "pending") {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) },
                label = { Text("Settle amount ($currency)") },
                supportingText = { Text("Full amount settles the record · a partial amount is deducted and noted") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSettle(inputAmount) },
                enabled = inputAmount > 0,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    if (inputAmount >= b.amount - 0.005) "Settle fully" else "Settle partial",
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(onClick = onReopen, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Reopen (mark pending)", fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Delete record")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBorrowSheet(state: AppState, currency: String, onDone: () -> Unit) {
    var type by remember { mutableStateOf("lend") }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var reminder by remember { mutableStateOf<LocalDate?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("New Record", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = type == "lend", onClick = { type = "lend" },
                label = { Text("↑ I lent money") })
            FilterChip(selected = type == "borrow", onClick = { type = "borrow" },
                label = { Text("↓ I borrowed") })
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Person") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(
            value = amount, onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(10) },
            label = { Text("Amount ($currency)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = date.toString(), onValueChange = {}, readOnly = true, label = { Text("Date") },
                modifier = Modifier.weight(1f).clickable { showDate = true }, shape = RoundedCornerShape(12.dp), enabled = false,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            OutlinedTextField(
                value = reminder?.toString() ?: "", onValueChange = {}, readOnly = true,
                label = { Text("Reminder (opt.)") }, placeholder = { Text("none") },
                modifier = Modifier.weight(1f).clickable { showReminder = true }, shape = RoundedCornerShape(12.dp), enabled = false,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Button(
            onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && amt > 0) {
                    state.addBorrow(type, name.trim(), amt, date.toString(), reminder?.toString(), notes.trim())
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0
        ) { Text("Save record", fontWeight = FontWeight.Bold) }
    }

    if (showDate) {
        val dp = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showDate = false },
            confirmButton = { TextButton(onClick = {
                dp.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                showDate = false
            }) { Text("OK") } }) { DatePicker(state = dp) }
    }
    if (showReminder) {
        val dp = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showReminder = false },
            confirmButton = { TextButton(onClick = {
                dp.selectedDateMillis?.let { reminder = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                showReminder = false
            }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { reminder = null; showReminder = false }) { Text("None") } }) { DatePicker(state = dp) }
    }
}
