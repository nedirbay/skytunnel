package tm.skytunnel.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** SkyTunnel-iň galkan + ýyldyrym logotipi (splash we promo üçin). */
@Composable
fun ShieldLogo(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp,
    shieldTop: Color = Color(0xFF7CE0FF),
    shieldBottom: Color = Color(0xFF3B5BFF),
    boltColor: Color = Color.White
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val shield = Path().apply {
            moveTo(w * 0.50f, h * 0.06f)
            lineTo(w * 0.90f, h * 0.22f)
            lineTo(w * 0.90f, h * 0.56f)
            cubicTo(
                w * 0.90f, h * 0.78f,
                w * 0.72f, h * 0.90f,
                w * 0.50f, h * 0.97f
            )
            cubicTo(
                w * 0.28f, h * 0.90f,
                w * 0.10f, h * 0.78f,
                w * 0.10f, h * 0.56f
            )
            lineTo(w * 0.10f, h * 0.22f)
            close()
        }

        drawPath(
            shield,
            Brush.verticalGradient(
                listOf(shieldTop, shieldBottom),
                startY = 0f,
                endY = h
            )
        )

        // Içki ýagtylyk
        drawPath(
            shield,
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(w * 0.38f, h * 0.28f),
                radius = w * 0.55f
            )
        )

        val bolt = Path().apply {
            moveTo(w * 0.58f, h * 0.24f)
            lineTo(w * 0.36f, h * 0.55f)
            lineTo(w * 0.49f, h * 0.55f)
            lineTo(w * 0.42f, h * 0.80f)
            lineTo(w * 0.66f, h * 0.47f)
            lineTo(w * 0.52f, h * 0.47f)
            close()
        }
        drawPath(bolt, boltColor)
    }
}
