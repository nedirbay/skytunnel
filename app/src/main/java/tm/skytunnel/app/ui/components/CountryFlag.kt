package tm.skytunnel.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ýurt baýdagyny Canvas arkaly çyzýar — surat faýllary gerek däl,
 * islendik ölçegde arassa görünýär.
 */
@Composable
fun CountryFlag(
    code: String,
    modifier: Modifier = Modifier,
    width: Dp = 44.dp,
    height: Dp = 32.dp,
    corner: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(corner))
    ) {
        Canvas(Modifier.size(width, height)) {
            when (code.uppercase()) {
                "TM" -> drawTurkmenistan()
                "TR" -> drawTurkey()
                "DE" -> drawHorizontalBands(listOf(Color(0xFF111111), Color(0xFFDD0000), Color(0xFFFFCE00)))
                "NL" -> drawHorizontalBands(listOf(Color(0xFFAE1C28), Color.White, Color(0xFF21468B)))
                "FR" -> drawVerticalBands(listOf(Color(0xFF0055A4), Color.White, Color(0xFFEF4135)))
                "US" -> drawUnitedStates()
                "GB" -> drawUnitedKingdom()
                "CA" -> drawCanada()
                "JP" -> drawJapan()
                "IT" -> drawVerticalBands(listOf(Color(0xFF009246), Color.White, Color(0xFFCE2B37)))
                "PL" -> drawHorizontalBands(listOf(Color.White, Color(0xFFDC143C)))
                "ID" -> drawHorizontalBands(listOf(Color(0xFFCE1126), Color.White))
                "IN" -> drawIndia()
                "TH" -> drawThailand()
                "ES" -> drawSpain()
                "SE" -> drawSweden()
                "CH" -> drawSwitzerland()
                "SG" -> drawSingapore()
                "AE" -> drawUae()
                "BR" -> drawBrazil()
                "MX" -> drawMexico()
                else -> drawFallback(code)
            }
        }
    }
}

// --- Ýönekeý zolakly baýdaklar ----------------------------------------------

private fun DrawScope.drawHorizontalBands(colors: List<Color>) {
    val band = size.height / colors.size
    colors.forEachIndexed { i, c ->
        drawRect(c, topLeft = Offset(0f, band * i), size = Size(size.width, band))
    }
}

private fun DrawScope.drawVerticalBands(colors: List<Color>) {
    val band = size.width / colors.size
    colors.forEachIndexed { i, c ->
        drawRect(c, topLeft = Offset(band * i, 0f), size = Size(band, size.height))
    }
}

// --- Türkmenistan ------------------------------------------------------------

private fun DrawScope.drawTurkmenistan() {
    val green = Color(0xFF28AE66)
    val carpetRed = Color(0xFFC1272D)
    val white = Color.White

    drawRect(green)

    // Haly zolagy (çep tarapda)
    val stripeX = size.width * 0.17f
    val stripeW = size.width * 0.18f
    drawRect(carpetRed, topLeft = Offset(stripeX, 0f), size = Size(stripeW, size.height))

    // Haly gölleriniň shematik şekili
    val cells = 5
    val cellH = size.height / cells
    repeat(cells) { i ->
        val cy = cellH * i + cellH / 2f
        drawRect(
            white,
            topLeft = Offset(stripeX + stripeW * 0.22f, cy - cellH * 0.18f),
            size = Size(stripeW * 0.56f, cellH * 0.36f)
        )
    }

    // Ýarymaý
    val cx = size.width * 0.58f
    val cy = size.height * 0.36f
    val r = size.height * 0.20f
    val crescent = Path().apply {
        addOval(Rect(Offset(cx, cy), r))
    }
    val cut = Path().apply {
        addOval(Rect(Offset(cx + r * 0.42f, cy - r * 0.05f), r * 0.92f))
    }
    drawPath(Path().apply { op(crescent, cut, PathOperation.Difference) }, white)

    // Bäş ýyldyz
    val starR = size.height * 0.055f
    listOf(
        0.78f to 0.22f, 0.86f to 0.36f, 0.82f to 0.52f,
        0.70f to 0.58f, 0.64f to 0.42f
    ).forEach { (fx, fy) ->
        drawStar(Offset(size.width * fx, size.height * fy), starR, white)
    }
}

// --- Türkiýe -----------------------------------------------------------------

