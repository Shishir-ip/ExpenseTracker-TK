package com.expensetracker.tk.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import com.expensetracker.tk.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.Image
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.expensetracker.tk.data.Categories
import com.expensetracker.tk.data.Expense
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.theme.Danger
import com.expensetracker.tk.ui.theme.Primary

/** Pulsing placeholder brush for loading skeletons. */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "x"
    )
    background(
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            start = Offset(x * 700f - 350f, 0f),
            end = Offset(x * 700f, 260f)
        )
    )
}

/** Staggered fade+slide entrance for list content. */
@Composable
fun Stagger(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var played by remember { mutableStateOf(false) }
    val anim = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            kotlinx.coroutines.delay((index.coerceAtMost(8)) * 45L)
            anim.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            played = true
        }
    }
    Box(
        modifier.graphicsLayer {
            alpha = anim.value
            translationY = (1f - anim.value) * 40f
        }
    ) { content() }
}

/** One-shot haptic tick for taps/toggles. */
@Composable
fun rememberHaptic(): () -> Unit {
    val view = LocalView.current
    return { view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY) }
}

/** Section card mirroring the web app's white rounded card with soft shadow. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 1000f), label = "pressScale"
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null) { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun CardHeader(title: String, icon: ImageVector? = null, tint: Color = MaterialTheme.colorScheme.onSurface, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, illustration: Int? = null) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (illustration != null) {
            val floatT = rememberInfiniteTransition(label = "float")
            val floatY by floatT.animateFloat(
                initialValue = -5f, targetValue = 5f,
                animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "floatY"
            )
            Image(
                painter = painterResource(illustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(120.dp).graphicsLayer { translationY = floatY }
            )
        } else {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** Category pill with tinted icon, same colors as the web app. */
@Composable
fun CategoryBadge(categoryId: String, small: Boolean = false) {
    val cat = Categories.byId(categoryId)
    val bg = Color(cat.color).copy(alpha = 0.14f)
    val fg = Color(cat.color)
    Row(
        Modifier.clip(RoundedCornerShape(if (small) 8.dp else 10.dp)).background(bg).padding(horizontal = if (small) 6.dp else 8.dp, vertical = if (small) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier.size(if (small) 12.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            CategoryIcon(categoryId, if (small) 10.dp else 13.dp, fg)
        }
        Text(cat.name, style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

/** Category glyph — crisp Material vector icons, tinted per category. */
@Composable
fun CategoryIcon(categoryId: String, size: Dp, tint: Color) {
    val icon = when (categoryId) {
        "food" -> Icons.Default.Restaurant
        "transport" -> Icons.Default.DirectionsBus
        "shopping" -> Icons.Default.ShoppingBag
        "entertainment" -> Icons.Default.SportsEsports
        "bills" -> Icons.Default.ReceiptLong
        "health" -> Icons.Default.Favorite
        "education" -> Icons.Default.School
        else -> Icons.Default.MoreHoriz
    }
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
}

/** Animated amount: slides the old value up and the new one in when the total changes. */
@Composable
fun AnimatedAmount(target: Double, prefix: String, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall, color: Color = MaterialTheme.colorScheme.onSurface) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val lineH = with(density) { (style.fontSize * 1.35f).toDp() }
    val shown = remember { mutableStateOf(target) }
    val anim = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target != shown.value) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
            shown.value = target
        }
    }
    androidx.compose.foundation.layout.Box(
        Modifier
            .clipToBounds()
            .height(lineH)
    ) {
        Text(
            "$prefix${AppState.fmtAmount(shown.value)}", style = style, color = color, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.graphicsLayer { translationY = -anim.value * lineH.toPx(); alpha = 1f - anim.value }
        )
        Text(
            "$prefix${AppState.fmtAmount(target)}", style = style, color = color, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.graphicsLayer { translationY = (1f - anim.value) * lineH.toPx(); alpha = anim.value }
        )
    }
}

