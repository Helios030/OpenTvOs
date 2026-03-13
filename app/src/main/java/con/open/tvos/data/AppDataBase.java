package con.open.tvos.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import con.open.tvos.cache.Cache;
import con.open.tvos.cache.CacheDao;
import con.open.tvos.cache.VodCollect;
import con.open.tvos.cache.VodCollectDao;
import con.open.tvos.cache.VodRecord;
import con.open.tvos.cache.VodRecordDao;


/**
 * 类描述:
 *
 * @author pj567
 * @since 2020/5/15
 */
@Database(entities = {Cache.class, VodRecord.class, VodCollect.class}, version = 1)
public abstract class AppDataBase extends RoomDatabase {
    public abstract CacheDao getCacheDao();

    public abstract VodRecordDao getVodRecordDao();

    public abstract VodCollectDao getVodCollectDao();
}
