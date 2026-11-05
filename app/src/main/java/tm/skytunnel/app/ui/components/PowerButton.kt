package tm.skytunnel.app.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import tm.skytunnel.app.data.VpnStatus
import tm.skytunnel.app.ui.theme.GreenConnected
import tm.skytunnel.app.ui.theme.GreenRing
import tm.skytunnel.app.ui.theme.OfflineGrey
import tm.skytunnel.app.ui.theme.Violet

/**
 * Esasy sahypanyň merkezindäki uly güýç düwmesi.
 * Üç ýagdaýy bar: üzülen (çal), birigýär (aýlanýan ýaý), bagly (ýaşyl halka).
 */
@Composable
fun PowerButton(
    status: VpnStatus,
    statusLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connected = status == VpnStatus.CONNECTED
    val connecting = status == VpnStatus.CONNECTING

    val accent by animateColorAsState(
        targetValue = when (status) {
            VpnStatus.CONNECTED -> GreenConnected
            VpnStatus.CONNECTING -> Violet
            VpnStatus.DISCONNECTED -> OfflineGrey
            VpnStatus.ERROR -> MaterialTheme.colorScheme.error
        },
        animationSpec = tween(400),
        label = "accent"
    )

    val transition = rememberInfiniteTransition(label = "power")

    // Bagly wagty dem alýan ýaly giňelýän halka
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Birigýän wagty aýlanýan ýaý
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    val innerScale by animateFloatAsState(
        targetValue = if (connected) 1f else 0.97f,
        animationSpec = tween(300),
        label = "innerScale"
    )

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(200.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Daşky şöhle
            if (connected) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(GreenRing.copy(alpha = 0.30f), Color.Transparent),
                        center = center,
                        radius = r * pulse
                    ),
                    radius = r * pulse,
                    center = center
                )
            }

            val ringR = r * 0.82f
            val stroke = r * 0.09f

            // Boş halka
            drawCircle(
                color = accent.copy(alpha = 0.18f),
                radius = ringR,
                center = center,
                style = Stroke(width = stroke)
            )

            // Progres halkasy
            val topLeft = Offset(center.x - ringR, center.y - ringR)
            val arcSize = Size(ringR * 2, ringR * 2)
            when {
                connecting -> drawArc(
                    color = accent,
                    startAngle = spin,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                connected -> drawArc(
                    brush = Brush.sweepGradient(
                        listOf(GreenRing, GreenConnected, GreenRing),
                        center = center
                    ),
                    startAngle = 130f,
                    sweepAngle = 280f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        // Içindäki ak tegelek + güýç nyşany
        Box(
            Modifier
                .size((132 * innerScale).dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    contentDescription = if (connected || connecting) "Disconnect" else "Connect",
                    tint = accent,
                    modifier = Modifier.size(62.dp)
                )
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = accent
                )
            }
        }
    }
}
