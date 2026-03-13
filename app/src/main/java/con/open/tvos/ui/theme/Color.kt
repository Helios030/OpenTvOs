package con.open.tvos.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * TVBox Color Palette for Android TV
 * Designed for TV screens with high contrast and readability
 */

// Primary Colors - Deep blue tones for cinema feel
val Primary = Color(0xFF1E88E5)
val PrimaryDark = Color(0xFF1565C0)
val PrimaryLight = Color(0xFF42A5F5)

// Secondary Colors - Warm amber for accents
val Secondary = Color(0xFFFFB300)
val SecondaryDark = Color(0xFFFF8F00)
val SecondaryLight = Color(0xFFFFD54F)

// Background Colors - Dark theme optimized for TV
val Background = Color(0xFF0D0D0D)
val BackgroundDark = Color(0xFF000000)
val BackgroundElevated = Color(0xFF1A1A1A)
val Surface = Color(0xFF1A1A1A)
val SurfaceElevated = Color(0xFF262626)

// Card Colors
val CardBackground = Color(0xFF1F1F1F)
val CardFocused = Color(0xFF2D2D2D)
val CardSelected = Color(0xFF333333)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB3B3B3)
val TextTertiary = Color(0xFF808080)
val TextDisabled = Color(0xFF4D4D4D)

// Status Colors
val Success = Color(0xFF4CAF50)
val Warning = Color(0xFFFF9800)
val Error = Color(0xFFF44336)
val Info = Color(0xFF2196F3)

// Focus Colors - Important for TV D-pad navigation
val FocusOuter = Color(0xFF1E88E5)
val FocusInner = Color(0xFF42A5F5)
val FocusGlow = Color(0x331E88E5)

// Gradient Colors for cards/posters
val GradientStart = Color(0xCC000000)
val GradientEnd = Color(0x00000000)

// Live TV specific colors
val LiveRed = Color(0xFFE53935)
val LiveBadge = Color(0xFFFF1744)

// Rating colors
val RatingHigh = Color(0xFF4CAF50)
val RatingMedium = Color(0xFFFF9800)
val RatingLow = Color(0xFFF44336)

/**
 * Dark Color Scheme for TVBox
 */
val TVBoxDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = TextPrimary,
    secondary = Secondary,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = Color.White,
    outline = TextTertiary,
    outlineVariant = CardBackground
)

/**
 * Get rating color based on score
 */
fun getRatingColor(score: Float): Color {
    return when {
        score >= 8.0f -> RatingHigh
        score >= 6.0f -> RatingMedium
        else -> RatingLow
    }
}
