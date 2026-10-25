package com.expensetracker.tk.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.tk.data.Categories
import com.expensetracker.tk.data.OpenRouter
import com.expensetracker.tk.data.QuickPreset
import com.expensetracker.tk.data.SupabaseRest
import com.expensetracker.tk.data.SyncState
import com.expensetracker.tk.notify.Notify
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.theme.AiGlow
import com.expensetracker.tk.ui.theme.Danger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: AppState, onLockChanged: (Boolean) -> Unit) {
    val ui = state.ui.collectAsState().value
    val prefs = state.prefs
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showTrash by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---------- profile header ----------
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF4338CA), Color(0xFF8B5CF6), Color(0xFF06B6D4))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.expensetracker.tk.R.drawable.ic_splash),
                    contentDescription = null,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Expense Tracker TK", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    "v1.7.0 · local-first",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                com.expensetracker.tk.ui.components.SyncBadge(ui.sync, ui.pending)
                Spacer(Modifier.height(4.dp))
                val dbSizeKb = remember {
                    try { context.getDatabasePath("tracker.db").length() / 1024 } catch (e: Exception) { 0 }
                }
                Text(
                    "Local data: ${if (dbSizeKb > 1024) "%.1f MB".format(dbSizeKb / 1024f) else "$dbSizeKb KB"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)

        // ---------- recurring templates manager ----------
        SectionCard2("Recurring templates", Icons.Default.Autorenew, MaterialTheme.colorScheme.primary) {
            val templates = ui.expenses.filter { it.isRecurring && it.recurDays != 0 && it.recurParent == null }
            if (templates.isEmpty()) {
                Text(
                    "No recurring templates yet. Mark an expense as recurring (Daily / Weekly / 30 days) when adding it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    templates.forEach { t ->
                        val step = kotlin.math.abs(t.recurDays)
                        val paused = t.recurDays < 0
                        val start = AppState.parseDate(t.date)
                        var next = start
                        if (!paused) {
                            while (!next.isAfter(java.time.LocalDate.now())) next = next.plusDays(step.toLong())
                        }
                        val intervalLabel = when (step) {
                            1 -> "Daily"
                            7 -> "Weekly"
                            else -> "Every 30 days"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    if (paused) "$intervalLabel · paused"
                                    else "$intervalLabel · next: ${next.dayOfMonth} ${AppState.MONTHS[next.monthValue - 1].take(3)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                state.prefs.currencySymbol + AppState.fmtInt(t.amount),
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold
                            )
                            TextButton(onClick = {
                                state.updateExpense(t.copy(recurDays = if (paused) step else -step))
                            }) { Text(if (paused) "Resume" else "Pause") }
                        }
                    }
                }
            }
        }

        SectionLabel("Cloud & AI")

        // ================= CLOUD (SUPABASE) =================
        CloudSection(state)

        // ================= AI (OPENROUTER) =================
        AiSection(state)

        // ================= SQL SETUP GUIDE =================
        SqlGuideSection()

        SectionLabel("Looks & feel")

        // ================= APPEARANCE =================
        SectionCard2("Appearance", Icons.Default.Palette, MaterialTheme.colorScheme.primary) {
            Text("Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "System", 1 to "Light", 2 to "Dark", 3 to "Dynamic", 4 to "Black AMOLED").forEach { (v, label) ->
                    FilterChip(selected = prefs.themeMode == v, onClick = { state.setTheme(v) }, label = { Text(label) })
                }
            }
            if (prefs.themeMode == 3 && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                Text("Dynamic colour needs Android 12+ — falling back to system theme.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Text("Accent colour", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.expensetracker.tk.ui.theme.ACCENTS.forEachIndexed { i, a ->
                    val selected = prefs.accentIndex == i
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(a.seed))
                            .border(
                                if (selected) 3.dp else 0.dp,
                                MaterialTheme.colorScheme.onSurface,
                                androidx.compose.foundation.shape.CircleShape
                            )
                            .clickable { state.setAccent(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Icon(Icons.Default.Check, null, tint = Color.White)
                    }
                }
            }
            Text("Re-tints buttons, chips, the FAB and navigation everywhere.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("Currency symbol", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("৳" to "Taka", "$" to "Dollar", "€" to "Euro", "₹" to "Rupee", "£" to "Pound").forEach { (sym, label) ->
                    FilterChip(selected = prefs.currencySymbol == sym,
                        onClick = {
                            prefs.currencySymbol = sym
                            state.reload()
                        },
                        label = { Text("$sym  ($label)") })
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Include Bills in totals", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Same toggle as the top bar on Home", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = ui.includeBills, onCheckedChange = { state.toggleBills() })
            }
        }

        // ================= HOME LAYOUT =================
        SectionCard2("Home Screen", Icons.Default.Reorder, MaterialTheme.colorScheme.primary) {
            Text(
                "Use the reorder button (⇅ icon, top-right of Home) to drag cards into your own order — the layout is saved. " +
                        "Cards: status banner, stat cards, quick add, chart, transactions, budgets, summary, AI insights.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ================= QUICK PRESETS =================
        PresetsSection(state)

        SectionLabel("Alerts")

        // ================= NOTIFICATIONS =================
        NotificationSection(state)

        SectionLabel("Data & safety")

        // ================= DATA: EXPORT / IMPORT / SYNC =================
        DataSection(state)

        SectionLabel("Data & safety")

        // ---------- trash (deleted items kept 30 days) ----------
        SectionCard2("Trash", Icons.Default.AutoDelete, Danger) {
            val trashCount = remember(ui.reloadKey) { state.trashItems().size }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Deleted items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (trashCount == 0) "Nothing in the trash"
                        else "$trashCount item(s) · kept 30 days, then removed permanently",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showTrash = true }, enabled = trashCount > 0) { Text("Open") }
            }
        }

        // ================= ABOUT =================
        SectionCard2("About", Icons.Default.Description, MaterialTheme.colorScheme.primary) {
            Text("Expense Tracker TK — Android", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "v1.5.0 · local-first · offline sync to Supabase · AI via OpenRouter. " +
                        "Data lives on this device in an encrypted-by-OS SQLite database and mirrors your cloud tables.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { state.setOnboarded(false) }) {
                Text("Replay welcome setup")
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Text("Developed by SHISHIR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "github.com/Shishir-ip",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable {
                        try {
                            context.startActivity(
                                android.content.Intent(android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/Shishir-ip"))
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) { /* no browser */ }
                    }
            )
        }
    }

    if (showTrash) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showTrash = false }) {
            TrashSheet(state, onClose = { showTrash = false })
        }
    }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

