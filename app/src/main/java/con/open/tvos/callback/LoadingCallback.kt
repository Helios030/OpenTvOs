package con.open.tvos.callback

import con.open.tvos.R
import com.kingja.loadsir.callback.Callback

/**
 * @author pj567
 * @date :2020/12/24
 * @description:
 */
class LoadingCallback : Callback() {
    override fun onCreateView(): Int = R.layout.loadsir_loading_layout
}