private fun DrawScope.drawTurkey() {
    val red = Color(0xFFE30A17)
    drawRect(red)

    val cx = size.width * 0.38f
    val cy = size.height * 0.5f
    val r = size.height * 0.26f
    val crescent = Path().apply { addOval(Rect(Offset(cx, cy), r)) }
    val cut = Path().apply { addOval(Rect(Offset(cx + r * 0.40f, cy), r * 0.82f)) }
    drawPath(Path().apply { op(crescent, cut, PathOperation.Difference) }, Color.White)
    drawStar(Offset(size.width * 0.62f, cy), size.height * 0.13f, Color.White)
}

// --- ABŞ ---------------------------------------------------------------------

private fun DrawScope.drawUnitedStates() {
    val red = Color(0xFFB22234)
    val blue = Color(0xFF3C3B6E)
    drawRect(Color.White)

    val stripes = 7
    val h = size.height / 13f
    repeat(stripes) { i ->
        drawRect(red, topLeft = Offset(0f, h * 2 * i), size = Size(size.width, h))
    }
    drawRect(blue, size = Size(size.width * 0.42f, size.height * 7f / 13f))

    val starR = size.height * 0.035f
    for (row in 0 until 4) {
        for (col in 0 until 5) {
            val x = size.width * 0.42f * (col + 0.5f) / 5f
            val y = size.height * (7f / 13f) * (row + 0.5f) / 4f
            drawStar(Offset(x, y), starR, Color.White)
        }
    }
}

// --- Birleşen Patyşalyk -------------------------------------------------------

private fun DrawScope.drawUnitedKingdom() {
    val blue = Color(0xFF012169)
    val red = Color(0xFFC8102E)
    val w = size.width
    val h = size.height
    drawRect(blue)

    // Diagonal ak zolaklar (saltire)
    val dw = h * 0.30f
    drawLine(Color.White, Offset(0f, 0f), Offset(w, h), dw)
    drawLine(Color.White, Offset(w, 0f), Offset(0f, h), dw)
    drawLine(red, Offset(0f, 0f), Offset(w, h), dw * 0.42f)
    drawLine(red, Offset(w, 0f), Offset(0f, h), dw * 0.42f)

    // Göni haç
    drawRect(Color.White, topLeft = Offset(0f, h / 2 - h * 0.18f), size = Size(w, h * 0.36f))
    drawRect(Color.White, topLeft = Offset(w / 2 - h * 0.18f, 0f), size = Size(h * 0.36f, h))
    drawRect(red, topLeft = Offset(0f, h / 2 - h * 0.10f), size = Size(w, h * 0.20f))
    drawRect(red, topLeft = Offset(w / 2 - h * 0.10f, 0f), size = Size(h * 0.20f, h))
}

// --- Kanada ------------------------------------------------------------------

private fun DrawScope.drawCanada() {
    val red = Color(0xFFD80621)
    drawRect(Color.White)
    drawRect(red, size = Size(size.width * 0.25f, size.height))
    drawRect(red, topLeft = Offset(size.width * 0.75f, 0f), size = Size(size.width * 0.25f, size.height))

    // Shematik ýaprak
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val s = size.height * 0.30f
    val leaf = Path().apply {
        moveTo(cx, cy - s)
        lineTo(cx + s * 0.32f, cy - s * 0.28f)
        lineTo(cx + s * 0.80f, cy - s * 0.44f)
        lineTo(cx + s * 0.56f, cy + s * 0.18f)
        lineTo(cx + s * 0.92f, cy + s * 0.40f)
        lineTo(cx + s * 0.18f, cy + s * 0.52f)
        lineTo(cx + s * 0.10f, cy + s)
        lineTo(cx - s * 0.10f, cy + s)
        lineTo(cx - s * 0.18f, cy + s * 0.52f)
        lineTo(cx - s * 0.92f, cy + s * 0.40f)
        lineTo(cx - s * 0.56f, cy + s * 0.18f)
        lineTo(cx - s * 0.80f, cy - s * 0.44f)
        lineTo(cx - s * 0.32f, cy - s * 0.28f)
        close()
    }
    drawPath(leaf, red)
}

// --- Ýaponiýa ----------------------------------------------------------------

