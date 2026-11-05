package tm.skytunnel.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tm.skytunnel.app.ui.theme.Gold
import tm.skytunnel.app.ui.theme.VioletDeep
import tm.skytunnel.app.ui.theme.VioletLight

/**
 * "Go Premium" banneri (04_Connected_Screen dizaýnyndaky aşaky blok).
 */
@Composable
fun PromoBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Go Premium",
    subtitle: String = "Unlock more servers,\nenjoy higher speed!"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(listOf(VioletLight, VioletDeep))
            )
            .clickable(onClick = onClick)
    ) {
        // Fondaky ýagtylyk halkalary
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.3f),
                    radius = size.height * 1.1f
                ),
                radius = size.height * 1.1f,
                center = Offset(size.width * 0.82f, size.height * 0.3f)
            )
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(38.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Box(
                Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Giň marketing banneri (07_Promo_Banner dizaýny) — Premium sahypasynyň başynda.
 */
@Composable
fun WidePromoBanner(
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF4B2BD6), Color(0xFF1D2BA8)))
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Sagdaky planeta şöhlesi
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF6FA8FF).copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.55f),
                    radius = size.height * 0.95f
                ),
                radius = size.height * 0.95f,
                center = Offset(size.width * 0.88f, size.height * 0.55f)
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShieldLogo(size = 42.dp)
                Spacer(Modifier.width(10.dp))
                Row {
                    Text(
                        "Sky",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Text(
                        "Tunnel",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFF6FD2FF)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Fast. Secure. Unlimited.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
                    .clickable(onClick = onUpgrade)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Upgrade to Premium",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
