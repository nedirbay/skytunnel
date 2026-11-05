package tm.skytunnel.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tm.skytunnel.app.data.Region
import tm.skytunnel.app.data.Server
import tm.skytunnel.app.data.ServerRepository
import tm.skytunnel.app.ui.components.FilterChipPill
import tm.skytunnel.app.ui.components.ServerRow
import tm.skytunnel.app.ui.components.SkyTopBar
import tm.skytunnel.app.ui.theme.Violet

/** 03_Servers_Screen */
@Composable
fun ServersScreen(
    selectedId: String,
    onSelect: (Server) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var region by remember { mutableStateOf<Region?>(null) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(refreshing) {
        if (refreshing) {
            delay(1200)
            refreshing = false
        }
    }

    val all = ServerRepository.servers
    val visible = all
        .filter { region == null || it.region == region }
        .filter {
            query.isBlank() ||
                it.country.contains(query, true) ||
                it.city.contains(query, true)
        }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SkyTopBar(
            title = "Servers",
            onBack = onBack,
            actionIcon = if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
            actionDescription = "Search",
            onAction = {
                searchOpen = !searchOpen
                if (!searchOpen) query = ""
            }
        )

        AnimatedVisibility(visible = searchOpen) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search country or city") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Violet,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // --- Filtr çipleri ---
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipPill(
                label = "All (${all.size})",
                selected = region == null,
                onClick = { region = null }
            )
            Region.entries.forEach { r ->
                FilterChipPill(
                    label = "${r.label} (${ServerRepository.count(r)})",
                    selected = region == r,
                    onClick = { region = r }
                )
            }
        }

        // --- Sanaw ---
        Box(Modifier.weight(1f)) {
            if (visible.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No servers match your search",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visible, key = { it.id }) { server ->
                        ServerRow(
                            server = server,
                            selected = server.id == selectedId,
                            onClick = { onSelect(server) }
                        )
                    }
                }
            }
        }

        // --- Refresh düwmesi ---
        val spinTransition = rememberInfiniteTransition(label = "refresh")
        val spin by spinTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin"
        )

        Button(
            onClick = { refreshing = true },
            enabled = !refreshing,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Violet,
                contentColor = Color.White,
                disabledContainerColor = Violet.copy(alpha = 0.7f),
                disabledContentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp)
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (refreshing) spin else 0f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (refreshing) "Refreshing..." else "Refresh",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
