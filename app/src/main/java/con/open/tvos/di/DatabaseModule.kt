package con.open.tvos.di

import android.content.Context
import androidx.room.Room
import con.open.tvos.data.local.AppDatabase
import con.open.tvos.data.local.dao.CacheDao
import con.open.tvos.data.local.dao.VodCollectDao
import con.open.tvos.data.local.dao.VodRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Room database and DAOs
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tvbox_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCacheDao(database: AppDatabase): CacheDao {
        return database.cacheDao()
    }

    @Provides
    fun provideVodRecordDao(database: AppDatabase): VodRecordDao {
        return database.vodRecordDao()
    }

    @Provides
    fun provideVodCollectDao(database: AppDatabase): VodCollectDao {
        return database.vodCollectDao()
    }
}
