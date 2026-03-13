package con.open.tvos.data.repository

import con.open.tvos.crawler.Spider
import con.open.tvos.api.ApiConfig
import con.open.tvos.bean.Movie
import con.open.tvos.bean.SourceBean
import con.open.tvos.bean.VodInfo
import con.open.tvos.util.DefaultConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SpiderRepository
 * 
 * Bridges the modern Flow-based API with the existing Spider interface.
 * Maintains compatibility with existing spiders while providing reactive data access.
 */
@Singleton
class SpiderRepositoryImpl @Inject constructor(
    private val apiConfig: ApiConfig
) : SpiderRepository {

    // Cache for loaded spiders
    private val spiderCache = mutableMapOf<String, Spider>()

    /**
     * Get or load spider for source
     */
    private fun getSpider(sourceKey: String): Spider? {
        val sourceBean = apiConfig.getSource(sourceKey) ?: return null
        return apiConfig.getCSP(sourceBean)
    }

    // ==================== Source Management ====================

    override fun getSources(): Flow<List<SourceBean>> = apiConfig.getSourcesFlow()

    override fun getActiveSource(): Flow<SourceBean?> = apiConfig.getActiveSourceFlow()

    override suspend fun setActiveSource(sourceKey: String) = withContext(Dispatchers.IO) {
        val source = apiConfig.getSource(sourceKey) ?: return@withContext
        apiConfig.setSourceBean(source)
    }

    // ==================== Content Loading ====================

    override fun getHomeContent(sourceKey: String): Flow<List<CategoryContent>> = flow {
        val spider = getSpider(sourceKey) ?: run {
            emit(emptyList())
            return@flow
        }

        val result = withContext(Dispatchers.IO) {
            try {
                val homeContent = spider.homeContent(true)
                parseHomeContent(homeContent)
            } catch (e: Exception) {
                emptyList()
            }
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    override fun getMovieList(
        sourceKey: String,
        categoryId: String,
        page: Int
    ): Flow<MovieListResult> = flow {
        val spider = getSpider(sourceKey) ?: run {
            emit(MovieListResult(error = "Spider not found"))
            return@flow
        }

        val result = withContext(Dispatchers.IO) {
            try {
                val categoryContent = spider.categoryContent(categoryId, page.toString(), true, HashMap())
                parseMovieListResult(categoryContent)
            } catch (e: Exception) {
                MovieListResult(error = e.message ?: "Unknown error")
            }
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    override fun getMovieDetail(sourceKey: String, movieId: String): Flow<VodInfo?> = flow {
        val spider = getSpider(sourceKey) ?: run {
            emit(null)
            return@flow
        }

        val result = withContext(Dispatchers.IO) {
            try {
                val detailContent = spider.detailContent(listOf(movieId))
                parseVodInfo(detailContent)
            } catch (e: Exception) {
                null
            }
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    override fun search(
        sourceKey: String,
        keyword: String,
        page: Int
    ): Flow<MovieListResult> = flow {
        val spider = getSpider(sourceKey) ?: run {
            emit(MovieListResult(error = "Spider not found"))
            return@flow
        }

        val result = withContext(Dispatchers.IO) {
            try {
                val searchContent = spider.searchContent(keyword, false)
                parseMovieListResult(searchContent)
            } catch (e: Exception) {
                MovieListResult(error = e.message ?: "Unknown error")
            }
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    override suspend fun getPlayUrl(
        sourceKey: String,
        vodInfo: VodInfo,
        episodeIndex: Int
    ): PlayUrlResult = withContext(Dispatchers.IO) {
        val spider = getSpider(sourceKey)
        if (spider == null) {
            return@withContext PlayUrlResult.Error("Spider not found")
        }

        try {
            val flag = vodInfo.seriesFlags?.getOrNull(episodeIndex) ?: ""
            val id = vodInfo.seriesIds?.getOrNull(episodeIndex) ?: ""
            val vipFlags = apiConfig.getVipParseFlags() ?: emptyList()

            val playerContent = spider.playerContent(flag, id, vipFlags)
            parsePlayUrlResult(playerContent)
        } catch (e: Exception) {
            PlayUrlResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun isSearchable(sourceKey: String): Boolean = withContext(Dispatchers.IO) {
        val source = apiConfig.getSource(sourceKey) ?: return@withContext false
        source.searchable == 1
    }

    override suspend fun isQuickSearchable(sourceKey: String): Boolean = withContext(Dispatchers.IO) {
        val source = apiConfig.getSource(sourceKey) ?: return@withContext false
        source.quickSearch == 1
    }

    // ==================== JSON Parsing Helpers ====================

    private fun parseHomeContent(json: String?): List<CategoryContent> {
        if (json.isNullOrEmpty()) return emptyList()

        return try {
            val jsonObject = JSONObject(json)
            val classesArray = jsonObject.optJSONArray("class")
            val listArray = jsonObject.optJSONArray("list")

            val categories = mutableListOf<CategoryContent>()

            // Parse categories
            if (classesArray != null) {
                for (i in 0 until classesArray.length()) {
                    val classObj = classesArray.optJSONObject(i)
                    val typeId = classObj?.optString("type_id", "") ?: ""
                    val typeName = classObj?.optString("type_name", "") ?: ""

                    // Get movies for this category
                    val movies = mutableListOf<VodInfo>()
                    if (listArray != null) {
                        for (j in 0 until listArray.length()) {
                            val movieObj = listArray.optJSONObject(j)
                            val movie = parseMovieFromJson(movieObj)
                            if (movie != null && (movie.type == typeId || typeId.isEmpty())) {
                                movies.add(movie)
                            }
                        }
                    }

                    categories.add(CategoryContent(
                        id = typeId,
                        name = typeName,
                        movies = movies
                    ))
                }
            }

            // If no categories, add all movies to a default category
            if (categories.isEmpty() && listArray != null) {
                val movies = mutableListOf<VodInfo>()
                for (j in 0 until listArray.length()) {
                    val movieObj = listArray.optJSONObject(j)
                    val movie = parseMovieFromJson(movieObj)
                    if (movie != null) {
                        movies.add(movie)
                    }
                }
                if (movies.isNotEmpty()) {
                    categories.add(CategoryContent(
                        id = "all",
                        name = "全部",
                        movies = movies
                    ))
                }
            }

            categories
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseMovieListResult(json: String?): MovieListResult {
        if (json.isNullOrEmpty()) {
            return MovieListResult(error = "Empty response")
        }

        return try {
            val jsonObject = JSONObject(json)
            val listArray = jsonObject.optJSONArray("list")

            val movies = mutableListOf<VodInfo>()
            if (listArray != null) {
                for (i in 0 until listArray.length()) {
                    val movieObj = listArray.optJSONObject(i)
                    val movie = parseMovieFromJson(movieObj)
                    if (movie != null) {
                        movies.add(movie)
                    }
                }
            }

            val page = jsonObject.optInt("page", 1)
            val hasMore = movies.isNotEmpty() && jsonObject.optInt("pagecount", 1) > page

            MovieListResult(
                movies = movies,
                page = page,
                hasMore = hasMore,
                total = jsonObject.optInt("total", movies.size)
            )
        } catch (e: Exception) {
            MovieListResult(error = e.message ?: "Parse error")
        }
    }

    private fun parseVodInfo(json: String?): VodInfo? {
        if (json.isNullOrEmpty()) return null

        return try {
            val jsonObject = JSONObject(json)
            val listArray = jsonObject.optJSONArray("list")

            if (listArray != null && listArray.length() > 0) {
                val movieObj = listArray.optJSONObject(0)
                parseMovieFromJson(movieObj)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMovieFromJson(obj: JSONObject?): VodInfo? {
        if (obj == null) return null

        return try {
            VodInfo().apply {
                sourceKey = obj.optString("source_key", "")
                vodId = obj.optString("vod_id", obj.optString("vodId", ""))
                vodName = obj.optString("vod_name", obj.optString("vodName", ""))
                vodPic = obj.optString("vod_pic", obj.optString("vodPic", ""))
                vodRemarks = obj.optString("vod_remarks", obj.optString("vodRemarks", ""))
                type = obj.optString("type_id", obj.optString("type", ""))
                typeDes = obj.optString("type_name", "")
                vodYear = obj.optString("vod_year", obj.optString("vodYear", ""))
                vodArea = obj.optString("vod_area", obj.optString("vodArea", ""))
                vodDirector = obj.optString("vod_director", obj.optString("vodDirector", ""))
                vodActor = obj.optString("vod_actor", obj.optString("vodActor", ""))
                vodContent = obj.optString("vod_content", obj.optString("vodContent", ""))
                vodPlayFrom = obj.optString("vod_play_from", obj.optString("vodPlayFrom", ""))
                vodPlayUrl = obj.optString("vod_play_url", obj.optString("vodPlayUrl", ""))

                // Parse series info
                parseSeriesInfo()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePlayUrlResult(json: String?): PlayUrlResult {
        if (json.isNullOrEmpty()) {
            return PlayUrlResult.Error("Empty response")
        }

        return try {
            val jsonObject = JSONObject(json)
            val url = jsonObject.optString("url", "")
            val parse = jsonObject.optInt("parse", 0)

            if (url.isEmpty()) {
                PlayUrlResult.Error("No URL in response")
            } else {
                val headers = mutableMapOf<String, String>()
                val header = jsonObject.optJSONObject("header")
                if (header != null) {
                    val keys = header.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        headers[key] = header.optString(key, "")
                    }
                }

                val subtitleUrl = jsonObject.optString("sub", null)

                PlayUrlResult.Success(
                    url = url,
                    headers = headers,
                    subtitleUrl = subtitleUrl
                )
            }
        } catch (e: Exception) {
            PlayUrlResult.Error(e.message ?: "Parse error")
        }
    }
}
