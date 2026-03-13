package con.open.tvos.di

import con.open.tvos.api.ApiConfig
import con.open.tvos.data.local.dao.CacheDao
import con.open.tvos.data.local.dao.VodCollectDao
import con.open.tvos.data.local.dao.VodRecordDao
import con.open.tvos.data.repository.CacheRepository
import con.open.tvos.data.repository.SpiderRepository
import con.open.tvos.data.repository.SpiderRepositoryImpl
import con.open.tvos.data.repository.VodCollectRepository
import con.open.tvos.data.repository.VodRecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Repository instances
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCacheRepository(cacheDao: CacheDao): CacheRepository {
        return CacheRepository(cacheDao)
    }

    @Provides
    @Singleton
    fun provideVodRecordRepository(vodRecordDao: VodRecordDao): VodRecordRepository {
        return VodRecordRepository(vodRecordDao)
    }

    @Provides
    @Singleton
    fun provideVodCollectRepository(vodCollectDao: VodCollectDao): VodCollectRepository {
        return VodCollectRepository(vodCollectDao)
    }

    @Provides
    @Singleton
    fun provideSpiderRepository(apiConfig: ApiConfig): SpiderRepository {
        return SpiderRepositoryImpl(apiConfig)
    }
}
