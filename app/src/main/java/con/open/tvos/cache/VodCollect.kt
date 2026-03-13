package con.open.tvos.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "vodCollect")
data class VodCollect(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo(name = "vodId")
    var vodId: String? = null,
    @ColumnInfo(name = "updateTime")
    var updateTime: Long = 0,
    @ColumnInfo(name = "sourceKey")
    var sourceKey: String? = null,
    @ColumnInfo(name = "name")
    var name: String? = null,
    @ColumnInfo(name = "pic")
    var pic: String? = null
) : Serializable
