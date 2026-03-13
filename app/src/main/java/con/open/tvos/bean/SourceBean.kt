package con.open.tvos.bean

import java.util.ArrayList

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
data class SourceBean(
    @JvmField var key: String? = null,
    @JvmField var name: String? = null,
    @JvmField var api: String? = null,
    @JvmField var type: Int = 0, // 0 xml 1 json 3 Spider
    @JvmField var searchable: Int = 0, // 是否可搜索
    @JvmField var quickSearch: Int = 0, // 是否可以快速搜索
    @JvmField var filterable: Int = 0, // 是否可以站点选择
    @JvmField var playerUrl: String? = null, // 站点解析Url
    @JvmField var ext: String? = null, // 扩展数据
    @JvmField var jar: String? = null, // 自定义jar
    @JvmField var categories: ArrayList<String>? = null, // 分类&排序
    @JvmField var playerType: Int = 0, // 0 system 1 ikj 2 exo 10 mxplayer -1 以参数设置页面的为准
    @JvmField var clickSelector: String? = null, // 需要点击播放的嗅探站点selector ddrk.me;#id
    @JvmField var style: String? = null // 展示风格
) {
    val isSearchable: Boolean
        get() = searchable != 0

    val isQuickSearch: Boolean
        get() = quickSearch != 0
}
