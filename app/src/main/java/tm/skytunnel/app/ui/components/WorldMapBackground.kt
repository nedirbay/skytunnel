package tm.skytunnel.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Esasy sahypanyň fonundaky nokatlardan ýasalan dünýä kartasy.
 * Materikleriň ýerleşişi gaty ýönekeý maska arkaly kesgitlenýär.
 */
@Composable
fun WorldMapBackground(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0xFF6C63C9),
    alpha: Float = 0.18f
) {
    Canvas(modifier) {
        val cols = 46
        val rows = 22
        val stepX = size.width / cols
        val stepY = size.height / rows
        val radius = (minOf(stepX, stepY) * 0.17f).coerceAtLeast(0.9f)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val u = c / cols.toFloat()
                val v = r / rows.toFloat()
                if (isLand(u, v)) {
                    drawCircle(
                        color = dotColor.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(c * stepX + stepX / 2, r * stepY + stepY / 2)
                    )
                }
            }
        }
    }
}

/** u: 0..1 (günbatar→gündogar), v: 0..1 (demirgazyk→günorta). */
private fun isLand(u: Float, v: Float): Boolean {
    // Demirgazyk Amerika
    if (u in 0.06f..0.26f && v in 0.14f..0.46f && (u + v * 0.5f) < 0.42f) return true
    // Merkezi Amerika
    if (u in 0.18f..0.25f && v in 0.46f..0.56f) return true
    // Günorta Amerika
    if (u in 0.24f..0.36f && v in 0.55f..0.90f && (u - v * 0.12f) < 0.33f) return true
    // Ýewropa
    if (u in 0.45f..0.57f && v in 0.18f..0.38f) return true
    // Afrika
    if (u in 0.46f..0.62f && v in 0.40f..0.80f) return true
    // Aziýa
    if (u in 0.57f..0.84f && v in 0.14f..0.46f) return true
    // Hindistan
    if (u in 0.66f..0.74f && v in 0.42f..0.54f) return true
    // Günorta-gündogar Aziýa
    if (u in 0.76f..0.86f && v in 0.46f..0.60f) return true
    // Awstraliýa
    if (u in 0.82f..0.94f && v in 0.64f..0.80f) return true
    return false
}
