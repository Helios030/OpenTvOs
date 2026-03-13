package con.open.tvos.ui.screens.home

import androidx.lifecycle.viewModelScope
import con.open.tvos.ui.base.BaseViewModel
import con.open.tvos.ui.base.UiEffect
import con.open.tvos.ui.base.UiEvent
import con.open.tvos.ui.base.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home Screen State
 */
data class HomeState(
    val categories: List<CategoryWithMovies> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) : UiState

/**
 * Home Screen Events
 */
sealed class HomeEvent : UiEvent {
    object LoadContent : HomeEvent()
    object Refresh : HomeEvent()
    data class CategorySelected(val categoryId: String) : HomeEvent()
}

/**
 * Home Screen Effects
 */
sealed class HomeEffect : UiEffect {
    data class NavigateToDetail(val vodId: String) : HomeEffect()
    data class NavigateToPlayer(val vodId: String, val episodeIndex: Int) : HomeEffect()
    object NavigateToSearch : HomeEffect()
    object NavigateToSettings : HomeEffect()
}

/**
 * Home Screen ViewModel
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    // TODO: Inject repository when ready
    // private val vodRepository: VodRepository
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    initialState = HomeState()
) {

    init {
        loadContent()
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadContent -> loadContent()
            is HomeEvent.Refresh -> refresh()
            is HomeEvent.CategorySelected -> onCategorySelected(event.categoryId)
        }
    }

    fun loadContent() {
        viewModelScope.launch {
            setLoading(true)
            clearError()
            
            try {
                // TODO: Load actual content from repository
                // For now, use dummy data
                val categories = loadDummyContent()
                
                updateState {
                    copy(
                        categories = categories,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                setError(e.message ?: "Unknown error")
            }
        }
    }

    private fun refresh() {
        loadContent()
    }

    private fun onCategorySelected(categoryId: String) {
        // TODO: Handle category selection
    }

    /**
     * Dummy data for development
     * TODO: Replace with actual repository call
     */
    private fun loadDummyContent(): List<CategoryWithMovies> {
        return listOf(
            CategoryWithMovies(
                id = "1",
                name = "最新电影",
                movies = listOf(
                    MovieItem("1", "电影1", null, "2024"),
                    MovieItem("2", "电影2", null, "2024"),
                    MovieItem("3", "电影3", null, "2024"),
                    MovieItem("4", "电影4", null, "2024"),
                    MovieItem("5", "电影5", null, "2024")
                )
            ),
            CategoryWithMovies(
                id = "2",
                name = "热门电视剧",
                movies = listOf(
                    MovieItem("6", "电视剧1", null, "更新至20集"),
                    MovieItem("7", "电视剧2", null, "更新至15集"),
                    MovieItem("8", "电视剧3", null, "全40集"),
                    MovieItem("9", "电视剧4", null, "更新至10集"),
                    MovieItem("10", "电视剧5", null, "全30集")
                )
            ),
            CategoryWithMovies(
                id = "3",
                name = "综艺",
                movies = listOf(
                    MovieItem("11", "综艺1", null, "最新一期"),
                    MovieItem("12", "综艺2", null, "最新一期"),
                    MovieItem("13", "综艺3", null, "最新一期")
                )
            ),
            CategoryWithMovies(
                id = "4",
                name = "动漫",
                movies = listOf(
                    MovieItem("14", "动漫1", null, "更新至12集"),
                    MovieItem("15", "动漫2", null, "全24集"),
                    MovieItem("16", "动漫3", null, "更新至8集")
                )
            )
        )
    }
}
