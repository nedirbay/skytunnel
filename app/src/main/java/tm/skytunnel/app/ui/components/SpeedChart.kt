package tm.skytunnel.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tm.skytunnel.app.ui.theme.Violet
import kotlin.math.roundToInt

/**
 * Ýumşadylan (spline) tizlik grafigi — 06_Statistics_Screen dizaýnyndaky ýaly,
 * saýlanan nokatda "24.5 Mbps" diýen çykgyt bilen.
 *
 * Grafigiň islendik ýerine basyp, başga nokady saýlap bolýar.
 */
@Composable
fun SpeedChart(
    points: List<Float>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lineColor: Color = Violet,
    gridColor: Color = Color(0xFFDDDCEE),
    labelColor: Color = Color(0xFF8E92B4),
    tooltipBg: Color = Color(0xFF1C2246)
) {
    if (points.isEmpty()) return

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(260),
        label = "sel"
    )

    val maxY = 30f
    val leftPad = with(density) { 26.dp.toPx() }
    val topPad = with(density) { 26.dp.toPx() }
    val bottomPad = with(density) { 10.dp.toPx() }
    val rightPad = with(density) { 8.dp.toPx() }

    Canvas(
        modifier
            .fillMaxSize()
            .pointerInput(points.size) {
                detectTapGestures { offset ->
                    val plotW = size.width - leftPad - rightPad
                    if (plotW <= 0f) return@detectTapGestures
                    val rel = ((offset.x - leftPad) / plotW).coerceIn(0f, 1f)
                    onSelect((rel * (points.size - 1)).roundToInt())
                }
            }
    ) {
        val plotW = size.width - leftPad - rightPad
        val plotH = size.height - topPad - bottomPad
        if (plotW <= 0f || plotH <= 0f) return@Canvas

        fun px(i: Int) = leftPad + plotW * i / (points.size - 1).coerceAtLeast(1)
        fun py(v: Float) = topPad + plotH * (1f - (v / maxY).coerceIn(0f, 1f))

        // --- Tor çyzyklary we Y belgileri ---
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
        listOf(0f, 10f, 20f, 30f).forEach { v ->
            val y = py(v)
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(size.width - rightPad, y),
                strokeWidth = 1f,
                pathEffect = dash
            )
            drawYLabel(measurer, v.toInt().toString(), leftPad, y, labelColor)
        }

        // --- Ýumşadylan çyzyk ---
        val line = smoothPath(points.indices.map { Offset(px(it), py(points[it])) })

        // Aşagyndaky doldurgyç
        val fill = Path().apply {
            addPath(line)
            lineTo(px(points.lastIndex), topPad + plotH)
            lineTo(px(0), topPad + plotH)
            close()
        }
        drawPath(
            fill,
            Brush.verticalGradient(
                listOf(lineColor.copy(alpha = 0.22f), Color.Transparent),
                startY = topPad,
                endY = topPad + plotH
            )
        )

        drawPath(
            line,
            lineColor,
            style = Stroke(width = 2.6f, cap = StrokeCap.Round)
        )

        // --- Saýlanan nokat ---
        val idx = animatedIndex.coerceIn(0f, points.lastIndex.toFloat())
        val i0 = idx.toInt().coerceIn(0, points.lastIndex)
        val i1 = (i0 + 1).coerceAtMost(points.lastIndex)
        val t = idx - i0
        val mx = px(i0) + (px(i1) - px(i0)) * t
        val value = points[i0] + (points[i1] - points[i0]) * t
        val my = py(value)

        drawCircle(Color.White, radius = 6.5f, center = Offset(mx, my))
        drawCircle(lineColor, radius = 6.5f, center = Offset(mx, my), style = Stroke(width = 2.5f))

        // --- Çykgyt ---
        drawTooltip(measurer, "%.1f Mbps".format(value), mx, my, tooltipBg, size.width)
    }
}

private fun DrawScope.drawYLabel(
    measurer: TextMeasurer,
    text: String,
    leftPad: Float,
    y: Float,
    color: Color
) {
    val layout = measurer.measure(text, TextStyle(fontSize = 9.sp, color = color))
    drawText(
        layout,
        topLeft = Offset(leftPad - layout.size.width - 6f, y - layout.size.height / 2f)
    )
}

private fun DrawScope.drawTooltip(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    bg: Color,
    canvasWidth: Float
) {
    val layout = measurer.measure(text, TextStyle(fontSize = 10.sp, color = Color.White))
    val padX = 9f
    val padY = 5f
    val w = layout.size.width + padX * 2
    val h = layout.size.height + padY * 2
    val left = (x - w / 2).coerceIn(2f, canvasWidth - w - 2f)
    val top = (y - h - 14f).coerceAtLeast(2f)

    drawRoundRect(
        color = bg,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f)
    )
    drawText(layout, topLeft = Offset(left + padX, top + padY))
}

/** Katmull-Rom esasly ýumşak egri. */
private fun smoothPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    if (pts.size < 3) {
        pts.drop(1).forEach { path.lineTo(it.x, it.y) }
        return path
    }
    for (i in 0 until pts.size - 1) {
        val p0 = pts[(i - 1).coerceAtLeast(0)]
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = pts[(i + 2).coerceAtMost(pts.lastIndex)]
        val c1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
        val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}
