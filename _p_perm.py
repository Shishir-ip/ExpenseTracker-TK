# -*- coding: utf-8 -*-
import io
ROOT = r'C:\Users\SHISHIR\Downloads\expense-tracker-wwwww\expense-tracker-main\android-app\app\src\main\java\com\expensetracker\tk'

# ============ 1) Manifest: RECORD_AUDIO ============
p = r'C:\Users\SHISHIR\Downloads\expense-tracker-wwwww\expense-tracker-main\android-app\app\src\main\AndroidManifest.xml'
s = io.open(p, encoding='utf-8').read()
old = '''    <uses-permission android:name="android.permission.INTERNET" />'''
new = '''    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />'''
assert old in s, 'mf1'
s = s.replace(old, new)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('Manifest ok')

# ============ 2) Prefs: notification-permission-asked flag ============
p = ROOT + r'\data\Prefs.kt'
s = io.open(p, encoding='utf-8').read()
old = """    // ---- misc ----
    var lastSync: Long"""
new = """    var notifPermissionAsked: Boolean
        get() = sp.getBoolean("notif_perm_asked", false)
        set(v) = sp.edit().putBoolean("notif_perm_asked", v).apply()

    // ---- misc ----
    var lastSync: Long"""
assert old in s, 'pf1'
s = s.replace(old, new)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('Prefs ok')

# ============ 3) VoiceExpenseSheet: mic permission flow ============
p = ROOT + r'\ui\screens\VoiceExpenseSheet.kt'
s = io.open(p, encoding='utf-8').read()

old = '''    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (text.isNullOrBlank()) {
            error = "Didn't catch that — tap the mic to try again"
        } else {
            transcript = text
            process(text)
        }
    }

    // start listening as soon as the sheet opens (assistant shortcut flows straight here)
    LaunchedEffect(Unit) { startListening() }'''
new = '''    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (text.isNullOrBlank()) {
            error = "Didn't catch that — tap the mic to try again"
        } else {
            transcript = text
            process(text)
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startListening()
        } else {
            error = "Microphone access is needed for voice logging. Grant it in Settings → Apps → Expense Tracker → Permissions, then tap the mic."
        }
    }

    fun ensureMicAndStart() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) startListening() else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    // start listening as soon as the sheet opens (asks mic permission first)
    LaunchedEffect(Unit) { ensureMicAndStart() }'''
assert old in s, 'vs1'
s = s.replace(old, new)

old = '''            OutlinedButton(onClick = { startListening() }, enabled = !busy) {'''
new = '''            OutlinedButton(onClick = { ensureMicAndStart() }, enabled = !busy) {'''
assert old in s, 'vs2'
s = s.replace(old, new)

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('VoiceExpenseSheet ok')

# ============ 4) MainActivity: ask notification permission from new users ============
p = ROOT + r'\MainActivity.kt'
s = io.open(p, encoding='utf-8').read()

old = '''    // first-run onboarding
    var onboarded = ui.onboarded
    if (!onboarded) {
        OnboardingScreen(state, onDone = { state.setOnboarded(true) })
        return
    }'''
new = '''    // first-run onboarding
    var onboarded = ui.onboarded
    if (!onboarded) {
        OnboardingScreen(state, onDone = { state.setOnboarded(true) })
        return
    }

    // ask notification permission once for new users (Android 13+)
    val notifPermission = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> state.prefs.notifPermissionAsked = true }
    LaunchedEffect(ui.onboarded) {
        if (ui.onboarded && !state.prefs.notifPermissionAsked) {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                state.prefs.notifPermissionAsked = true
            }
        }
    }'''
assert old in s, 'mk1'
s = s.replace(old, new)

if 'import androidx.activity.result.contract.ActivityResultContracts\n' not in s:
    s = s.replace('import androidx.activity.compose.setContent\n',
                  'import androidx.activity.compose.setContent\nimport androidx.activity.result.contract.ActivityResultContracts\n', 1)

io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('MainActivity ok')
