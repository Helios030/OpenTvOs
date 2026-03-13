package con.open.tvos.player

data class TrackInfoBean(
    var name: String? = null,
    var language: String? = null,
    var groupIndex: Int = 0,
    var index: Int = 0,
    var selected: Boolean = false
)
