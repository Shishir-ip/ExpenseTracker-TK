package com.expensetracker.tk.ui.screens

import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.ui.util.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expensetracker.tk.data.Categories
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.components.CategoryIcon
import com.expensetracker.tk.ui.theme.Danger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(state: AppState, editId: String?, origin: androidx.compose.ui.geometry.Rect? = null, onDismiss: () -> Unit) {
    val existing = editId?.let { state.ui.value.expenses.firstOrNull { e -> e.id == it } }
    val currency = state.prefs.currencySymbol

    var amount by remember { mutableStateOf(existing?.let { if (it.amount % 1.0 == 0.0) "%.0f".format(it.amount) else it.amount.toString() } ?: "") }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "food") }
    var date by remember { mutableStateOf(existing?.let { AppState.parseDate(it.date) } ?: LocalDate.now()) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var recurring by remember { mutableStateOf(existing?.isRecurring ?: false) }
    var recurDays by remember { mutableStateOf(existing?.recurDays ?: 30) }
    var showDate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // container transform: morph from the tapped row into the full screen
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, androidx.compose.animation.core.tween(340, easing = FastOutSlowInEasing)) }
    fun dismissAnimated() {
        scope.launch {
            progress.animateTo(0f, androidx.compose.animation.core.tween(210, easing = FastOutSlowInEasing))
            onDismiss()
        }
    }
    val metrics = LocalContext.current.resources.displayMetrics
    val p = progress.value
    val o = origin

    Surface(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (o != null) {
                    val sw = metrics.widthPixels.toFloat().coerceAtLeast(1f)
                    val sh = metrics.heightPixels.toFloat().coerceAtLeast(1f)
                    transformOrigin = TransformOrigin(
                        ((o.left + o.right) / 2f / sw).coerceIn(0f, 1f),
                        ((o.top + o.bottom) / 2f / sh).coerceIn(0f, 1f)
                    )
                    scaleX = lerp(o.width / sw, 1f, p)
                    scaleY = lerp(o.height / sh, 1f, p)
                    alpha = 0.35f + 0.65f * p
                    shape = RoundedCornerShape(lerp(18f, 0f, p))
                    clip = true
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (existing == null) "Add Expense" else "Edit Expense",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { dismissAnimated() }) { Icon(Icons.Default.Close, "Close") }
            }

            Spacer(Modifier.height(14.dp))

            // amount — big display + custom keypad
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    currency + if (amount.isEmpty()) "0" else amount,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "DEL")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                keys.chunked(3).forEach { rowKeys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowKeys.forEach { k ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .clickable {
                                        amount = when (k) {
                                            "DEL" -> amount.dropLast(1)
                                            "." -> if (amount.contains('.')) amount else if (amount.isEmpty()) "0." else amount + "."
                                            else -> if (amount.replace(".", "").length >= 9) amount
                                                    else if (amount == "0") k else amount + k
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (k == "DEL") {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Backspace, "Delete digit",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Text(
                                        k,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("What did you spend on?") },
                placeholder = { Text("e.g. Lunch at campus") },
                singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
            Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // category grid 4 per row
            Categories.all.chunked(4).forEach { rowCats ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowCats.forEach { c ->
                        val selected = category == c.id
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) Color(c.color).copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { category = c.id }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color(c.color).copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                                CategoryIcon(c.id, 18.dp, Color(c.color))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(c.name, style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color(c.color) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date.toString(), onValueChange = {}, readOnly = true,
                    label = { Text("Date") }, leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                    modifier = Modifier.weight(1f).clickable { showDate = true },
                    enabled = false, shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)") }, placeholder = { Text("Any details…") },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Recurring", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Auto-logs a copy on schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = recurring, onCheckedChange = { recurring = it })
            }

            if (recurring) {
                Spacer(Modifier.height(8.dp))
                Text("Repeats", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Daily", 7 to "Weekly", 30 to "Every 30 days").forEach { (d, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = recurDays == d,
                            onClick = { recurDays = d },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Starting from the date above, a copy is logged automatically each interval (checked once a day).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = Danger, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                existing?.let { e ->
                    Button(
                        onClick = { state.deleteExpense(e.id); dismissAnimated() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Danger),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        when {
                            amt <= 0 -> error = "Enter a valid amount"
                            title.isBlank() -> error = "Give it a title"
                            else -> {
                                val t = title.trim()
                                if (existing == null) {
                                    state.addExpense(t, amt, category, date.toString(), notes.trim(), recurring, recurDays)
                                } else {
                                    state.updateExpense(existing.copy(title = t, amount = amt, category = category, date = date.toString(), notes = notes.trim(), isRecurring = recurring, recurDays = if (recurring) recurDays else 0))
                                }
                                dismissAnimated()
                            }
                        }
                    },
                    modifier = Modifier.weight(2f).height(52.dp)
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (existing == null) "Add expense" else "Save changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDate) {
        val dp = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dp.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    showDate = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = dp) }
    }
}
