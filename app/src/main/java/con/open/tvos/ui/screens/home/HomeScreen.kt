package con.open.tvos.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import con.open.tvos.ui.components.TvButton
import con.open.tvos.ui.components.TvCategoryHeader
import con.open.tvos.ui.components.TvPosterCard
import con.open.tvos.ui.theme.Background
import con.open.tvos.ui.theme.Primary
import con.open.tvos.ui.theme.TextPrimary

/**
 * Home Screen - Main landing screen for TV Box
 * 
 * Features:
 * - Category tabs for different content types
 * - Horizontal scrolling rows of content
 * - Quick access to search and settings
 */
@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLiveTvClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top Bar with logo and quick actions
        HomeTopBar(
            onSearchClick = onSearchClick,
            onSettingsClick = onSettingsClick,
            focusRequester = focusRequester
        )

        // Content rows
        when {
            state.isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "加载中...",
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            state.error != null -> {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "加载失败",
                            color = Color.Red,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TvButton(
                            text = "重试",
                            onClick = { viewModel.loadContent() }
                        )
                    }
                }
            }
            else -> {
                // Content
                HomeContent(
                    categories = state.categories,
                    onMovieClick = onMovieClick,
                    onLiveTvClick = onLiveTvClick
                )
            }
        }
    }
}

/**
 * Home Top Bar
 */
@Composable
private fun HomeTopBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo/Title
        Text(
            text = "TVBox",
            color = Primary,
            style = MaterialTheme.typography.displaySmall
        )

        // Quick actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvButton(
                text = "搜索",
                onClick = onSearchClick,
                modifier = Modifier.focusRequester(focusRequester)
            )
            TvButton(
                text = "设置",
                onClick = onSettingsClick
            )
        }
    }
}

/**
 * Home Content - Category rows
 */
@Composable
private fun HomeContent(
    categories: List<CategoryWithMovies>,
    onMovieClick: (String) -> Unit,
    onLiveTvClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberOverscrollEffect())
            .padding(bottom = 32.dp)
    ) {
        // Live TV Banner (if applicable)
        LiveTvBanner(onClick = onLiveTvClick)

        // Category rows
        categories.forEach { category ->
            CategoryRow(
                category = category,
                onMovieClick = onMovieClick
            )
        }
    }
}

/**
 * Live TV Banner
 */
@Composable
private fun LiveTvBanner(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 16.dp)
    ) {
        TvButton(
            text = "📺 直播电视",
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(0.5f)
        )
    }
}

/**
 * Category Row with horizontal scrolling
 */
@Composable
private fun CategoryRow(
    category: CategoryWithMovies,
    onMovieClick: (String) -> Unit
) {
    Column {
        TvCategoryHeader(
            title = category.name,
            onMoreClick = { /* TODO: Navigate to category detail */ }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(category.movies) { movie ->
                TvPosterCard(
                    title = movie.name,
                    subtitle = movie.note,
                    imageUrl = movie.pic,
                    onClick = { onMovieClick(movie.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Data classes for Home Screen
 */
data class CategoryWithMovies(
    val id: String,
    val name: String,
    val movies: List<MovieItem>
)

data class MovieItem(
    val id: String,
    val name: String,
    val pic: String?,
    val note: String?
)
