package con.open.tvos.ui.screens.live

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
 * Live TV Screen - Shows live TV channels with EPG
 * TODO: Implement full live TV functionality
 */
@Composable
fun LiveTvScreen(
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "直播电视\n(待实现)",
            color = TextPrimary
        )
        // TODO: Implement live TV with EPG
    }
}
