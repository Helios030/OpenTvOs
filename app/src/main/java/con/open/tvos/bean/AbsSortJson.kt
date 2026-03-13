package con.open.tvos.bean

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class AbsSortJson : Serializable {
    @SerializedName("class")
    var classes: ArrayList<AbsJsonClass>? = null

    @SerializedName("list")
    var list: ArrayList<AbsJson.AbsJsonVod>? = null

    fun toAbsSortXml(): AbsSortXml {
        val absSortXml = AbsSortXml()
        val movieSort = MovieSort()
        movieSort.sortList = ArrayList()
        classes?.forEach { cls ->
            val sortData = MovieSort.SortData().apply {
                id = cls.type_id
                name = cls.type_name
                flag = cls.type_flag
            }
            movieSort.sortList!!.add(sortData)
        }
        if (!list.isNullOrEmpty()) {
            val movie = Movie()
            val videos = ArrayList<Movie.Video>()
            list?.forEach { vod ->
                videos.add(vod.toXmlVideo())
            }
            movie.videoList = videos
            absSortXml.list = movie
        } else {
            absSortXml.list = null
        }
        absSortXml.classes = movieSort
        return absSortXml
    }

    inner class AbsJsonClass : Serializable {
        var type_id: String? = null
        var type_name: String? = null
        var type_flag: String? = null
    }
}
