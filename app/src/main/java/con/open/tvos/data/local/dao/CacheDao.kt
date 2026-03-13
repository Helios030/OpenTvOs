package con.open.tvos.data.local.dao

import androidx.room.*
import con.open.tvos.data.local.entity.CacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Cache entity
 * Provides CRUD operations with Flow support for reactive programming
 */
@Dao
interface CacheDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(cache: CacheEntity): Long

    @Query("SELECT * FROM cache WHERE `key` = :key")
    suspend fun getCache(key: String): CacheEntity?

    @Query("SELECT * FROM cache WHERE `key` = :key")
    fun getCacheFlow(key: String): Flow<CacheEntity?>

    @Delete
    suspend fun delete(cache: CacheEntity): Int

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(cache: CacheEntity): Int

    @Query("DELETE FROM cache WHERE `key` = :key")
    suspend fun deleteByKey(key: String): Int

    @Query("DELETE FROM cache")
    suspend fun deleteAll(): Int
}
