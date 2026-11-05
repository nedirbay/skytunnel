package tm.skytunnel.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tm.skytunnel.app.ui.components.ShieldLogo
import tm.skytunnel.app.ui.theme.SkyBlue
import tm.skytunnel.app.ui.theme.SplashBottom
import tm.skytunnel.app.ui.theme.SplashMid
import tm.skytunnel.app.ui.theme.SplashTop

/**
 * 01_Splash_Screen — kosmosdan görünýän Ýer şary, galkan logotipi we ýükleýji.
 * [onFinished] 2.6 sekuntdan soň çagyrylýar.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(2600)
        onFinished()
    }

    val logoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.82f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "logoScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(900),
        label = "contentAlpha"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SplashTop, SplashMid, SplashBottom)
                )
            )
    ) {
        EarthHorizon(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(140.dp))

            ShieldLogo(
                size = 118.dp,
                modifier = Modifier.scale(logoScale).alpha(contentAlpha)
            )

            Spacer(Modifier.height(26.dp))

            Row(Modifier.alpha(contentAlpha)) {
                Text(
                    "Sky",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "Tunnel",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SkyBlue
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "Fast. Secure. Unlimited.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(contentAlpha)
            )

            Spacer(Modifier.weight(1f))

            LoadingSpinner()
            Spacer(Modifier.height(14.dp))
            Text(
                "Connecting to a better internet...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** Aşakdaky Ýer şarynyň egri gyrasy we atmosfera şöhlesi. */
@Composable
private fun EarthHorizon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = w * 1.35f
        val cx = w / 2f
        val cy = h * 0.72f + r

        // Atmosfera halkasy
        drawCircle(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF4FC3F7).copy(alpha = 0.55f), Color.Transparent),
                startY = cy - r - h * 0.08f,
                endY = cy - r + h * 0.25f
            ),
            radius = r + h * 0.035f,
            center = Offset(cx, cy)
        )

        // Planetanyň jisimi
        drawCircle(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0D1B5E), Color(0xFF060B33)),
                startY = cy - r,
                endY = cy + r * 0.2f
            ),
            radius = r,
            center = Offset(cx, cy)
        )

        // Gyrasyndaky ýagty çyzyk
        drawCircle(
            color = Color(0xFF8FD8FF).copy(alpha = 0.75f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 2.2f)
        )

        // Şäher yşyklary
        val rnd = java.util.Random(42)
        repeat(90) {
            val ax = rnd.nextFloat() * w
            val depth = rnd.nextFloat()
            val ay = h * 0.74f + depth * h * 0.22f
            val dx = ax - cx
            val limit = cy - kotlin.math.sqrt((r * r - dx * dx).coerceAtLeast(0f))
            if (ay > limit) {
                drawCircle(
                    color = Color(0xFFFFC66B).copy(alpha = 0.25f + rnd.nextFloat() * 0.6f),
                    radius = 0.8f + rnd.nextFloat() * 1.5f,
                    center = Offset(ax, ay)
                )
            }
        }

        // Orbital ýaý
        drawArc(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color(0xFF9AD8FF).copy(alpha = 0.5f), Color.Transparent)
            ),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - r * 1.05f, cy - r * 1.05f),
            size = androidx.compose.ui.geometry.Size(r * 2.1f, r * 2.1f),
            style = Stroke(width = 1.6f)
        )
    }
}

/** Splash-daky aýlanýan ýükleýji halka. */
@Composable
private fun LoadingSpinner() {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    Canvas(Modifier.size(26.dp)) {
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            style = Stroke(width = 3f)
        )
        drawArc(
            color = Color.White,
            startAngle = angle,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }
}
