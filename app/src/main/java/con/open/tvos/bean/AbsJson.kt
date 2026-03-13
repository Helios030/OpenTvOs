package con.open.tvos.bean

import java.io.Serializable

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
class AbsJson : Serializable {
    var code: Int = 0
    var limit: String? = null
    var list: ArrayList<AbsJsonVod>? = null
    var msg: String? = null
    var page: Int = 0
    var pagecount: Int = 0
    var total: Int = 0

    inner class AbsJsonVod : Serializable {
        var group_id: Int = 0
        var type_id: Int = 0
        var type_id_1: Int = 0
        var type_name: String? = null
        var vod_actor: String? = null
        var vod_area: String? = null
        var vod_author: String? = null
        var vod_behind: String? = null
        var vod_blurb: String? = null
        var vod_class: String? = null
        var vod_color: String? = null
        var vod_content: String? = null
        var vod_copyright: String? = null
        var vod_director: String? = null
        var vod_douban_id: String? = null
        var vod_douban_score: String? = null
        var vod_down: String? = null
        var vod_down_from: String? = null
        var vod_down_note: String? = null
        var vod_down_server: String? = null
        var vod_down_url: String? = null
        var vod_duration: String? = null
        var vod_en: String? = null
        var vod_hits: String? = null
        var vod_hits_day: String? = null
        var vod_hits_month: String? = null
        var vod_hits_week: String? = null
        var vod_id: String? = null
        var vod_isend: String? = null
        var vod_jumpurl: String? = null
        var vod_lang: String? = null
        var vod_letter: String? = null
        var vod_level: String? = null
        var vod_lock: String? = null
        var vod_name: String? = null
        var vod_pic: String? = null
        var vod_pic_screenshot: String? = null
        var vod_pic_slide: String? = null
        var vod_pic_thumb: String? = null
        var vod_play_from: String? = null
        var vod_play_note: String? = null
        var vod_play_server: String? = null
        var vod_play_url: String? = null
        var vod_plot: String? = null
        var vod_plot_detail: String? = null
        var vod_plot_name: String? = null
        var vod_points: String? = null
        var vod_points_down: String? = null
        var vod_points_play: String? = null
        var vod_pubdate: String? = null
        var vod_pwd: String? = null
        var vod_pwd_down: String? = null
        var vod_pwd_down_url: String? = null
        var vod_pwd_play: String? = null
        var vod_pwd_play_url: String? = null
        var vod_pwd_url: String? = null
        var vod_rel_art: String? = null
        var vod_rel_vod: String? = null
        var vod_remarks: String? = null
        var vod_reurl: String? = null
        var vod_score: String? = null
        var vod_score_all: String? = null
        var vod_score_num: String? = null
        var vod_serial: String? = null
        var vod_state: String? = null
        var vod_status: String? = null
        var vod_sub: String? = null
        var vod_tag: String? = null
        var vod_time: String? = null
        var vod_time_add: String? = null
        var vod_time_hits: String? = null
        var vod_time_make: String? = null
        var vod_total: String? = null
        var vod_tpl: String? = null
        var vod_tpl_down: String? = null
        var vod_tpl_play: String? = null
        var vod_trysee: String? = null
        var vod_tv: String? = null
        var vod_up: String? = null
        var vod_version: String? = null
        var vod_weekday: String? = null
        var vod_writer: String? = null
        var vod_year: String? = null

        fun toXmlVideo(): Movie.Video {
            val video = Movie.Video()
            video.tag = vod_tag
            video.last = vod_time
            video.id = vod_id
            video.tid = type_id
            video.name = vod_name
            video.type = type_name
            video.pic = vod_pic
            video.lang = vod_lang
            video.area = vod_area
            video.year = vod_year?.toIntOrNull() ?: 0
            video.state = vod_state
            video.note = vod_remarks
            video.actor = vod_actor
            video.director = vod_director
            val urlBean = Movie.Video.UrlBean()
            if (vod_play_from != null && vod_play_url != null) {
                val playFlags = vod_play_from!!.split("\\\$\\\$\\\$".toRegex())
                val playUrls = vod_play_url!!.split("\\\$\\\$\\\$".toRegex())
                val infoList = ArrayList<Movie.Video.UrlBean.UrlInfo>()
                for (i in playFlags.indices) {
                    val urlInfo = Movie.Video.UrlBean.UrlInfo()
                    urlInfo.flag = playFlags[i]
                    urlInfo.urls = if (i < playUrls.size) playUrls[i] else ""
                    infoList.add(urlInfo)
                }
                urlBean.infoList = infoList
            }
            video.urlBean = urlBean
            video.des = vod_content
            return video
        }
    }

    fun toAbsXml(): AbsXml {
        val xml = AbsXml()
        val movie = Movie()
        movie.page = page
        movie.pagecount = pagecount
        movie.pagesize = limit?.toIntOrNull() ?: 0
        movie.recordcount = total
        val videoList = ArrayList<Movie.Video>()
        list?.forEach { vod ->
            try {
                videoList.add(vod.toXmlVideo())
            } catch (th: Throwable) {
                movie.pagesize = 0
            }
        }
        movie.videoList = videoList
        xml.movie = movie
        xml.msg = msg
        return xml
    }
}
