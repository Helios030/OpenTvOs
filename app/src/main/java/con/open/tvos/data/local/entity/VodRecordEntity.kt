package con.open.tvos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Video playback history record
 * Stores watch progress and video metadata for history feature
 */
@Entity(tableName = "vodRecord")
data class VodRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "vodId")
    val vodId: String,
    @ColumnInfo(name = "updateTime")
    val updateTime: Long,
    @ColumnInfo(name = "sourceKey")
    val sourceKey: String,
    val dataJson: String
)
