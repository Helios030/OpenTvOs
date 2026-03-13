package con.open.tvos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import con.open.tvos.data.local.dao.CacheDao
import con.open.tvos.data.local.dao.VodCollectDao
import con.open.tvos.data.local.dao.VodRecordDao
import con.open.tvos.data.local.entity.CacheEntity
import con.open.tvos.data.local.entity.VodCollectEntity
import con.open.tvos.data.local.entity.VodRecordEntity

/**
 * Room Database for TVBox application
 * Contains cache, watch history, and favorites
 */
@Database(
    entities = [
        CacheEntity::class,
        VodRecordEntity::class,
        VodCollectEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
    abstract fun vodRecordDao(): VodRecordDao
    abstract fun vodCollectDao(): VodCollectDao
}
