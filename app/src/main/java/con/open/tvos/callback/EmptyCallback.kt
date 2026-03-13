package con.open.tvos.callback

import con.open.tvos.R
import com.kingja.loadsir.callback.Callback

/**
 * @author pj567
 * @date :2020/12/24
 * @description:
 */
class EmptyCallback : Callback() {
    override fun onCreateView(): Int = R.layout.loadsir_empty_layout
}
