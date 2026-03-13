package con.open.tvos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * TVBox Theme Composable
 * Provides the dark theme optimized for Android TV screens
 *
 * Note: Android TV apps should always use dark theme for:
 * 1. Better visibility in dimly lit living room environments
 * 2. Reduced eye strain during extended viewing
 * 3. Cinema-like aesthetic experience
 */
@Composable
fun TVBoxTheme(
    // Always use dark theme for TV
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = TVBoxDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TVBoxTypography,
        shapes = TVBoxShapes,
        content = content
    )
}

/**
 * Preview wrapper for Compose previews
 */
@Composable
fun TVBoxPreview(
    content: @Composable () -> Unit
) {
    TVBoxTheme {
        content()
    }
}
