package con.open.tvos.bean

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamConverter
import com.thoughtworks.xstream.annotations.XStreamImplicit
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter
import java.io.Serializable

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
@XStreamAlias("list")
data class Movie(
    @XStreamAsAttribute
    @JvmField var page: Int = 0,
    @XStreamAsAttribute
    @JvmField var pagecount: Int = 0,//总页数
    @XStreamAsAttribute
    @JvmField var pagesize: Int = 0,
    @XStreamAsAttribute
    @JvmField var recordcount: Int = 0,//总条数
    @XStreamImplicit(itemFieldName = "video")
    @JvmField var videoList: List<Video>? = null
) : Serializable {

    @XStreamAlias("video")
    data class Video(
        @XStreamAlias("last")
        @JvmField var last: String? = null,//时间
        @XStreamAlias("id")
        @JvmField var id: String? = null,//内容id
        @XStreamAlias("tid")
        @JvmField var tid: Int = 0,//父级id
        @XStreamAlias("name")
        @JvmField var name: String? = null,//影片名称
        @XStreamAlias("type")
        @JvmField var type: String? = null,//类型名称
        /*@XStreamAlias("dt")//视频分类 zuidam3u8,zuidall
        @JvmField var dt: String? = null,*/
        @XStreamAlias("pic")
        @JvmField var pic: String? = null,//图片
        @XStreamAlias("lang")
        @JvmField var lang: String? = null,//语言
        @XStreamAlias("area")
        @JvmField var area: String? = null,//地区
        @XStreamAlias("year")
        @JvmField var year: Int = 0,//年份
        @XStreamAlias("state")
        @JvmField var state: String? = null,
        @XStreamAlias("note")
        @JvmField var note: String? = null,//描述集数或者影片信息
        @XStreamAlias("actor")
        @JvmField var actor: String? = null,//演员
        @XStreamAlias("director")
        @JvmField var director: String? = null,//导演
        @XStreamAlias("dl")
        @JvmField var urlBean: UrlBean? = null,
        @XStreamAlias("des")
        @JvmField var des: String? = null,//描述
        @JvmField var sourceKey: String? = null,
        @XStreamAlias("tag")
        @JvmField var tag: String? = null
    ) : Serializable {

        @XStreamAlias("dl")
        data class UrlBean(
            @XStreamImplicit(itemFieldName = "dd")
            @JvmField var infoList: List<UrlInfo>? = null
        ) : Serializable {

            @XStreamAlias("dd")
            @XStreamConverter(value = ToAttributedValueConverter::class, strings = ["urls"])
            data class UrlInfo(
                @XStreamAsAttribute
                @JvmField var flag: String? = null,//zuidam3u8,zuidall(MP4)
                @JvmField var urls: String? = null,
                @JvmField var beanList: List<InfoBean>? = null
            ) : Serializable {

                data class InfoBean(
                    @JvmField var name: String? = null,
                    @JvmField var url: String? = null
                ) : Serializable {
                    constructor(name: String, url: String) : this(name, url)
                }
            }
        }
    }
}
