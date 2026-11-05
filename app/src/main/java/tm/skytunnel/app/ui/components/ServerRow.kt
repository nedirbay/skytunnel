package tm.skytunnel.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tm.skytunnel.app.data.Server
import tm.skytunnel.app.ui.theme.GreenConnected
import tm.skytunnel.app.ui.theme.GreenSoft
import tm.skytunnel.app.ui.theme.OfflineGrey

/** "Servers" sahypasyndaky bir serwer setiri. */
@Composable
fun ServerRow(
    server: Server,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.3f
    val bg by animateColorAsState(
        targetValue = when {
            selected && isDark -> Color(0xFF14341F)
            selected -> GreenSoft
            else -> MaterialTheme.colorScheme.surface
        },
        label = "rowBg"
    )

    SkyCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = bg,
        shape = RoundedCornerShape(16.dp),
        border = if (selected) BorderStroke(1.5.dp, GreenConnected) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Saýlaw belgisi
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .background(GreenConnected, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .size(20.dp)
                            .border(1.5.dp, OfflineGrey.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))
            CountryFlag(server.countryCode, width = 40.dp, height = 29.dp)
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    server.country,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    server.city,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StatusDot(server.isOnline)
            Spacer(Modifier.width(12.dp))
            SignalBars(server.signal, active = server.isOnline)
        }
    }
}
