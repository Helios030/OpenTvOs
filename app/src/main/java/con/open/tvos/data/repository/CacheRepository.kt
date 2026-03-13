package con.open.tvos.data.repository

import con.open.tvos.data.local.dao.CacheDao
import con.open.tvos.data.local.entity.CacheEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for generic cache operations
 */
@Singleton
class CacheRepository @Inject constructor(
    private val cacheDao: CacheDao
) {
    /**
     * Save data to cache
     */
    suspend fun save(key: String, data: ByteArray) {
        cacheDao.save(CacheEntity(key, data))
    }

    /**
     * Get data from cache
     */
    suspend fun get(key: String): ByteArray? {
        return cacheDao.getCache(key)?.data
    }

    /**
     * Get data from cache as Flow
     */
    fun getFlow(key: String): Flow<ByteArray?> {
        return cacheDao.getCacheFlow(key).let { flow ->
            kotlinx.coroutines.flow.map { it?.data }
        }
    }

    /**
     * Delete by key
     */
    suspend fun delete(key: String): Int {
        return cacheDao.deleteByKey(key)
    }

    /**
     * Clear all cache
     */
    suspend fun clearAll(): Int {
        return cacheDao.deleteAll()
    }

    /**
     * Update cache entry
     */
    suspend fun update(key: String, data: ByteArray) {
        cacheDao.update(CacheEntity(key, data))
    }
}
