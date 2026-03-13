package con.open.tvos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Generic key-value cache entity
 * Used for storing serialized data with string keys
 */
@Entity(tableName = "cache")
data class CacheEntity(
    @PrimaryKey(autoGenerate = false)
    val key: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CacheEntity
        if (key != other.key) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
