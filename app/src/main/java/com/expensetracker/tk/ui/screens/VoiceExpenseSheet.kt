package com.expensetracker.tk.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Speak an expense ("lunch 120 and bus 50 yesterday"), review the parsed drafts,
 * save. Uses the on-device SpeechRecognizer (works on phones without the Google
 * voice dialog), your AI key for parsing when online, keyword fallback offline.
 * A typed input is always available as a fallback.
 */
@Composable
fun VoiceExpenseSheet(state: AppState, currency: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()

    var transcript by remember { mutableStateOf<String?>(null) }
    var partial by remember { mutableStateOf<String?>(null) }
    var listening by remember { mutableStateOf(false) }
    var drafts by remember { mutableStateOf(listOf<VoiceParser.Draft>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var typed by remember { mutableStateOf("") }

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

    // ---- on-device speech recognizer (no external activity needed) ----
    val speech = remember {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
        } catch (e: Exception) { null }
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                speech?.destroy()
            } catch (e: Exception) { /* already gone */ }
        }
    }

    fun startListening() {
        if (speech == null || !SpeechRecognizer.isRecognitionAvailable(context)) {
            error = "Voice input isn't available on this phone — type it below instead:"
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true; error = null; partial = null
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                listening = false
            }

            override fun onError(err: Int) {
                listening = false
                error = when (err) {
                    6 -> "Didn't hear anything — try again"
                    7 -> "No speech matched — try again"
                    8 -> "Recognition service is busy — try again in a moment"
                    9 -> "Microphone permission is missing"
                    else -> "Voice service error — type it below instead"
                }
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text.isNullOrBlank()) {
                    error = "Didn't catch that — tap the mic to try again"
                } else {
                    transcript = text
                    partial = null
                    process(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    partial = it
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try {
            speech.startListening(intent)
        } catch (e: Exception) {
            error = "Voice input failed to start — type it below instead:"
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startListening()
        } else {
            error = "Microphone access is needed for voice logging. Grant it in Settings → Apps → Expense Tracker → Permissions, or type below."
        }
    }

    fun ensureMicAndStart() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) startListening() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    // start listening as soon as the sheet opens (asks mic permission first)
    LaunchedEffect(Unit) { ensureMicAndStart() }

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

        if (listening) {
            val pulse = rememberInfiniteTransition(label = "mic")
            val scale by pulse.animateFloat(
                initialValue = 1f, targetValue = 1.25f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                label = "pulse"
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                )
                Text("Listening… speak now", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }

        val shown = partial ?: transcript
        shown?.let {
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
                    drafts = drafts.filterIndexed { i, _ -> i != idx }
                    if (drafts.isEmpty()) {
                        onClose()
                    }
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { ensureMicAndStart() }, enabled = !busy) {
                Icon(Icons.Default.Mic, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (transcript == null) "Listen again" else "Say another", maxLines = 1)
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
                    Text("Save all (${drafts.size})", maxLines = 1)
                }
            }
        }

        // typed fallback — always available
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            placeholder = { Text("Or type: lunch 120 and bus 50") },
            trailingIcon = {
                IconButton(onClick = {
                    if (typed.isNotBlank()) {
                        val t = typed.trim()
                        typed = ""
                        transcript = t
                        process(t)
                    }
                }, enabled = typed.isNotBlank()) {
                    Icon(Icons.Default.CheckCircle, "Parse", tint = MaterialTheme.colorScheme.primary)
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )

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