// ============================================================
@Composable
fun SectionCard2(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

// ============================================================
// CLOUD
// ============================================================
@Composable
fun CloudSection(state: AppState) {
    val prefs = state.prefs
    val ui = state.ui.collectAsState().value
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(prefs.supabaseUrl) }
    var key by remember { mutableStateOf(prefs.supabaseKey) }
    var userKey by remember { mutableStateOf(prefs.userKey) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var resultOk by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    SectionCard2("Cloud · Supabase", Icons.Default.Cloud, MaterialTheme.colorScheme.primary) {
        Text(
            "Paste your project URL and anon key once — they're stored on this device and used for every sync. " +
                    "Works fully offline; changes upload automatically when internet returns.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = url, onValueChange = { url = it; saved = false },
            label = { Text("Supabase project URL") },
            placeholder = { Text("https://xxxx.supabase.co") },
            singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = key, onValueChange = { key = it; saved = false },
            label = { Text("Anon key (public)") },
            placeholder = { Text("eyJhbGciOi…") },
            singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = userKey, onValueChange = { userKey = it; saved = false },
            label = { Text("Profile key (user_pin value)") },
            supportingText = { Text("Rows with this user_pin value are yours. If you used the web app with a PIN, enter that same PIN here. Default: main") },
            singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                prefs.supabaseUrl = url
                prefs.supabaseKey = key
                prefs.userKey = userKey
                saved = true
                result = null
                scope.launch {
                    testing = true
                    try {
                        withContext(Dispatchers.IO) {
                            val client = SupabaseRest(prefs.supabaseUrl, prefs.supabaseKey)
                            client.testConnection()
                        }
                        resultOk = true
                        result = "Connected! Tables reachable."
                        state.manualSync()
                    } catch (e: Exception) {
                        resultOk = false
                        result = "Failed: ${e.message?.take(200)}"
                    } finally { testing = false }
                }
            }) {
                Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save & test")
            }
            if (testing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        if (saved && result == null) {
            Text("Saved on this device ✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        result?.let {
            Text(it, color = if (resultOk) com.expensetracker.tk.ui.theme.Success else Danger, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.CloudSync, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            val last = prefs.lastSync
            Text(
                if (last > 0) "Last sync: ${java.time.Instant.ofEpochMilli(last).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))}"
                else "Never synced yet",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { state.manualSync() }) { Text("Sync now") }
        }
        if (ui.sync == SyncState.ERROR) ui.syncError?.let {
            Text("Sync error: $it", color = Danger, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
        }
    }
}

// ============================================================
// AI
// ============================================================
@Composable
fun AiSection(state: AppState) {
    val prefs = state.prefs
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf(prefs.openRouterKey) }
    var model by remember { mutableStateOf(prefs.openRouterModel) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var resultOk by remember { mutableStateOf(false) }

    SectionCard2("AI · OpenRouter", Icons.Default.Biotech, AiGlow) {
        Text(
            "Used for AI insights and the chat assistant. Create a free key at openrouter.ai → Keys. " +
                    "The key is saved on this device only.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = key, onValueChange = { key = it },
            label = { Text("OpenRouter API key") },
            placeholder = { Text("sk-or-v1-…") },
            singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = model, onValueChange = { model = it },
            label = { Text("Model") },
            supportingText = { Text("Any OpenRouter model id, e.g. openrouter/auto, google/gemini-2.0-flash-exp:free, meta-llama/llama-3.3-70b-instruct:free") },
            singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                prefs.openRouterKey = key
                prefs.openRouterModel = model
                scope.launch {
                    testing = true; result = null
                    try {
                        val reply = withContext(Dispatchers.IO) {
                            OpenRouter(prefs.openRouterKey, prefs.openRouterModel).testKey()
                        }
                        resultOk = true
                        result = "AI replied: \"${reply.take(60)}\""
                    } catch (e: Exception) {
                        resultOk = false
                        result = "Failed: ${e.message?.take(200)}"
                    } finally { testing = false }
                }
            }) {
                Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save & test")
            }
            if (testing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = AiGlow)
        }
        result?.let { Text(it, color = if (resultOk) com.expensetracker.tk.ui.theme.Success else Danger, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp) }
    }
}

// ============================================================
// SQL GUIDE
// ============================================================
const val SQL_SETUP = """-- Run this ONCE in Supabase → SQL Editor → New query.
-- It creates the same three tables your web app uses.

-- 1) EXPENSES
create table if not exists public.expenses (
  id text primary key,
  user_pin text not null,
  title text not null,
  amount numeric not null default 0,
  category text not null default 'other',
  date date not null default current_date,
  notes text default '',
  is_recurring boolean default false,
  updated_at bigint default 0,
  created_at bigint default 0
);

-- 2) BUDGETS  (category budgets + '__daily_limit__' + '__monthly_target__')
create table if not exists public.budgets (
  id bigint generated always as identity primary key,
  user_pin text not null,
  category text not null,
  amount numeric not null default 0,
  year_month text default ''
);

-- 3) BORROW / LEND
create table if not exists public.borrow_lend (
  id text primary key,
  user_pin text not null,
  type text not null check (type in ('borrow','lend')),
  person_name text not null,
  amount numeric not null default 0,
  date date not null default current_date,
  reminder_date date,
  status text default 'pending' check (status in ('pending','settled')),
  notes text default '',
  updated_at bigint default 0
);

-- Row Level Security: the anon key may only touch rows of the app's profile key.
alter table public.expenses    enable row level security;
alter table public.budgets     enable row level security;
alter table public.borrow_lend enable row level security;

create policy "expenses_all"   on public.expenses    for all using (true) with check (true);
create policy "budgets_all"    on public.budgets     for all using (true) with check (true);
create policy "borrow_all"     on public.borrow_lend for all using (true) with check (true);

-- Indexes for speed
create index if not exists expenses_user_date_idx  on public.expenses (user_pin, date desc);
create index if not exists budgets_user_idx        on public.budgets (user_pin);
create index if not exists borrow_user_idx         on public.borrow_lend (user_pin, date desc);
"""

@Composable
fun SqlGuideSection() {
    val clipboard = LocalClipboardManager.current
    var open by remember { mutableStateOf(false) }

    SectionCard2("Database setup (one-time)", Icons.Default.Code, com.expensetracker.tk.ui.theme.Cyan) {
        Text(
            "Your Supabase project needs three tables: expenses, budgets, borrow_lend. " +
                    "Tap below to copy the full SQL, run it once in Supabase → SQL Editor, then paste your URL + anon key above. " +
                    "Already used the web app? Your data appears automatically — just set the Profile key to your web PIN.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                clipboard.setText(AnnotatedString(SQL_SETUP))
                open = true
            }) {
                Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy full SQL")
            }
            OutlinedButton(onClick = { open = true }) { Text("View SQL") }
        }
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } },
            title = { Text("SQL setup", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                    Text(
                        SQL_SETUP,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        )
    }
}

