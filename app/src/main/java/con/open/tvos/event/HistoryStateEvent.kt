package con.open.tvos.event

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
data class HistoryStateEvent(val type: Int) {
    companion object {
        const val TYPE_TOP = 0
    }
}
