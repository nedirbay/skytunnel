package tm.skytunnel.app.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tm.skytunnel.app.data.Server
import tm.skytunnel.app.data.VpnUiState
import tm.skytunnel.app.ui.components.CountryFlag
import tm.skytunnel.app.ui.components.MetricBlock
import tm.skytunnel.app.ui.components.PowerButton
import tm.skytunnel.app.ui.components.PremiumChip
import tm.skytunnel.app.ui.components.SignalBars
import tm.skytunnel.app.ui.components.SkyCard
import tm.skytunnel.app.ui.components.VerticalDivider
import tm.skytunnel.app.ui.components.WorldMapBackground
import tm.skytunnel.app.ui.theme.Violet

/** 02_Home_Screen */
@Composable
fun HomeScreen(
    state: VpnUiState,
    onToggleConnection: () -> Unit,
    onServerClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {

        WorldMapBackground(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(top = 150.dp)
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // --- Ýokarky hatar ---
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!state.isPremium) {
                    PremiumChip(onClick = onPremiumClick)
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                "SkyTunnel",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Secure your connection\nanywhere, anytime",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // --- Güýç düwmesi ---
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PowerButton(
                    status = state.status,
                    statusLabel = state.statusLabel,
                    onClick = onToggleConnection
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                when {
                    state.isConnected -> state.timerText
                    state.isBusy -> "Please wait"
                    else -> "Tap to connect"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // --- Saýlanan serwer ---
            SelectedServerCard(
                server = state.selectedServer,
                connected = state.isConnected,
                onClick = onServerClick
            )

            Spacer(Modifier.height(12.dp))

            // --- Tizlik ---
            SkyCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricBlock(
                        icon = Icons.Filled.ArrowDownward,
                        label = "Download",
                        value = "%.1f Mbps".format(state.downloadMbps),
                        accent = Violet,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider()
                    Spacer(Modifier.width(16.dp))
                    MetricBlock(
                        icon = Icons.Filled.ArrowUpward,
                        label = "Upload",
                        value = "%.1f Mbps".format(state.uploadMbps),
                        accent = Violet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Baglanyşyk jikme-jiklikleri ---
            SkyCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.isConnected, onClick = onDetailsClick)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = Violet,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Connection Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SelectedServerCard(
    server: Server,
    connected: Boolean,
    onClick: () -> Unit
) {
    SkyCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountryFlag(server.countryCode, width = 48.dp, height = 34.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    server.country,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    server.city,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SignalBars(server.signal, active = connected)
        }
    }
}
