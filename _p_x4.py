# -*- coding: utf-8 -*-
import io
ROOT = r'C:\Users\SHISHIR\Downloads\expense-tracker-wwwww\expense-tracker-main\android-app\app\src\main\java\com\expensetracker\tk'

# ============ TransactionsScreen: reorder declarations + imports ============
p = ROOT + r'\ui\screens\TransactionsScreen.kt'
s = io.open(p, encoding='utf-8').read()

# remove the premature LaunchedEffect block
old = '''    val scope = rememberCoroutineScope()

    // Year Overview month tap: jump into calendar view at that month
    LaunchedEffect(state.pendingUiAction.value) {
        val a = state.pendingUiAction.value
        if (a?.startsWith("history:") == true) {
            runCatching { calMonth = YearMonth.parse(a.substringAfter("history:")) }
            viewMode = 1
            calDay = null
            state.pendingUiAction.value = null
        }
    }
    var calMonth by remember { mutableStateOf(YearMonth.now()) }
    var calDay by remember { mutableStateOf<String?>(null) }
    val haptic = rememberHaptic()'''
new = '''    var calMonth by remember { mutableStateOf(YearMonth.now()) }
    var calDay by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()

    // Year Overview month tap: jump into calendar view at that month
    LaunchedEffect(state.pendingUiAction.value) {
        val a = state.pendingUiAction.value
        if (a?.startsWith("history:") == true) {
            runCatching { calMonth = YearMonth.parse(a.substringAfter("history:")) }
            viewMode = 1
            calDay = null
            state.pendingUiAction.value = null
        }
    }'''
assert old in s, 'tx1'
s = s.replace(old, new)

if 'import androidx.compose.runtime.LaunchedEffect' not in s:
    s = s.replace('import androidx.compose.runtime.Composable\n',
                  'import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.LaunchedEffect\n', 1)

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('Transactions fixed')

# ============ HomeScreen: LocalContext val + ChartCard scope ============
p = ROOT + r'\ui\screens\HomeScreen.kt'
s = io.open(p, encoding='utf-8').read()

old = '''fun HomeScreen(state: AppState, onAdd: () -> Unit, onEdit: (String, androidx.compose.ui.geometry.Rect?) -> Unit, onOpenSettings: () -> Unit = {}) {'''
new = '''fun HomeScreen(state: AppState, onAdd: () -> Unit, onEdit: (String, androidx.compose.ui.geometry.Rect?) -> Unit, onOpenSettings: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current'''
assert old in s, 'h1'
s = s.replace(old, new)

# ChartCard scope
old = '''    val total = filtered.sumOf { it.amount }

    SectionCard {
        CardHeader("Spending by Category", Icons.Default.PieChart, MaterialTheme.colorScheme.primary,'''
new = '''    val total = filtered.sumOf { it.amount }
    val scope = rememberCoroutineScope()

    SectionCard {
        CardHeader("Spending by Category", Icons.Default.PieChart, MaterialTheme.colorScheme.primary,'''
assert old in s, 'h2'
s = s.replace(old, new)

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('HomeScreen fixed')

# ============ SettingsScreen TrashSheet: context val ============
p = ROOT + r'\ui\screens\SettingsScreen.kt'
s = io.open(p, encoding='utf-8').read()
old = '''fun TrashSheet(state: AppState, onClose: () -> Unit) {
    val currency = state.prefs.currencySymbol
    var items by remember { mutableStateOf(state.trashItems()) }'''
new = '''fun TrashSheet(state: AppState, onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currency = state.prefs.currencySymbol
    var items by remember { mutableStateOf(state.trashItems()) }'''
assert old in s, 'ts1'
s = s.replace(old, new)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('TrashSheet fixed')

# ============ AppShortcuts: TrackerApp import ============
p = ROOT + r'\data\AppShortcuts.kt'
s = io.open(p, encoding='utf-8').read()
if 'import com.expensetracker.tk.TrackerApp' not in s:
    s = s.replace('import com.expensetracker.tk.MainActivity\n',
                  'import com.expensetracker.tk.MainActivity\nimport com.expensetracker.tk.TrackerApp\n', 1)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('AppShortcuts fixed')

# ============ MainActivity: else branch action reference ============
p = ROOT + r'\MainActivity.kt'
s = io.open(p, encoding='utf-8').read()
old = '''                    else -> {
                        // "preset:<id>" is consumed by HomeScreen (one-tap log)
                        if (action != null && action.startsWith("preset:")) {
                            nav.navigate("home") {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }'''
new = '''                    else -> {
                        // "preset:<id>" is consumed by HomeScreen (one-tap log)
                        val a = state.pendingUiAction.value
                        if (a != null && a.startsWith("preset:")) {
                            nav.navigate("home") {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }'''
assert old in s, 'mk1'
s = s.replace(old, new)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('MainActivity fixed')
