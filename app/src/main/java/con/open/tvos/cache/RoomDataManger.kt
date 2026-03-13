package con.open.tvos.cache

import android.text.TextUtils
import con.open.tvos.data.AppDataManager
import com.google.gson.ExclusionStrategy
import con.open.tvos.bean.VodInfo
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import con.open.tvos.util.HawkConfig
import con.open.tvos.util.HistoryHelper
import com.orhanobut.hawk.Hawk

/**
 * @author pj567
 * @date :2021/1/7
 * @description:
 */
object RoomDataManger {
    private val vodInfoStrategy = object : ExclusionStrategy {
        override fun shouldSkipField(field: FieldAttributes): Boolean {
            if (field.declaringClass == VodInfo::class.java && field.name == "seriesFlags") {
                return true
            }
            if (field.declaringClass == VodInfo::class.java && field.name == "seriesMap") {
                return true
            }
            return false
        }

        override fun shouldSkipClass(clazz: Class<*>): Boolean {
            return false
        }
    }

    private fun getVodInfoGson(): Gson {
        return GsonBuilder().addSerializationExclusionStrategy(vodInfoStrategy).create()
    }

    fun insertVodRecord(sourceKey: String, vodInfo: VodInfo) {
        var record = AppDataManager.get().vodRecordDao.getVodRecord(sourceKey, vodInfo.id)
        if (record == null) {
            record = VodRecord()
        }
        record.sourceKey = sourceKey
        record.vodId = vodInfo.id
        record.updateTime = System.currentTimeMillis()
        record.dataJson = getVodInfoGson().toJson(vodInfo)
        AppDataManager.get().vodRecordDao.insert(record)
    }

    fun getVodInfo(sourceKey: String, vodId: String): VodInfo? {
        val record = AppDataManager.get().vodRecordDao.getVodRecord(sourceKey, vodId)
        return try {
            if (record?.dataJson != null && !TextUtils.isEmpty(record.dataJson)) {
                val vodInfo = getVodInfoGson().fromJson<VodInfo>(
                    record.dataJson,
                    object : TypeToken<VodInfo>() {}.type
                )
                if (vodInfo.name == null) null else vodInfo
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteVodRecord(sourceKey: String, vodInfo: VodInfo) {
        val record = AppDataManager.get().vodRecordDao.getVodRecord(sourceKey, vodInfo.id)
        if (record != null) {
            AppDataManager.get().vodRecordDao.delete(record)
        }
    }

    fun getAllVodRecord(limit: Int): List<VodInfo> {
        val count = AppDataManager.get().vodRecordDao.getCount()
        val index = Hawk.get(HawkConfig.HISTORY_NUM, 0)
        val hisNum = HistoryHelper.getHisNum(index)
        if (count > hisNum) {
            AppDataManager.get().vodRecordDao.reserver(hisNum)
        }
        val recordList = AppDataManager.get().vodRecordDao.getAll(limit)
        val vodInfoList = mutableListOf<VodInfo>()
        if (recordList != null) {
            for (record in recordList) {
                var info: VodInfo? = null
                try {
                    if (record.dataJson != null && !TextUtils.isEmpty(record.dataJson)) {
                        info = getVodInfoGson().fromJson<VodInfo>(
                            record.dataJson,
                            object : TypeToken<VodInfo>() {}.type
                        )
                        info?.sourceKey = record.sourceKey
                        if (info?.name == null) {
                            info = null
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (info != null) {
                    vodInfoList.add(info)
                }
            }
        }
        return vodInfoList
    }

    fun insertVodCollect(sourceKey: String, vodInfo: VodInfo) {
        var record = AppDataManager.get().vodCollectDao.getVodCollect(sourceKey, vodInfo.id)
        if (record != null) {
            return
        }
        record = VodCollect()
        record.sourceKey = sourceKey
        record.vodId = vodInfo.id
        record.updateTime = System.currentTimeMillis()
        record.name = vodInfo.name
        record.pic = vodInfo.pic
        AppDataManager.get().vodCollectDao.insert(record)
    }

    fun deleteVodCollect(id: Int) {
        AppDataManager.get().vodCollectDao.delete(id)
    }

    fun deleteVodCollect(sourceKey: String, vodInfo: VodInfo) {
        val record = AppDataManager.get().vodCollectDao.getVodCollect(sourceKey, vodInfo.id)
        if (record != null) {
            AppDataManager.get().vodCollectDao.delete(record)
        }
    }

    fun deleteVodCollectAll() {
        AppDataManager.get().vodCollectDao.deleteAll()
    }

    fun deleteVodRecordAll() {
        AppDataManager.get().vodRecordDao.deleteAll()
    }

    fun isVodCollect(sourceKey: String, vodId: String): Boolean {
        val record = AppDataManager.get().vodCollectDao.getVodCollect(sourceKey, vodId)
        return record != null
    }

    fun getAllVodCollect(): List<VodCollect> {
        return AppDataManager.get().vodCollectDao.getAll()
    }
}