@Composable
fun SyncBadge(sync: com.expensetracker.tk.data.SyncState, pending: Int) {
    val label: String
    val color: Color
    when (sync) {
        com.expensetracker.tk.data.SyncState.SYNCING -> { label = "Syncing…"; color = MaterialTheme.colorScheme.primary }
        com.expensetracker.tk.data.SyncState.SYNCED -> { label = if (pending > 0) "$pending pending" else "Synced"; color = com.expensetracker.tk.ui.theme.Success }
        com.expensetracker.tk.data.SyncState.ERROR -> { label = "Sync error · $pending pending"; color = Danger }
        com.expensetracker.tk.data.SyncState.OFFLINE -> { label = "Offline · saved locally"; color = com.expensetracker.tk.ui.theme.Warning }
    }
    val bg by animateColorAsState(color.copy(alpha = 0.14f), label = "bgbadge")
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier.size(6.dp).clip(CircleShape).background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

/** One transaction row with swipe-to-delete + tap-to-edit. */
@Composable
fun TransactionRow(
    expense: Expense,
    currency: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onBoundsChange: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null
) {
    val view = LocalView.current
    val rowInteraction = remember { MutableInteractionSource() }
    val rowPressed by rowInteraction.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (rowPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 1000f), label = "rowScale"
    )
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                onDelete(); true
            } else false
        },
        positionalThreshold = { total -> total * 0.62f }
    )
    LaunchedEffect(expense.id) { dismissState.reset() }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.onGloballyPositioned { onBoundsChange?.invoke(it.boundsInRoot()) },
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
        val cat = Categories.byId(expense.category)
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = rowScale; scaleY = rowScale }
                .clip(RoundedCornerShape(16.dp))
                .background(
                    androidx.compose.ui.graphics.lerp(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant,
                        0.55f
                    )
                )
                .clickable(interactionSource = rowInteraction, indication = null) { onClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // category accent stripe — scan a day's spending by color
            Box(
                Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(cat.color))
            )
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(cat.color).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(expense.category, 20.dp, Color(cat.color))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    expense.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(AppState.parseDate(expense.date).let { "${it.dayOfMonth} ${AppState.MONTHS[it.monthValue - 1].take(3)}" },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (expense.notes.isNotBlank()) {
                        Text("· ${expense.notes}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                    }
                    if (expense.isRecurring) Text("· 🔁", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("-$currency${AppState.fmtAmount(expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Danger, fontWeight = FontWeight.ExtraBold)
                if (expense.dirty) Text("⏳", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Gradient chip used for quick presets & AI buttons. */
@Composable
fun GradientChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(com.expensetracker.tk.ui.theme.AiGlow, Primary, com.expensetracker.tk.ui.theme.Cyan)))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leading?.invoke()
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

/** Tiny markdown renderer for AI output: **bold**, *italic*, `code`, ## headings, bullets, numbered lists. */
@Composable
fun SimpleMarkdown(text: String) {
    val lines = text.lines()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> Spacer(Modifier.height(2.dp))
                line.startsWith("### ") -> MarkdownLine(line.removePrefix("### "), MaterialTheme.typography.titleSmall, FontWeight.Bold)
                line.startsWith("## ") -> MarkdownLine(line.removePrefix("## "), MaterialTheme.typography.titleMedium, FontWeight.Bold)
                line.startsWith("# ") -> MarkdownLine(line.removePrefix("# "), MaterialTheme.typography.titleMedium, FontWeight.ExtraBold)
                line.startsWith("- ") || line.startsWith("* ") -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    MarkdownLine(line.substring(2), MaterialTheme.typography.bodyMedium, FontWeight.Normal)
                }
                line.startsWith("> ") -> MarkdownLine(
                    line.substring(2), MaterialTheme.typography.bodyMedium, FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Regex("^\\d+\\. ").containsMatchIn(line) -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(line.substringBefore(". ") + ".", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    MarkdownLine(line.substringAfter(". "), MaterialTheme.typography.bodyMedium, FontWeight.Normal)
                }
                else -> MarkdownLine(line, MaterialTheme.typography.bodyMedium, FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun MarkdownLine(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    weight: androidx.compose.ui.text.font.FontWeight,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    // inline **bold** / *italic* / `code`
    val parts = Regex("(\\*\\*[^*]+\\*\\*|\\*[^*]+\\*|`[^`]+`)").findAll(text).map { it.value }.toList()
    if (parts.isEmpty()) {
        Text(stripInline(text), style = style, fontWeight = weight, color = color)
        return
    }
    val annotated = androidx.compose.ui.text.buildAnnotatedString {
        var rest = text
        while (rest.isNotEmpty()) {
            val match = Regex("(\\*\\*[^*]+\\*\\*|\\*[^*]+\\*|`[^`]+`)").find(rest)
            if (match == null) {
                append(stripInline(rest)); break
            }
            append(stripInline(rest.substring(0, match.range.first)))
            val token = match.value
            when {
                token.startsWith("**") -> withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(token.removeSurrounding("**")) }
                token.startsWith("`") -> withStyle(androidx.compose.ui.text.SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, background = MaterialTheme_colorCode())) { append(token.removeSurrounding("`")) }
                else -> withStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) { append(token.removeSurrounding("*")) }
            }
            rest = rest.substring(match.range.last + 1)
        }
    }
    Text(annotated, style = style, fontWeight = weight, color = color)
}

private fun MaterialTheme_colorCode(): Color = Color(0x14000000)

private fun stripInline(s: String): String = s
    .replace("**", "")
    .replace("`", "")
    .replace(Regex("(?<!\\*)\\*(?!\\*)"), "")


/** Brief celebratory burst (ring + check + confetti) shown over the screen. */
@Composable
fun SuccessBurst(visible: Boolean, onDone: () -> Unit) {
    if (!visible) return
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
        onDone()
    }
    val dotColors = listOf(
        Color(0xFFF59E0B), Color(0xFF06B6D4), Color(0xFF8B5CF6),
        Color(0xFFEF4444), Color(0xFF10B981), Color(0xFFEC4899)
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // expanding ring
        Box(
            Modifier
                .size((36 + 150 * anim.value).dp)
                .clip(CircleShape)
                .background(com.expensetracker.tk.ui.theme.Success.copy(alpha = ((1f - anim.value) * 0.35f).coerceIn(0f, 1f)))
        )
        // confetti dots flying outward
        for (i in 0 until 10) {
            val angle = Math.toRadians((i * 36 + 12).toDouble())
            val dist = 40f + 120f * anim.value
            val gravity = 40f * anim.value * anim.value
            Box(
                Modifier
                    .offset(x = (kotlin.math.cos(angle) * dist).dp, y = (kotlin.math.sin(angle) * dist + gravity).dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColors[i % dotColors.size].copy(alpha = (1f - anim.value).coerceIn(0f, 1f)))
            )
        }
        // check badge
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(com.expensetracker.tk.ui.theme.Success),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check, contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(38.dp)
                    .graphicsLayer { alpha = (anim.value * 3f).coerceAtMost(1f) }
            )
        }
    }
}
