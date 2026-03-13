package con.open.tvos.data.repository

import con.open.tvos.data.local.dao.VodRecordDao
import con.open.tvos.data.local.entity.VodRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for watch history operations
 */
@Singleton
class VodRecordRepository @Inject constructor(
    private val vodRecordDao: VodRecordDao
) {
    /**
     * Get all watch history records
     */
    fun getAllFlow(limit: Int = 100): Flow<List<VodRecordEntity>> {
        return vodRecordDao.getAllFlow(limit)
    }

    /**
     * Get a specific record by source and vodId
     */
    suspend fun getRecord(sourceKey: String, vodId: String): VodRecordEntity? {
        return vodRecordDao.getVodRecord(sourceKey, vodId)
    }

    /**
     * Get a specific record as Flow
     */
    fun getRecordFlow(sourceKey: String, vodId: String): Flow<VodRecordEntity?> {
        return vodRecordDao.getVodRecordFlow(sourceKey, vodId)
    }

    /**
     * Save or update a record
     */
    suspend fun save(record: VodRecordEntity) {
        vodRecordDao.insert(record)
    }

    /**
     * Delete a record
     */
    suspend fun delete(record: VodRecordEntity) {
        vodRecordDao.delete(record)
    }

    /**
     * Delete all records
     */
    suspend fun deleteAll() {
        vodRecordDao.deleteAll()
    }

    /**
     * Keep only the most recent records, delete the rest
     */
    suspend fun keepOnly(keepCount: Int) {
        vodRecordDao.keepOnly(keepCount)
    }

    /**
     * Get total count of records
     */
    suspend fun getCount(): Int {
        return vodRecordDao.getCount()
    }
}
