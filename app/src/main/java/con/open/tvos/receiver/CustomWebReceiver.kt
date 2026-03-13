package con.open.tvos.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
class CustomWebReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION == intent.action && intent.extras != null) {
            val action = intent.extras?.getString("action")
            when (action) {
                REFRESH_PARSE -> {
                    /*val name = intent.extras?.getString("name")
                    val url = intent.extras?.getString("url")*/
                    return
                }
                REFRESH_LIVE -> return
                else -> return
            }
            /*callbacks.forEach { callback ->
                callback.onChange(action, refreshObj)
            }*/
        }
    }

    interface Callback {
        fun onChange(action: String, obj: Any?)
    }

    companion object {
        const val ACTION = "android.content.movie.custom.web.Action"
        const val REFRESH_SOURCE = "source"
        const val REFRESH_LIVE = "live"
        const val REFRESH_PARSE = "parse"

        val callbacks: MutableList<Callback> = ArrayList()
    }
}