// ============================================================
// PRESETS
// ============================================================
@Composable
fun PresetsSection(state: AppState) {
    val ui = state.ui.collectAsState().value
    val currency = state.prefs.currencySymbol

    SectionCard2("Quick presets", Icons.Default.Language, MaterialTheme.colorScheme.primary) {
        Text("One-tap buttons on the Home card. Tap a preset below to edit its name, amount and category.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))

        val list = ui.presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            list.forEach { p ->
                val cat = Categories.byId(p.category)
                var expanded by remember(p.id) { mutableStateOf(false) }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .clickable { expanded = !expanded }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color(cat.color).copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            com.expensetracker.tk.ui.components.CategoryIcon(p.category, 17.dp, Color(cat.color))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(cat.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$currency${AppState.fmtAmount(p.amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            if (expanded) "Collapse" else "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (expanded) {
                        Spacer(Modifier.height(10.dp))
                        PresetEditor(
                            initial = p,
                            currency = currency,
                            canDelete = list.size > 1,
                            onSave = { updated ->
                                val idx = list.indexOfFirst { it.id == p.id }
                                if (idx >= 0) {
                                    val mut = list.toMutableList()
                                    mut[idx] = updated
                                    state.savePresets(mut)
                                }
                                expanded = false
                            },
                            onDelete = {
                                state.savePresets(list.filter { it.id != p.id })
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        if (list.size < 8) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = {
                val id = "p${System.currentTimeMillis()}"
                state.savePresets(list + QuickPreset(id, "New preset", "New preset", 50.0, "other"))
            }) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add quick action")
            }
        }
    }
}

// ============================================================
// NOTIFICATIONS
// ============================================================
@Composable
fun NotificationSection(state: AppState) {
    val prefs = state.prefs
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(prefs.dailyReminder) }
    var hour by remember { mutableStateOf(prefs.dailyReminderHour) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled = granted && prefs.dailyReminder
        if (granted) {
            Notify.scheduleDaily(context)
        }
    }

    SectionCard2("Reminders", Icons.Default.Notifications, MaterialTheme.colorScheme.primary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Daily spending summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("A notification each evening with today's total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = { on ->
                prefs.dailyReminder = on
                if (on) {
                    permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    scope.launch { delay(400); Notify.scheduleDaily(context) }
                } else {
                    Notify.scheduleDaily(context)  // cancels when disabled
                }
                enabled = on
            })
        }
        if (enabled) {
            Spacer(Modifier.height(8.dp))
            Text("Reminder time: %02d:00".format(hour), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(8, 12, 18, 21, 22).forEach { h ->
                    FilterChip(selected = hour == h, onClick = {
                        hour = h
                        prefs.dailyReminderHour = h
                        Notify.scheduleDaily(context)
                    }, label = { Text("%02d:00".format(h)) })
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Borrow/lend reminder notifications fire on the reminder date you set per entry (automatic, no toggle needed).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ============================================================
// DATA
// ============================================================
@Composable
fun DataSection(state: AppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val csv = withContext(Dispatchers.IO) { state.exportCsv() }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { w ->
                            w.write("\ufeff")  // BOM so Excel detects UTF-8 (৳ symbol)
                            w.write(csv)
                        }
                    }
                    message = "CSV exported ✓"
                } catch (e: Exception) { message = "CSV export failed: ${e.message?.take(120)}" }
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) { state.exportJson().toString(2) }
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    message = "Exported ✓ (${json.length} chars)"
                } catch (e: Exception) { message = "Export failed: ${e.message}" }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: "" }
                    val n = withContext(Dispatchers.IO) { state.importJson(org.json.JSONObject(text)) }
                    message = "Imported $n expenses ✓ — syncing to cloud…"
                } catch (e: Exception) { message = "Import failed: ${e.message?.take(120)}" }
            }
        }
    }

    SectionCard2("Data", Icons.Default.Storage, MaterialTheme.colorScheme.primary) {
        Text("Backup to a JSON file or restore from one (web app export format is compatible).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { exportLauncher.launch("expense-tracker-export.json") }) {
                Icon(Icons.Default.Download, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Export JSON")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }) {
                Icon(Icons.Default.Upload, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Import")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { csvLauncher.launch("expense-tracker-export.csv") }) {
            Icon(Icons.Default.Description, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Export CSV (Excel/Sheets)")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            scope.launch {
                val file = withContext(Dispatchers.IO) { com.expensetracker.tk.data.ReportGenerator.generate(context, state) }
                if (file != null) {
                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context, context.packageName + ".fileprovider", file
                        )
                        val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(share, "Share monthly report")
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        message = "Report ready"
                    } catch (e: Exception) { message = "Report created: " + file.name }
                } else message = "Report generation failed"
            }
        }) {
            Icon(Icons.Default.Description, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Monthly PDF report")
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(14.dp))

        // ---- auto local backup (app-private folder — no folder picker, can't crash) ----
        val prefs = state.prefs
        var autoBackup by remember { mutableStateOf(prefs.autoBackup) }
        var lastBackupTick by remember { mutableStateOf(prefs.lastBackup) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Auto local backup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Automatic weekly snapshot of all your data (expenses, budgets, borrow/lend) inside the app's private storage. Works even without Supabase. Restore anytime with Import.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = autoBackup, onCheckedChange = { on ->
                prefs.autoBackup = on
                autoBackup = on
                com.expensetracker.tk.data.Backup.schedule(context)
                message = if (on) "Auto-backup on — weekly snapshots" else "Auto-backup off"
            })
        }
        if (autoBackup) {
            Spacer(Modifier.height(6.dp))
            Text(
                (if (lastBackupTick > 0)
                    "Last backup: " + java.time.Instant.ofEpochMilli(lastBackupTick)
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
                else "No backup written yet") + " · keeps the latest 8 snapshots",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { com.expensetracker.tk.data.Backup.writeBackup(context) }
                        if (file != null) {
                            lastBackupTick = prefs.lastBackup
                            message = "Backed up: " + file.name
                        } else message = "Backup failed"
                    }
                }) { Text("Back up now") }
                OutlinedButton(onClick = {
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { com.expensetracker.tk.data.Backup.latestBackupFile(context) }
                        if (file != null) {
                            try {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context, context.packageName + ".fileprovider", file
                                )
                                val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(share, "Share backup")
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (e: Exception) { message = "Share failed: " + e.message?.take(80) }
                        } else message = "No backup yet — tap Back up now first"
                    }
                }) { Text("Share latest") }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
    }
}


