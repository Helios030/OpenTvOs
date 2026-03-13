package con.open.tvos.bean

import con.open.tvos.util.HawkConfig
import com.orhanobut.hawk.Hawk
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class Epginfo(
    Date: Date,
    str: String,
    date: Date,
    str1: String,
    str2: String,
    pos: Int
) {
    var startdateTime: Date? = null
    var enddateTime: Date? = null
    var datestart: Int = 0
    var dateend: Int = 0
    var title: String? = null
    var originStart: String? = null
    var originEnd: String? = null
    var start: String? = null
    var end: String? = null
    var index: Int = 0
    var epgDate: Date? = null
    var currentEpgDate: String? = null

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd")

    init {
        epgDate = Date
        currentEpgDate = timeFormat.format(epgDate!!)
        title = str
        originStart = str1
        originEnd = str2
        index = pos
        
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT+8:00")
        val userSimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
        userSimpleDateFormat.timeZone = TimeZone.getDefault()
        
        startdateTime = userSimpleDateFormat.parse(
            simpleDateFormat.format(date) + " " + str1 + ":00 GMT+8:00",
            ParsePosition(0)
        )
        enddateTime = userSimpleDateFormat.parse(
            simpleDateFormat.format(date) + " " + str2 + ":00 GMT+8:00",
            ParsePosition(0)
        )
        
        val zoneFormat = SimpleDateFormat("HH:mm")
        start = zoneFormat.format(startdateTime!!)
        end = zoneFormat.format(enddateTime!!)
        datestart = start!!.replace(":", "").toInt()
        dateend = end!!.replace(":", "").toInt()
    }
}
