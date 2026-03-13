package con.open.tvos.cache

import con.open.tvos.data.AppDataManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * 类描述:
 *
 * @author pj567
 * @since 2020/5/15
 */
object CacheManager {
    //反序列,把二进制数据转换成java object对象
    private fun toObject(data: ByteArray): Any? {
        return try {
            ByteArrayInputStream(data).use { bais ->
                ObjectInputStream(bais).use { ois ->
                    ois.readObject()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    //序列化存储数据需要转换成二进制
    private fun <T> toByteArray(body: T): ByteArray {
        return try {
            ByteArrayOutputStream().use { baos ->
                ObjectOutputStream(baos).use { oos ->
                    oos.writeObject(body)
                    oos.flush()
                    baos.toByteArray()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(0)
        }
    }

    fun <T> delete(key: String, body: T) {
        val cache = Cache(key = key, data = toByteArray(body))
        AppDataManager.get().cacheDao.delete(cache)
    }

    fun <T> save(key: String, body: T) {
        val cache = Cache(key = key, data = toByteArray(body))
        AppDataManager.get().cacheDao.save(cache)
    }

    fun getCache(key: String): Any? {
        val cache = AppDataManager.get().cacheDao.getCache(key)
        return if (cache?.data != null) {
            toObject(cache.data)
        } else {
            null
        }
    }
}
