package con.open.tvos.ui.screens.player

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
 * Player Screen - Video player for VOD content
 * TODO: Integrate with ExoPlayer/IJKPlayer
 */
@Composable
fun PlayerScreen(
    vodId: String,
    episodeIndex: Int,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "播放器\nVOD ID: $vodId\n剧集: $episodeIndex",
            color = TextPrimary
        )
        // TODO: Implement actual player
        // Will integrate with existing player module
    }
}
