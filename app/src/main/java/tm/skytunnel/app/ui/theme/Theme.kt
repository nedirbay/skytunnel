package tm.skytunnel.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = SurfaceVariant,
    onPrimaryContainer = Violet,
    secondary = GreenConnected,
    onSecondary = Color.White,
    secondaryContainer = GreenSoft,
    onSecondaryContainer = GreenDeep,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    error = Color(0xFFE53935)
)

private val DarkColors = darkColorScheme(
    primary = VioletLight,
    onPrimary = Color.White,
    primaryContainer = DarkSurfaceVar,
    onPrimaryContainer = Color.White,
    secondary = GreenConnected,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF123A24),
    onSecondaryContainer = GreenRing,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVar,
    onSurfaceVariant = DarkTextSecond,
    outline = Color(0xFF32325C),
    error = Color(0xFFEF5350)
)

/** Programmanyň "Appearance" sazlamasy: System / Light / Dark. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun SkyTunnelTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    forceLightStatusIcons: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !dark && !forceLightStatusIcons
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = SkyTypography,
        content = content
    )
}
