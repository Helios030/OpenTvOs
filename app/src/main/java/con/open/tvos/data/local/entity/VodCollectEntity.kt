package con.open.tvos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Video favorite/collection entity
 * Stores user's favorite videos for quick access
 */
@Entity(tableName = "vodCollect")
data class VodCollectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "vodId")
    val vodId: String,
    @ColumnInfo(name = "updateTime")
    val updateTime: Long,
    @ColumnInfo(name = "sourceKey")
    val sourceKey: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "pic")
    val pic: String
)
