package tm.skytunnel.app.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import tm.skytunnel.app.data.VpnUiState
import tm.skytunnel.app.ui.components.SkyCard
import tm.skytunnel.app.ui.components.SkyTopBar
import tm.skytunnel.app.ui.theme.OfflineGrey
import tm.skytunnel.app.ui.theme.ThemeMode
import tm.skytunnel.app.ui.theme.Violet

/** 05_Settings_Screen */
@Composable
fun SettingsScreen(
    state: VpnUiState,
    onBack: () -> Unit,
    onAutoConnect: (Boolean) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onKillSwitch: (Boolean) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onAccount: () -> Unit,
    onSplitTunneling: () -> Unit,
    modifier: Modifier = Modifier
) {
    var themeDialog by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        SkyTopBar(title = "Settings", onBack = onBack)

        Spacer(Modifier.height(4.dp))

        SkyCard(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                NavRow(
                    icon = Icons.Filled.Person,
                    title = "Account",
                    value = if (state.isPremium) "Premium" else "Free Plan",
                    onClick = onAccount
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Filled.Speed,
                    title = "Auto Connect",
                    checked = state.autoConnect,
                    onChange = onAutoConnect
                )
                RowDivider()
                NavRow(
                    icon = Icons.AutoMirrored.Filled.AltRoute,
                    title = "Split Tunneling",
                    value = null,
                    onClick = onSplitTunneling
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    checked = state.notifications,
                    onChange = onNotifications
                )
                RowDivider()
                NavRow(
                    icon = Icons.Filled.DarkMode,
                    title = "Appearance",
                    value = when (state.themeMode) {
                        ThemeMode.SYSTEM -> "System"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    onClick = { themeDialog = true }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        SkyCard(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                ToggleRow(
                    icon = Icons.Filled.Shield,
                    title = "Kill Switch",
                    subtitle = "Block traffic if the VPN drops",
                    checked = state.killSwitch,
                    onChange = onKillSwitch
                )
                RowDivider()
                NavRow(
                    icon = Icons.Filled.Https,
                    title = "Protocol",
                    value = "WireGuard",
                    onClick = {}
                )
                RowDivider()
                NavRow(
                    icon = Icons.Filled.Info,
                    title = "About",
                    value = "v1.0",
                    onClick = {}
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }

    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text("Appearance") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeMode(mode)
                                    themeDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = {
                                    onThemeMode(mode)
                                    themeDialog = false
                                }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { themeDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(
    color = MaterialTheme.colorScheme.outline,
    modifier = Modifier.padding(start = 54.dp)
)

@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon)
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = Violet
            )
            Spacer(Modifier.width(4.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Violet,
                checkedBorderColor = Violet,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = OfflineGrey,
                uncheckedBorderColor = OfflineGrey
            )
        )
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(30.dp)
            .background(Violet.copy(alpha = 0.10f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(17.dp))
    }
    Spacer(Modifier.width(12.dp))
}
