package con.open.tvos.bean

import con.open.tvos.util.HawkConfig
import com.orhanobut.hawk.Hawk

/**
 * @author pj567
 * @date :2021/3/8
 * @description:
 */
class IJKCode {
    var name: String? = null
    var option: LinkedHashMap<String, String>? = null
    private var selected: Boolean = false

    fun selected(selected: Boolean) {
        this.selected = selected
        if (selected) {
            Hawk.put(HawkConfig.IJK_CODEC, name)
        }
    }

    fun isSelected(): Boolean = selected
}
