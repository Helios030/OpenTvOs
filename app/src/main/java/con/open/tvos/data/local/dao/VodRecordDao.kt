package con.open.tvos.data.local.dao

import androidx.room.*
import con.open.tvos.data.local.entity.VodRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for VodRecord entity
 * Manages video playback history with Flow support
 */
@Dao
interface VodRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: VodRecordEntity): Long

    @Query("SELECT * FROM vodRecord ORDER BY updateTime DESC LIMIT :limit")
    suspend fun getAll(limit: Int = 100): List<VodRecordEntity>

    @Query("SELECT * FROM vodRecord ORDER BY updateTime DESC LIMIT :limit")
    fun getAllFlow(limit: Int = 100): Flow<List<VodRecordEntity>>

    @Query("SELECT * FROM vodRecord WHERE `sourceKey` = :sourceKey AND `vodId` = :vodId")
    suspend fun getVodRecord(sourceKey: String, vodId: String): VodRecordEntity?

    @Query("SELECT * FROM vodRecord WHERE `sourceKey` = :sourceKey AND `vodId` = :vodId")
    fun getVodRecordFlow(sourceKey: String, vodId: String): Flow<VodRecordEntity?>

    @Delete
    suspend fun delete(record: VodRecordEntity): Int

    @Query("SELECT COUNT(*) FROM vodRecord")
    suspend fun getCount(): Int

    @Query("DELETE FROM vodRecord")
    suspend fun deleteAll()

    @Query("DELETE FROM vodRecord WHERE id NOT IN (SELECT id FROM vodRecord ORDER BY updateTime DESC LIMIT :keepCount)")
    suspend fun keepOnly(keepCount: Int): Int
}
