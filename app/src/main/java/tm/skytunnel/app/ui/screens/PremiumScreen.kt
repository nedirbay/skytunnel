package tm.skytunnel.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tm.skytunnel.app.ui.components.SkyCard
import tm.skytunnel.app.ui.components.SkyTopBar
import tm.skytunnel.app.ui.components.WidePromoBanner
import tm.skytunnel.app.ui.theme.GreenConnected
import tm.skytunnel.app.ui.theme.Violet

/** 07_Promo_Banner dizaýnyna esaslanan Premium sahypasy. */
@Composable
fun PremiumScreen(
    isPremium: Boolean,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val perks = listOf(
        "Access to all 24 servers worldwide",
        "Unlimited bandwidth, no throttling",
        "Up to 5 devices at the same time",
        "Ad-free experience",
        "Priority 24/7 support"
    )

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        SkyTopBar(title = "Premium", onBack = onBack)

        WidePromoBanner(
            onUpgrade = onUpgrade,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Access the open internet with no limits.",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        SkyCard(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                perks.forEach { perk ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = GreenConnected,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            perk,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onUpgrade,
            enabled = !isPremium,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Violet,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(54.dp)
        ) {
            Text(
                if (isPremium) "You are Premium" else "Upgrade — 4.99 / month",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(30.dp))
    }
}
