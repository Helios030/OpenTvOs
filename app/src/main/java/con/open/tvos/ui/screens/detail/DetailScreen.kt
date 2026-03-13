package con.open.tvos.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import con.open.tvos.ui.components.TvButton
import con.open.tvos.ui.components.TvPosterCard
import con.open.tvos.ui.components.TvRatingBadge
import con.open.tvos.ui.components.TvSectionTitle
import con.open.tvos.ui.components.TvWideCard
import con.open.tvos.ui.theme.Background
import con.open.tvos.ui.theme.TextPrimary
import con.open.tvos.ui.theme.TextSecondary

/**
 * Detail Screen - Shows movie/series details
 */
@Composable
fun DetailScreen(
    vodId: String,
    onPlayClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Load content when screen opens
    androidx.compose.runtime.LaunchedEffect(vodId) {
        viewModel.loadDetail(vodId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("加载中...", color = TextPrimary)
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("加载失败: ${state.error}", color = TextPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        TvButton(text = "返回", onClick = onBackClick)
                    }
                }
            }
            state.vodInfo != null -> {
                DetailContent(
                    state = state,
                    onPlayClick = onPlayClick,
                    onBackClick = onBackClick
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    state: DetailState,
    onPlayClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val vodInfo = state.vodInfo ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        // Back button
        item {
            TvButton(
                text = "← 返回",
                onClick = onBackClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Main info section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Poster
                TvPosterCard(
                    title = vodInfo.name,
                    imageUrl = vodInfo.pic,
                    onClick = {},
                    modifier = Modifier.width(200.dp)
                )

                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = vodInfo.name,
                        color = TextPrimary,
                        style = MaterialTheme.typography.displaySmall
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        vodInfo.year.takeIf { it > 0 }?.let {
                            Text("$it", color = TextSecondary)
                        }
                        vodInfo.area?.takeIf { it.isNotEmpty() }?.let {
                            Text(it, color = TextSecondary)
                        }
                        vodInfo.note?.takeIf { it.isNotEmpty() }?.let {
                            Text(it, color = TextSecondary)
                        }
                    }

                    // Description
                    vodInfo.des?.takeIf { it.isNotEmpty() }?.let { desc ->
                        Text(
                            text = desc,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 5
                        )
                    }

                    // Actors
                    vodInfo.actor?.takeIf { it.isNotEmpty() }?.let { actors ->
                        Text(
                            text = "演员: $actors",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Director
                    vodInfo.director?.takeIf { it.isNotEmpty() }?.let { director ->
                        Text(
                            text = "导演: $director",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Episode selection
        if (vodInfo.seriesFlags.isNotEmpty()) {
            item {
                TvSectionTitle(text = "选集")
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Series flags
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    vodInfo.seriesFlags.forEach { flag ->
                        TvButton(
                            text = flag.name,
                            onClick = { }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Episodes
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val episodes = vodInfo.seriesMap[vodInfo.seriesFlags.getOrNull(vodInfo.playIndex)?.name] ?: emptyList()
                    items(episodes.size) { index ->
                        val episode = episodes[index]
                        TvButton(
                            text = episode.name,
                            onClick = { onPlayClick(index) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
