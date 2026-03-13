package con.open.tvos.util

import android.util.Log

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
object LOG {
    private const val TAG = "TVBox-runtime"

    @JvmStatic
    fun e(msg: String?) {
        Log.e(TAG, msg?.toString() ?: "")
    }

    @JvmStatic
    fun i(msg: String?) {
        Log.i(TAG, msg?.toString() ?: "")
    }
}
