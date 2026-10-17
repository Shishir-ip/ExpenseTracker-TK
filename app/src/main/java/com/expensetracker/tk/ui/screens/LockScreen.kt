package com.expensetracker.tk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.theme.Primary
import com.expensetracker.tk.ui.theme.Slate100
import com.expensetracker.tk.ui.theme.Slate800
import com.expensetracker.tk.ui.theme.Slate900

/**
 * OPTIONAL app lock — shown only when the user enabled it in Settings.
 * PIN keypad + optional biometric unlock. App works with no lock at all by default.
 */
@Composable
fun LockScreen(state: AppState, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun check(p: String) {
        if (state.prefs.hashPin(p) == state.prefs.lockPinHash) onUnlocked() else {
            error = true
            pin = ""
        }
    }

    LaunchedEffect(pin) {
        if (pin.length == 4) check(pin)
        if (error && pin.isNotEmpty()) error = false
    }

    // biometric
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        if (state.prefs.lockBiometric) {
            try {
                val fm = androidx.fragment.app.FragmentActivity::class.java.cast(ctx)
                val bio = androidx.biometric.BiometricPrompt(fm,
                    androidx.core.content.ContextCompat.getMainExecutor(ctx),
                    object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                            onUnlocked()
                        }
                    })
                val info = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Expense Tracker")
                    .setNegativeButtonText("Use PIN")
                    .build()
                bio.authenticate(info)
            } catch (e: Exception) { /* fall back to PIN */ }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF16213E)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(74.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Primary, Color(0xFF8B5CF6)))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            Text("Expense Tracker", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Enter your PIN to unlock", color = Slate100.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)

            // dots
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) { i ->
                    Box(
                        Modifier.size(14.dp).clip(CircleShape)
                            .background(if (i < pin.length) Primary else Color.White.copy(alpha = 0.25f))
                    )
                }
            }
            if (error) Text("Wrong PIN — try again", color = Color(0xFFF87171), style = MaterialTheme.typography.bodySmall)

            // keypad
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("bio", "0", "del"))
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { key ->
                        when (key) {
                            "del" -> Box(
                                Modifier.size(68.dp).clip(CircleShape).clickable { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Backspace, "Delete", tint = Slate100) }
                            "bio" -> if (state.prefs.lockBiometric) Box(
                                Modifier.size(68.dp).clip(CircleShape).clickable {
                                    // re-trigger biometric by recomposition hack — simple: ignore, auto prompt runs on open
                                },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Fingerprint, "Biometric", tint = Primary) }
                            else Box(Modifier.size(68.dp))
                            else -> Box(
                                Modifier.size(68.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable { if (pin.length < 4) pin += key },
                                contentAlignment = Alignment.Center
                            ) { Text(key, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}
