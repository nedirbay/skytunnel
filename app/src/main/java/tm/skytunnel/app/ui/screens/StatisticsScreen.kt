package tm.skytunnel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tm.skytunnel.app.data.StatsRange
import tm.skytunnel.app.data.StatsRepository
import tm.skytunnel.app.data.VpnUiState
import tm.skytunnel.app.ui.components.SkyCard
import tm.skytunnel.app.ui.components.SkyTopBar
import tm.skytunnel.app.ui.components.SpeedChart
import tm.skytunnel.app.ui.theme.GreenConnected
import tm.skytunnel.app.ui.theme.Violet

/** 06_Statistics_Screen */
@Composable
fun StatisticsScreen(
    state: VpnUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var range by remember { mutableStateOf(StatsRange.TODAY) }
    val summary = remember(range) { StatsRepository.summaryFor(range) }
    var selected by remember { mutableIntStateOf(summary.peakIndex) }

    LaunchedEffect(range) { selected = summary.peakIndex }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        SkyTopBar(title = "Statistics", onBack = onBack)

        // --- Döwür saýlawy (segment) ---
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp)
        ) {
            StatsRange.entries.forEach { r ->
                val isSelected = r == range
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (isSelected) Violet else Color.Transparent)
                        .clickable { range = r }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        r.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Grafik ---
        SkyCard(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(170.dp)
        ) {
            Box(Modifier.padding(10.dp)) {
                SpeedChart(
                    points = summary.points,
                    selectedIndex = selected,
                    onSelect = { selected = it }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- Jemler ---
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TotalCard(
                icon = Icons.Filled.ArrowDownward,
                label = "Total Download",
                value = summary.totalDownload,
                accent = Violet,
                modifier = Modifier.weight(1f)
            )
            TotalCard(
                icon = Icons.Filled.ArrowUpward,
                label = "Total Upload",
                value = summary.totalUpload,
                accent = Violet,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TotalCard(
                icon = Icons.Filled.Schedule,
                label = "Session Time",
                value = if (state.isConnected) state.timerText else "00:00:00",
                accent = GreenConnected,
                modifier = Modifier.weight(1f)
            )
            TotalCard(
                icon = Icons.Filled.Shield,
                label = "Peak Speed",
                value = "%.1f Mbps".format(summary.peakMbps),
                accent = GreenConnected,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TotalCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    SkyCard(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
