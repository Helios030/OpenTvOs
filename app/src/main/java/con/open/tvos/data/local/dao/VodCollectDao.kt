package con.open.tvos.data.local.dao

import androidx.room.*
import con.open.tvos.data.local.entity.VodCollectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for VodCollect entity
 * Manages user's favorite videos with Flow support
 */
@Dao
interface VodCollectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collect: VodCollectEntity): Long

    @Query("SELECT * FROM vodCollect ORDER BY updateTime DESC")
    suspend fun getAll(): List<VodCollectEntity>

    @Query("SELECT * FROM vodCollect ORDER BY updateTime DESC")
    fun getAllFlow(): Flow<List<VodCollectEntity>>

    @Query("SELECT * FROM vodCollect WHERE `id` = :id")
    suspend fun getById(id: Int): VodCollectEntity?

    @Query("SELECT * FROM vodCollect WHERE `sourceKey` = :sourceKey AND `vodId` = :vodId")
    suspend fun getBySourceAndVodId(sourceKey: String, vodId: String): VodCollectEntity?

    @Query("SELECT * FROM vodCollect WHERE `sourceKey` = :sourceKey AND `vodId` = :vodId")
    fun getBySourceAndVodIdFlow(sourceKey: String, vodId: String): Flow<VodCollectEntity?>

    @Delete
    suspend fun delete(collect: VodCollectEntity): Int

    @Query("DELETE FROM vodCollect WHERE `id` = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM vodCollect")
    suspend fun deleteAll()

    @Query("SELECT EXISTS(SELECT 1 FROM vodCollect WHERE `sourceKey` = :sourceKey AND `vodId` = :vodId)")
    suspend fun exists(sourceKey: String, vodId: String): Boolean

    @Query("SELECT COUNT(*) FROM vodCollect")
    suspend fun getCount(): Int
}