private fun DrawScope.drawJapan() {
    drawRect(Color.White)
    drawCircle(Color(0xFFBC002D), radius = size.height * 0.30f, center = Offset(size.width / 2, size.height / 2))
}

// --- Beýleki ýönekeýleşdirilen baýdaklar --------------------------------------

private fun DrawScope.drawIndia() {
    drawHorizontalBands(listOf(Color(0xFFFF9933), Color.White, Color(0xFF138808)))
    drawCircle(
        Color(0xFF000080),
        radius = size.height * 0.13f,
        center = Offset(size.width / 2, size.height / 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.height * 0.03f)
    )
}

private fun DrawScope.drawThailand() {
    val h = size.height / 6f
    drawRect(Color(0xFFA51931))
    drawRect(Color.White, topLeft = Offset(0f, h), size = Size(size.width, h * 4))
    drawRect(Color(0xFF2D2A4A), topLeft = Offset(0f, h * 2), size = Size(size.width, h * 2))
}

private fun DrawScope.drawSpain() {
    val h = size.height / 4f
    drawRect(Color(0xFFAA151B))
    drawRect(Color(0xFFF1BF00), topLeft = Offset(0f, h), size = Size(size.width, h * 2))
}

private fun DrawScope.drawSweden() {
    val t = size.height * 0.2f
    drawRect(Color(0xFF006AA7))
    drawRect(Color(0xFFFECC00), topLeft = Offset(0f, (size.height - t) / 2), size = Size(size.width, t))
    drawRect(Color(0xFFFECC00), topLeft = Offset(size.width * 0.3f, 0f), size = Size(t, size.height))
}

private fun DrawScope.drawSwitzerland() {
    drawRect(Color(0xFFDA291C))
    val long = size.height * 0.6f
    val thick = size.height * 0.2f
    val c = Offset(size.width / 2, size.height / 2)
    drawRect(Color.White, topLeft = Offset(c.x - long / 2, c.y - thick / 2), size = Size(long, thick))
    drawRect(Color.White, topLeft = Offset(c.x - thick / 2, c.y - long / 2), size = Size(thick, long))
}

private fun DrawScope.drawSingapore() {
    val red = Color(0xFFEE2536)
    drawHorizontalBands(listOf(red, Color.White))
    val c = Offset(size.width * 0.24f, size.height * 0.25f)
    val r = size.height * 0.17f
    val crescent = Path().apply { addOval(Rect(c, r)) }
    val cut = Path().apply { addOval(Rect(Offset(c.x + r * 0.4f, c.y), r * 0.85f)) }
    drawPath(Path().apply { op(crescent, cut, PathOperation.Difference) }, Color.White)
}

private fun DrawScope.drawUae() {
    drawHorizontalBands(listOf(Color(0xFF00732F), Color.White, Color(0xFF111111)))
    drawRect(Color(0xFFFF0000), size = Size(size.width * 0.25f, size.height))
}

private fun DrawScope.drawBrazil() {
    drawRect(Color(0xFF009C3B))
    val w = size.width
    val h = size.height
    val diamond = Path().apply {
        moveTo(w / 2, h * 0.12f)
        lineTo(w * 0.92f, h / 2)
        lineTo(w / 2, h * 0.88f)
        lineTo(w * 0.08f, h / 2)
        close()
    }
    drawPath(diamond, Color(0xFFFFDF00))
    drawCircle(Color(0xFF002776), radius = h * 0.22f, center = Offset(w / 2, h / 2))
}

private fun DrawScope.drawMexico() {
    drawVerticalBands(listOf(Color(0xFF006847), Color.White, Color(0xFFCE1126)))
    drawCircle(Color(0xFF8C5A2B), radius = size.height * 0.11f, center = Offset(size.width / 2, size.height / 2))
}

// --- Näbelli kod -------------------------------------------------------------

private fun DrawScope.drawFallback(code: String) {
    drawRect(Color(0xFFCED3E8))
    drawRect(
        Color(0xFF9AA4C8),
        topLeft = Offset(0f, size.height * 0.38f),
        size = Size(size.width, size.height * 0.24f)
    )
}

// --- Kömekçi: bäşburçly ýyldyz ------------------------------------------------

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val inner = radius * 0.42f
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else inner
        val a = -PI / 2 + i * PI / 5
        val x = center.x + (r * cos(a)).toFloat()
        val y = center.y + (r * sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
