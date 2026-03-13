package con.open.tvos.data.repository

import con.open.tvos.bean.Movie
import con.open.tvos.bean.SourceBean
import con.open.tvos.bean.VodInfo
import kotlinx.coroutines.flow.Flow

/**
 * Spider Repository Interface
 * 
 * Abstraction layer for content loading from various spider sources.
 * This interface maintains compatibility with existing spider implementations
 * while providing a modern Kotlin/Flow-based API.
 */
interface SpiderRepository {

    /**
     * Get list of available sources
     */
    fun getSources(): Flow<List<SourceBean>>

    /**
     * Get current active source
     */
    fun getActiveSource(): Flow<SourceBean?>

    /**
     * Set active source by key
     */
    suspend fun setActiveSource(sourceKey: String)

    /**
     * Get home content (categories with movies)
     * 
     * @param sourceKey The source to load from
     * @return Flow of category list with movies
     */
    fun getHomeContent(sourceKey: String): Flow<List<CategoryContent>>

    /**
     * Get movie list by category
     * 
     * @param sourceKey The source key
     * @param categoryId The category ID
     * @param page Page number (1-based)
     * @return Flow of movie list result
     */
    fun getMovieList(
        sourceKey: String,
        categoryId: String,
        page: Int = 1
    ): Flow<MovieListResult>

    /**
     * Get movie detail
     * 
     * @param sourceKey The source key
     * @param movieId The movie ID
     * @return Flow of VodInfo detail
     */
    fun getMovieDetail(
        sourceKey: String,
        movieId: String
    ): Flow<VodInfo>

    /**
     * Search movies
     * 
     * @param sourceKey The source key
     * @param keyword Search keyword
     * @param page Page number
     * @return Flow of search result
     */
    fun search(
        sourceKey: String,
        keyword: String,
        page: Int = 1
    ): Flow<MovieListResult>

    /**
     * Get play URL for episode
     * 
     * @param sourceKey The source key
     * @param vodInfo The VOD info
     * @param episodeIndex Episode index
     * @return Play URL result
     */
    suspend fun getPlayUrl(
        sourceKey: String,
        vodInfo: VodInfo,
        episodeIndex: Int
    ): PlayUrlResult

    /**
     * Check if source is searchable
     */
    suspend fun isSearchable(sourceKey: String): Boolean

    /**
     * Check if source has quick search
     */
    suspend fun isQuickSearchable(sourceKey: String): Boolean
}

/**
 * Category with content
 */
data class CategoryContent(
    val id: String,
    val name: String,
    val type: Int = 0,
    val movies: List<VodInfo> = emptyList(),
    val hasMore: Boolean = false
)

/**
 * Movie list result with pagination
 */
data class MovieListResult(
    val movies: List<VodInfo> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val total: Int = 0,
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = error == null
}

/**
 * Play URL result
 */
sealed class PlayUrlResult {
    data class Success(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val subtitleUrl: String? = null
    ) : PlayUrlResult()

    data class Error(
        val message: String
    ) : PlayUrlResult()
}
