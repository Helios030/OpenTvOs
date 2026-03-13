package con.open.tvos.event

/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
data class ServerEvent(val type: Int, val obj: Any? = null) {
    companion object {
        const val SERVER_SUCCESS = 0
        const val SERVER_CONNECTION = 1
        const val SERVER_SEARCH = 2
    }
}
