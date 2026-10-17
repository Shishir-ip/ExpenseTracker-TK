package com.expensetracker.tk.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.tk.data.Categories
import com.expensetracker.tk.state.AppState
import com.expensetracker.tk.ui.theme.HeatHigh
import com.expensetracker.tk.ui.theme.HeatLow
import com.expensetracker.tk.ui.theme.HeatMid
import com.expensetracker.tk.ui.theme.HeatMin
import java.time.LocalDate

data class DonutSlice(val label: String, val value: Double, val color: Color)

/** Animated donut chart with centre label + legend, drawn on Canvas (60fps friendly). */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerTop: String,
    centerBottom: String,
    modifier: Modifier = Modifier,
    onSliceClick: ((DonutSlice) -> Unit)? = null,
    selectedSlice: DonutSlice? = null,
    currency: String = ""
) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(slices) { played = true }
    val sweep by animateFloatAsState(if (played) 1f else 0f, tween(900, easing = FastOutSlowInEasing), label = "sweep")

    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(190.dp)) {
                val total = slices.sumOf { it.value }.toFloat()
                var start = -90f
                val stroke = Stroke(width = 34.dp.toPx(), cap = StrokeCap.Butt)
                if (total <= 0f) {
                    drawArc(
                        color = Color(0x22000000),
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        style = stroke
                    )
                } else {
                    // 4-degree gaps between slices for a cleaner, modern look
                    val gap = if (slices.size > 1) 4f else 0f
                    slices.forEach { sl ->
                        val angle = (sl.value.toFloat() / total) * 360f * sweep
                        val drawn = angle - gap
                        if (drawn > 0.5f) {
                            drawArc(
                                color = sl.color,
                                startAngle = start + gap / 2f, sweepAngle = drawn, useCenter = false,
                                style = stroke
                            )
                        }
                        start += angle
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectedSlice != null) {
                    Text(selectedSlice.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = selectedSlice.color, fontWeight = FontWeight.Bold)
                    Text("$currency${AppState.fmtInt(selectedSlice.value)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    val pct = if (slices.sumOf { it.value } > 0) (selectedSlice.value / slices.sumOf { it.value } * 100).toInt() else 0
                    Text("$pct% of total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(centerTop, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(centerBottom, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        // legend
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.take(8).forEach { s ->
                val isSelected = selectedSlice?.label == s.label
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(if (onSliceClick != null) Modifier.clickable { onSliceClick(s) } else Modifier)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) s.color.copy(alpha = 0.14f) else Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(s.color))
                    Spacer(Modifier.width(8.dp))
                    Text(s.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Text(AppState.fmtAmount(s.value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    val pct = if (slices.sumOf { it.value } > 0) (s.value / slices.sumOf { it.value } * 100).toInt() else 0
                    Spacer(Modifier.width(6.dp))
                    Text("$pct%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

data class BarEntry(val label: String, val value: Double, val highlight: Boolean = false)

/** Animated vertical bar chart with value labels. */
@Composable
fun BarChart(bars: List<BarEntry>, modifier: Modifier = Modifier, currency: String = "") {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(bars) { played = true }
    val grow by animateFloatAsState(if (played) 1f else 0f, tween(800, easing = FastOutSlowInEasing), label = "grow")
    val maxVal = bars.maxOfOrNull { it.value } ?: 1.0

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(170.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bars.forEach { b ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    // value label above each bar
                    Text(
                        if (b.value > 0) AppState.fmtInt(b.value) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (b.highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(3.dp))
                    val h = ((b.value / maxVal).coerceAtMost(1.0) * 128.0 * grow).coerceAtLeast(if (b.value > 0) 4.0 else 0.0)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(h.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (b.highlight) Brush.verticalGradient(listOf(Color(0xFF818CF8), Color(0xFF6366F1)))
                                else Brush.verticalGradient(listOf(Color(0xFFA5B4FC), Color(0xFF818CF8)))
                            )
                    )
                }
            }
        }
        // baseline
        Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            bars.forEach { b ->
                Text(
                    b.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (b.highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/** Smooth curved line chart with gradient fill — used for monthly trends. */
@Composable
fun LineChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6366F1)
) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(values) { played = true }
    val grow by animateFloatAsState(if (played) 1f else 0f, tween(900, easing = FastOutSlowInEasing), label = "line")

    val outlineColor = MaterialTheme.colorScheme.outline
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            if (values.size < 2) return@Canvas
            val maxV = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0).toFloat()
            val stepX = size.width / (values.size - 1)
            val padTop = 16.dp.toPx()
            val usable = size.height - padTop
            val pts = values.mapIndexed { i, v ->
                Offset(i * stepX, padTop + usable * (1f - (v.toFloat() / maxV) * grow))
            }

            // smooth path through midpoints
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) {
                    val prev = pts[i - 1]
                    val cur = pts[i]
                    val midX = (prev.x + cur.x) / 2f
                    cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                }
            }
            // gradient fill under the curve
            val fill = androidx.compose.ui.graphics.Path().apply {
                addPath(path)
                lineTo(pts.last().x, size.height)
                lineTo(pts.first().x, size.height)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), Color.Transparent)))
            drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            // dots
            pts.forEach { p ->
                drawCircle(color, radius = 4.dp.toPx(), center = p)
                drawCircle(Color.White, radius = 2.dp.toPx(), center = p)
            }
            // baseline
            drawLine(
                outlineColor.copy(alpha = 0.5f),
                start = Offset(0f, size.height - 1.dp.toPx()),
                end = Offset(size.width, size.height - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEach { l ->
                Text(
                    l,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/** Monthly calendar heatmap — same heat levels as the web app (>500 high, >250 mid, >100 low, >0 min). */
@Composable
fun MonthCalendarHeatmap(
    dayTotals: Map<String, Double>,
    year: Int,
    month: Int, // 1-based
    currency: String,
    onDayClick: (String) -> Unit = {}
) {
    val monthName = AppState.MONTHS[month - 1]
    val daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth()
    val firstDow = java.time.LocalDate.of(year, month, 1).dayOfWeek.value % 7 // Sunday=0
    val todayStr = AppState.todayStr()

    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(
                    it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        val weeks = (firstDow + daysInMonth + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            var day = 1
            repeat(weeks) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(7) { dow ->
                        val cellWeight = Modifier.weight(1f)
                        if ((it == 0 && dow < firstDow) || day > daysInMonth) {
                            Box(cellWeight.height(44.dp))
                        } else {
                            val dstr = "%04d-%02d-%02d".format(year, month, day)
                            val amount = dayTotals[dstr] ?: 0.0
                            val (bg, fg) = when {
                                amount > 500 -> HeatHigh to Color.White
                                amount > 250 -> HeatMid to Color.White
                                amount > 100 -> HeatLow to Color.White
                                amount > 0 -> HeatMin to Color.White
                                else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val isToday = dstr == todayStr
                            Column(
                                cellWeight
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isToday) Modifier.border(
                                            2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)
                                        ) else Modifier
                                    )
                                    .background(bg)
                                    .clickable { onDayClick(dstr) }
                                    .padding(vertical = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "$day", style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold, color = fg
                                )
                                if (amount > 0) Text(
                                    "${AppState.fmtInt(amount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = fg, fontWeight = FontWeight.SemiBold
                                )
                            }
                            day++
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
            listOf(
                HeatMin to "≤100", HeatLow to "≤250", HeatMid to "≤500", HeatHigh to "500+"
            ).forEach { (c, l) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(c))
                    Text(l, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        // today ring drawn via border in future; simple marker:
        Text(
            "Tap a day to filter",
            Modifier.fillMaxWidth().padding(top = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Rolling 30-day purple heatmap like the insights page of the web app. */
@Composable
fun Heatmap30(days: List<Pair<String, Double>>, onDayClick: (String) -> Unit = {}) {
    val maxAmount = (days.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    Column {
        rowsOf(days, 10).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                week.forEach { (date, amount) ->
                    val intensity = amount / maxAmount
                    val bg = when {
                        intensity <= 0.0 -> MaterialTheme.colorScheme.surfaceVariant
                        intensity < 0.25 -> Color(0xFFA78BFA)
                        intensity < 0.5 -> Color(0xFF8B5CF6)
                        intensity < 0.75 -> Color(0xFF7C3AED)
                        else -> Color(0xFFEF4444)
                    }
                    val dayNum = try { AppState.parseDate(date).dayOfMonth } catch (e: Exception) { 0 }
                    Column(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(bg)
                            .clickable { onDayClick(date) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("$dayNum", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("None" to MaterialTheme.colorScheme.surfaceVariant, "Low" to Color(0xFFA78BFA), "Mid" to Color(0xFF8B5CF6), "High" to Color(0xFF7C3AED), "Max" to Color(0xFFEF4444)).forEach { (l, c) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(c))
                    Text(l, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun <T> rowsOf(items: List<T>, perRow: Int): List<List<T>> {
    val rows = ArrayList<List<T>>()
    var i = 0
    while (i < items.size) {
        rows.add(items.subList(i, minOf(i + perRow, items.size)))
        i += perRow
    }
    return rows
}


/** Month burn-down: cumulative actual spend vs the ideal target pace, today marked. */
@Composable
fun BurnDownChart(
    cumulative: List<Double>,
    daysInMonth: Int,
    targetTotal: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            if (daysInMonth < 2 || targetTotal <= 0.0) return@Canvas
            val scaleMax = (targetTotal.coerceAtLeast(cumulative.maxOrNull() ?: 0.0)).toFloat().coerceAtLeast(1f)
            fun x(day: Int) = (day.toFloat() / (daysInMonth - 1).toFloat()) * size.width
            fun y(amount: Double) = size.height * (1f - (amount.toFloat() / scaleMax)).coerceIn(0f, 1f)

            // ideal pace line (dashed)
            val ideal = androidx.compose.ui.graphics.Path().apply {
                moveTo(x(0), y(0.0))
                lineTo(x(daysInMonth - 1), y(targetTotal))
            }
            drawPath(
                ideal, outlineColor,
                style = Stroke(width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            )

            // actual cumulative curve
            if (cumulative.isNotEmpty()) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x(0), y(cumulative[0]))
                    for (i in 1 until cumulative.size) lineTo(x(i), y(cumulative[i]))
                }
                val fill = androidx.compose.ui.graphics.Path().apply {
                    addPath(path)
                    lineTo(x(cumulative.size - 1), size.height)
                    lineTo(x(0), size.height)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent)))
                drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                // today marker
                drawCircle(color, radius = 5.dp.toPx(), center = Offset(x(cumulative.size - 1), y(cumulative.last())))
                drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(x(cumulative.size - 1), y(cumulative.last())))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(14.dp, 3.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Text("Actual", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(14.dp, 3.dp).clip(RoundedCornerShape(2.dp)).background(outlineColor))
                Text("Target pace", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
            }
        }
    }
}
