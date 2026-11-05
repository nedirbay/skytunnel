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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import tm.skytunnel.app.data.Region
import tm.skytunnel.app.data.Server
import tm.skytunnel.app.ui.components.FilterChipPill
import tm.skytunnel.app.ui.components.ServerRow
import tm.skytunnel.app.ui.components.SkyTopBar
import tm.skytunnel.app.ui.theme.Violet

/** 03_Servers_Screen */
@Composable
fun ServersScreen(
    servers: List<Server>,
    selectedId: String?,
    isRefreshing: Boolean,
    onSelect: (Server) -> Unit,
    onRefresh: () -> Unit,
    onAddSubscription: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var region by remember { mutableStateOf<Region?>(null) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    val refreshing = isRefreshing

    val all = servers
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
                val count = all.count { it.region == r }
                if (count > 0) {
                    FilterChipPill(
                        label = "${r.label} ($count)",
                        selected = region == r,
                        onClick = { region = r }
                    )
                }
            }
            FilterChipPill(
                label = "+ Add",
                selected = false,
                onClick = { addDialog = true }
            )
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
                        if (all.isEmpty()) "Add your subscription link to see servers"
                        else "No servers match your search",
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
            onClick = { if (all.isEmpty()) addDialog = true else onRefresh() },
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
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp)
        ) {
            Icon(
                if (all.isEmpty()) Icons.Filled.Add else Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (refreshing) spin else 0f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                when {
                    refreshing -> "Refreshing..."
                    all.isEmpty() -> "Add subscription"
                    else -> "Refresh"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    if (addDialog) {
        AddSubscriptionDialog(
            onDismiss = { addDialog = false },
            onAdd = {
                addDialog = false
                onAddSubscription(it)
            }
        )
    }
}

@Composable
private fun AddSubscriptionDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add subscription") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("https:// or vless:// link") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(text) }, enabled = text.isNotBlank()) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
