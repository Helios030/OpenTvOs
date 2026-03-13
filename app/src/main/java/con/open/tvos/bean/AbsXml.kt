package con.open.tvos.bean

import com.thoughtworks.xstream.annotations.XStreamAlias
import java.io.Serializable

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
@XStreamAlias("rss")
class AbsXml : Serializable {
    @XStreamAlias("list")
    var movie: Movie? = null

    @XStreamAlias("msg")
    var msg: String? = null
}
