package con.open.tvos.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import con.open.tvos.event.ServerEvent
import con.open.tvos.ui.activity.SearchActivity
import con.open.tvos.util.AppManager
import org.greenrobot.eventbus.EventBus

/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
class SearchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION == intent.action && intent.extras != null) {
            if (AppManager.getInstance().getActivity(SearchActivity::class.java) != null) {
                AppManager.getInstance().backActivity(SearchActivity::class.java)
                EventBus.getDefault().post(ServerEvent(ServerEvent.SERVER_SEARCH, intent.extras?.getString("title")))
            } else {
                Intent(context, SearchActivity::class.java).apply {
                    putExtra("title", intent.extras?.getString("title"))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    context.startActivity(this)
                }
            }
        }
    }

    companion object {
        const val ACTION = "android.content.movie.search.Action"
    }
}
