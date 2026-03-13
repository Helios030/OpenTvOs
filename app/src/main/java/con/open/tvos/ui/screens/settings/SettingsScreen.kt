package con.open.tvos.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import con.open.tvos.ui.theme.Background
import con.open.tvos.ui.theme.TextPrimary

/**
 * Settings Screen - App settings
 * TODO: Implement full settings functionality
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "设置\n(待实现)",
            color = TextPrimary
        )
        // TODO: Implement settings
    }
}
