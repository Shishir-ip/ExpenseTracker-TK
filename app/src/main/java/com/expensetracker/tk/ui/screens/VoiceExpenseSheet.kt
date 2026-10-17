package com.expensetracker.tk.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expensetracker.tk.data.Categories
import com.expensetracker.tk.data.OpenRouter
import com.expensetracker.tk.data.VoiceParser
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.components.rememberHaptic
import com.expensetracker.tk.ui.theme.AiGlow
import com.expensetracker.tk.ui.theme.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Speak an expense ("lunch 120 and bus 50 yesterday"), review the parsed drafts,
 * save. Uses the AI model for parsing when a key is configured, keyword fallback
 * otherwise. Speech recognition uses the system dialog (no mic permission needed).
 */
@Composable
fun VoiceExpenseSheet(state: AppState, currency: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()

    var transcript by remember { mutableStateOf<String?>(null) }
    var drafts by remember { mutableStateOf(listOf<VoiceParser.Draft>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedCount by remember { mutableStateOf(0) }

    val process: (String) -> Unit = { text ->
        scope.launch {
            busy = true
            error = null
            drafts = withContext(Dispatchers.IO) {
                if (state.prefs.openRouterKey.isNotBlank()) {
                    try {
                        VoiceParser.parseAi(text, state.prefs)
                    } catch (e: Exception) {
                        VoiceParser.parseLocal(text)
                    }
                } else {
                    VoiceParser.parseLocal(text)
                }
            }
            if (drafts.isEmpty()) error = "No expense found in that — try e.g. \"lunch 120 and bus 50\""
            busy = false
        }
    }

    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (text.isNullOrBlank()) {
            error = "Didn't catch that — tap the mic to try again"
        } else {
            transcript = text
            process(text)
        }
    }

    fun startListening() {
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
            error = "Voice recognition isn't available on this phone — type instead:"
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say e.g. \"lunch 120 and bus 50 yesterday\"")
        }
        try {
            recognizer.launch(intent)
        } catch (e: Exception) {
            error = "Couldn't open voice input — type instead:"
        }
    }

    // start listening as soon as the sheet opens (assistant shortcut flows straight here)
    LaunchedEffect(Unit) { startListening() }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Voice expense", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("Say it once — review, then save", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
        }

        transcript?.let {
            Text(
                "\"$it\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(10.dp)
            )
        }

        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = AiGlow)
                Text("Reading that…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        drafts.forEachIndexed { idx, d ->
            VoiceDraftCard(
                draft = d,
                currency = currency,
                onChange = { updated -> drafts = drafts.toMutableList().also { it[idx] = updated } },
                onSave = {
                    haptic()
                    state.addExpense(d.title.ifBlank { "Voice expense" }, d.amount, d.category, d.date, "", false)
                    savedCount++
                    drafts = drafts.filterIndexed { i, _ -> i != idx }
                    if (drafts.isEmpty()) {
                        onClose()
                    }
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { startListening() }, enabled = !busy) {
                Icon(Icons.Default.Mic, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (transcript == null) "Listen again" else "Say another")
            }
            if (drafts.size > 1) {
                Button(
                    enabled = !busy,
                    onClick = {
                        haptic()
                        drafts.forEach { d ->
                            state.addExpense(d.title.ifBlank { "Voice expense" }, d.amount, d.category, d.date, "", false)
                        }
                        onClose()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save all (${drafts.size})")
                }
            }
        }

        Text(
            if (state.prefs.openRouterKey.isNotBlank())
                "Parsing with your AI model · falls back to offline keywords"
            else "Offline keyword parsing · add an AI key in Settings for smarter parsing",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VoiceDraftCard(
    draft: VoiceParser.Draft,
    currency: String,
    onChange: (VoiceParser.Draft) -> Unit,
    onSave: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft.title,
                onValueChange = { onChange(draft.copy(title = it)) },
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = if (draft.amount % 1.0 == 0.0) "%.0f".format(draft.amount) else draft.amount.toString(),
                onValueChange = { s -> onChange(draft.copy(amount = s.filter { ch -> ch.isDigit() || ch == '.' }.take(10).toDoubleOrNull() ?: 0.0)) },
                label = { Text(currency) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(110.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Categories.all.forEach { c ->
                val selected = draft.category == c.id
                Text(
                    c.name,
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) Color(c.color).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { onChange(draft.copy(category = c.id)) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color(c.color) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (draft.date == java.time.LocalDate.now().toString()) "Today" else draft.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onSave, enabled = draft.amount > 0) {
                Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save")
            }
        }
    }
}
