package tm.skytunnel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tm.skytunnel.app.ui.theme.GreenConnected
import tm.skytunnel.app.ui.theme.OfflineGrey

/** Dizaýndaky 4 sütünjikli signal görkezijisi. */
@Composable
fun SignalBars(
    level: Int,
    modifier: Modifier = Modifier,
    active: Boolean = true
) {
    val heights = listOf(7.dp, 11.dp, 15.dp, 19.dp)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        heights.forEachIndexed { i, h ->
            val filled = i < level
            val color = when {
                !filled -> OfflineGrey.copy(alpha = 0.30f)
                active -> GreenConnected
                else -> OfflineGrey.copy(alpha = 0.65f)
            }
            Spacer(
                Modifier
                    .width(4.dp)
                    .height(h)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}
