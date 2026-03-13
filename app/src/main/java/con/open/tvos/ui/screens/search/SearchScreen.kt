package con.open.tvos.ui.screens.search

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
 * Search Screen - Search for VOD content
 * TODO: Implement full search functionality
 */
@Composable
fun SearchScreen(
    onMovieClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "搜索\n(待实现)",
            color = TextPrimary
        )
        // TODO: Implement search with keyboard input
    }
}
