package con.open.tvos.bean

class SubtitleData {
    var isNew: Boolean? = null
    var subtitleList: List<Subtitle>? = null
    var isZip: Boolean? = null

    override fun toString(): String {
        return "SubtitleData{isNew='$isNew'}"
    }
}
