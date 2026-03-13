package con.open.tvos.data.repository

import con.open.tvos.data.local.dao.VodCollectDao
import con.open.tvos.data.local.entity.VodCollectEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for favorites/collection operations
 */
@Singleton
class VodCollectRepository @Inject constructor(
    private val vodCollectDao: VodCollectDao
) {
    /**
     * Get all favorites
     */
    fun getAllFlow(): Flow<List<VodCollectEntity>> {
        return vodCollectDao.getAllFlow()
    }

    /**
     * Get all favorites (one-shot)
     */
    suspend fun getAll(): List<VodCollectEntity> {
        return vodCollectDao.getAll()
    }

    /**
     * Get a favorite by id
     */
    suspend fun getById(id: Int): VodCollectEntity? {
        return vodCollectDao.getById(id)
    }

    /**
     * Check if a video is in favorites
     */
    suspend fun exists(sourceKey: String, vodId: String): Boolean {
        return vodCollectDao.exists(sourceKey, vodId)
    }

    /**
     * Get a favorite as Flow
     */
    fun getBySourceAndVodIdFlow(sourceKey: String, vodId: String): Flow<VodCollectEntity?> {
        return vodCollectDao.getBySourceAndVodIdFlow(sourceKey, vodId)
    }

    /**
     * Add to favorites
     */
    suspend fun add(collect: VodCollectEntity): Long {
        return vodCollectDao.insert(collect)
    }

    /**
     * Remove from favorites by id
     */
    suspend fun removeById(id: Int) {
        vodCollectDao.deleteById(id)
    }

    /**
     * Remove from favorites
     */
    suspend fun remove(collect: VodCollectEntity) {
        vodCollectDao.delete(collect)
    }

    /**
     * Remove all favorites
     */
    suspend fun removeAll() {
        vodCollectDao.deleteAll()
    }

    /**
     * Get total count of favorites
     */
    suspend fun getCount(): Int {
        return vodCollectDao.getCount()
    }
}
