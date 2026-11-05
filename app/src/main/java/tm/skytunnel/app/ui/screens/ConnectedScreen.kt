package tm.skytunnel.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tm.skytunnel.app.data.VpnUiState
import tm.skytunnel.app.ui.components.CountryFlag
import tm.skytunnel.app.ui.components.PromoBanner
import tm.skytunnel.app.ui.components.SignalBars
import tm.skytunnel.app.ui.components.SkyCard
import tm.skytunnel.app.ui.components.SkyTopBar
import tm.skytunnel.app.ui.theme.GreenConnected
import tm.skytunnel.app.ui.theme.GreenDeep
import tm.skytunnel.app.ui.theme.GreenRing
import tm.skytunnel.app.ui.theme.Violet

/** 04_Connected_Screen — baglanyşygyň jikme-jik sahypasy. */
@Composable
fun ConnectedScreen(
    state: VpnUiState,
    onBack: () -> Unit,
    onDisconnect: () -> Unit,
    onPremium: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        SkyTopBar(
            title = "Connected",
            onBack = onBack,
            actionIcon = Icons.Filled.MoreVert,
            actionDescription = "More"
        )

        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SuccessShield()
        }

        Spacer(Modifier.height(10.dp))

        Text(
            state.statusLabel,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
            color = GreenConnected,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            state.timerText,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(18.dp))

        // --- Serwer kartasy ---
        SkyCard(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountryFlag(state.selectedServer.countryCode, width = 48.dp, height = 34.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.selectedServer.country,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        state.selectedServer.city,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SignalBars(state.selectedServer.signal, active = state.isConnected)
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- Jikme-jiklikler ---
        SkyCard(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                DetailRow(Icons.Filled.Language, "Your IP", state.ipAddress)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                DetailRow(
                    Icons.Filled.Bolt,
                    "Connection Speed",
                    "%.1f Mbps".format(state.downloadMbps)
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                DetailRow(
                    Icons.Filled.DataUsage,
                    "Data Used",
                    "%.1f MB".format(state.dataUsedMb)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // --- Disconnect ---
        Button(
            onClick = onDisconnect,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = Violet
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp)
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Disconnect", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(14.dp))

        if (!state.isPremium) {
            PromoBanner(
                onClick = onPremium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Ýaşyl galkan + bellik nyşany, daşynda pulsirleýän şöhle. */
@Composable
private fun SuccessShield() {
    val transition = rememberInfiniteTransition(label = "shield")
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(Modifier.size(170.dp)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2, h / 2)

        drawCircle(
            brush = Brush.radialGradient(
                listOf(GreenRing.copy(alpha = 0.26f), Color.Transparent),
                center = center,
                radius = w / 2 * pulse
            ),
            radius = w / 2 * pulse,
            center = center
        )

        val s = w * 0.56f
        val left = center.x - s / 2
        val top = center.y - s / 2
        val shield = Path().apply {
            moveTo(left + s * 0.5f, top)
            lineTo(left + s, top + s * 0.20f)
            lineTo(left + s, top + s * 0.56f)
            cubicTo(
                left + s, top + s * 0.84f,
                left + s * 0.76f, top + s * 0.96f,
                left + s * 0.5f, top + s
            )
            cubicTo(
                left + s * 0.24f, top + s * 0.96f,
                left, top + s * 0.84f,
                left, top + s * 0.56f
            )
            lineTo(left, top + s * 0.20f)
            close()
        }
        drawPath(
            shield,
            Brush.verticalGradient(
                listOf(GreenRing, GreenDeep),
                startY = top,
                endY = top + s
            )
        )

        // Bellik (check)
        val check = Path().apply {
            moveTo(center.x - s * 0.20f, center.y + s * 0.02f)
            lineTo(center.x - s * 0.04f, center.y + s * 0.18f)
            lineTo(center.x + s * 0.24f, center.y - s * 0.17f)
        }
        drawPath(
            check,
            Color.White,
            style = Stroke(
                width = s * 0.11f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
