package com.expensetracker.tk.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.tk.R
import com.expensetracker.tk.data.SupabaseRest
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.theme.AiGlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** First-run welcome: intro → optional cloud setup → theme. Shown once. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(state: AppState, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { 3 }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        // skip
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text("Skip") }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> CloudPage(state)
                else -> ThemePage(state)
            }
        }

        // dots + actions
        Row(
            Modifier.fillMaxWidth().padding(bottom = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    val active = pager.currentPage == i
                    Box(
                        Modifier
                            .size(if (active) 22.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (pager.currentPage < 2) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    } else onDone()
                }
            ) {
                Text(if (pager.currentPage < 2) "Next" else "Get started", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF4338CA), AiGlow, Color(0xFF06B6D4)))),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_splash),
                contentDescription = null,
                modifier = Modifier.size(84.dp)
            )
        }
        Spacer(Modifier.height(22.dp))
        Text("Expense Tracker TK", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text(
            "Your money, your device, your cloud.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(26.dp))
        FeatureRow(Icons.Default.Bolt, "Log in seconds", "Quick presets, voice logging, swipe to delete")
        FeatureRow(Icons.Default.CloudDone, "Offline-first sync", "Everything saves locally and syncs to your own Supabase")
        FeatureRow(Icons.Default.AutoAwesome, "AI that knows you", "Insights and chat powered by your own API key")
        Spacer(Modifier.height(20.dp))
        Text(
            "Developed by SHISHIR · github.com/Shishir-ip",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CloudPage(state: AppState) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(state.prefs.supabaseUrl) }
    var key by remember { mutableStateOf(state.prefs.supabaseKey) }
    var userKey by remember { mutableStateOf(state.prefs.userKey) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Connect your cloud", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Paste your Supabase URL and anon key — saved on this device once. You can also do this later in Settings.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = url, onValueChange = { url = it },
            label = { Text("Supabase project URL") },
            placeholder = { Text("https://xxxx.supabase.co") },
            singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = key, onValueChange = { key = it },
            label = { Text("Anon key") },
            singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = userKey, onValueChange = { userKey = it },
            label = { Text("Profile key (your web PIN, if any)") },
            singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                scope.launch {
                    testing = true
                    state.prefs.supabaseUrl = url
                    state.prefs.supabaseKey = key
                    state.prefs.userKey = userKey
                    result = try {
                        withContext(Dispatchers.IO) {
                            SupabaseRest(state.prefs.supabaseUrl, state.prefs.supabaseKey).testConnection()
                        }
                        "Connected ✓ — your data will sync"
                    } catch (e: Exception) {
                        "Saved, but the test failed: ${e.message?.take(120)}"
                    }
                    testing = false
                }
            }) { Text(if (url.isBlank()) "Skip — set up later" else "Save & test") }
            if (testing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        result?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun ThemePage(state: AppState) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(14.dp))
        Text("Make it yours", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Pick a theme now — change anytime in Settings. Reorder your home cards with the ⇅ button.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "System", 1 to "Light", 2 to "Dark", 3 to "Dynamic", 4 to "Black").forEach { (v, label) ->
                androidx.compose.material3.FilterChip(
                    selected = state.prefs.themeMode == v,
                    onClick = { state.setTheme(v) },
                    label = { Text(label) }
                )
            }
        }
    }
}