/** 30-day trash: restore accidentally deleted expenses / borrow entries. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TrashSheet(state: AppState, onClose: () -> Unit) {
    val currency = state.prefs.currencySymbol
    var items by remember { mutableStateOf(state.trashItems()) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Trash", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Close") }
        }
        Text(
            "Tap Restore to put an item back (it syncs to the cloud again). Items are removed permanently after 30 days.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            Text("Trash is empty", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items.size, key = { items[it].id + "_" + items[it].deletedAt }) { i ->
                    val item = items[i]
                    var title = "?"
                    var amount = 0.0
                    var isBorrow = false
                    try {
                        val o = org.json.JSONObject(item.payload)
                        if (item.type == "borrow") {
                            isBorrow = true
                            title = o.optString("person_name", "?")
                            amount = o.optDouble("amount", 0.0)
                        } else {
                            title = o.optString("title", "?")
                            amount = o.optDouble("amount", 0.0)
                        }
                    } catch (e: Exception) { /* keep defaults */ }
                    val daysAgo = ((System.currentTimeMillis() - item.deletedAt) / 86400000L).coerceAtLeast(0)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isBorrow) Icons.Default.Handshake else Icons.Default.ReceiptLong,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(
                                "$currency${AppState.fmtInt(amount)} · deleted ${if (daysAgo == 0L) "today" else "${daysAgo}d ago"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = {
                            if (state.restoreFromTrash(item)) {
                                items = state.trashItems()
                            }
                        }) { Text("Restore") }
                        IconButton(onClick = {
                            state.deleteFromTrashForever(item)
                            items = state.trashItems()
                        }) {
                            Icon(
                                Icons.Default.Delete, "Delete forever",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
