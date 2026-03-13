package con.open.tvos.bean

import com.thoughtworks.xstream.annotations.XStreamAlias
import java.io.Serializable

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
@XStreamAlias("rss")
class AbsSortXml : Serializable {
    @XStreamAlias("class")
    var classes: MovieSort? = null

    @XStreamAlias("list")
    var list: Movie? = null

    var videoList: List<Movie.Video>? = null
}
