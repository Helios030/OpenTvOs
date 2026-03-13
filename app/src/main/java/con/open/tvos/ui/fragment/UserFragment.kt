package con.open.tvos.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import com.chad.library.adapter.base.BaseQuickAdapter
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.BaseLazyFragment
import con.open.tvos.bean.Movie
import con.open.tvos.bean.SourceBean
import con.open.tvos.bean.VodInfo
import con.open.tvos.cache.RoomDataManger
import con.open.tvos.event.ServerEvent
import con.open.tvos.ui.activity.*
import con.open.tvos.ui.adapter.HomeHotVodAdapter
import con.open.tvos.util.FastClickCheckUtil
import con.open.tvos.util.HawkConfig
import con.open.tvos.util.ImgUtil
import con.open.tvos.util.UA
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Calendar

/**
 * @author pj567
 * @date :2021/3/9
 * @description:
 */
class UserFragment : BaseLazyFragment(), View.OnClickListener {

    private var tvLive: LinearLayout? = null
    private var tvSearch: LinearLayout? = null
    private var tvSetting: LinearLayout? = null
    private var tvHistory: LinearLayout? = null
    private var tvCollect: LinearLayout? = null
    private var tvPush: LinearLayout? = null
    private var homeSourceRec: MutableList<Movie.Video>? = null
    private var style: ImgUtil.Style? = null

    companion object {
        var homeHotVodAdapter: HomeHotVodAdapter? = null
        var tvHotList: TvRecyclerView? = null

        fun newInstance(): UserFragment = UserFragment()

        fun newInstance(recVod: MutableList<Movie.Video>?): UserFragment {
            return UserFragment().apply {
                this.homeSourceRec = recVod
            }
        }
    }

    override fun onFragmentResume() {
        super.onFragmentResume()
        if (Hawk.get(HawkConfig.HOME_REC_STYLE, false)) {
            tvHotList?.apply {
                visibility = View.VISIBLE
                setHasFixedSize(true)
                var spanCount = 5
                if (style != null && Hawk.get(HawkConfig.HOME_REC, 0) == 1) {
                    spanCount = ImgUtil.spanCountByStyle(style!!, spanCount)
                }
                layoutManager = V7GridLayoutManager(mContext, spanCount)
                val paddingLeft = resources.getDimensionPixelSize(R.dimen.vs_15)
                val paddingTop = resources.getDimensionPixelSize(R.dimen.vs_10)
                val paddingRight = resources.getDimensionPixelSize(R.dimen.vs_15)
                val paddingBottom = resources.getDimensionPixelSize(R.dimen.vs_10)
                setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
            }
        } else {
            tvHotList?.apply {
                visibility = View.VISIBLE
                layoutManager = V7LinearLayoutManager(mContext, V7LinearLayoutManager.HORIZONTAL, false)
                val paddingLeft = resources.getDimensionPixelSize(R.dimen.vs_15)
                val paddingTop = resources.getDimensionPixelSize(R.dimen.vs_40)
                val paddingRight = resources.getDimensionPixelSize(R.dimen.vs_15)
                val paddingBottom = resources.getDimensionPixelSize(R.dimen.vs_40)
                setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
            }
        }

        if (Hawk.get(HawkConfig.HOME_REC, 0) == 2) {
            val allVodRecord = RoomDataManger.getAllVodRecord(20)
            val vodList = mutableListOf<Movie.Video>()
            for (vodInfo in allVodRecord) {
                val vod = Movie.Video().apply {
                    id = vodInfo.id
                    sourceKey = vodInfo.sourceKey
                    name = vodInfo.name
                    pic = vodInfo.pic
                    note = if (!vodInfo.playNote.isNullOrEmpty()) {
                        "上次看到${vodInfo.playNote}"
                    } else {
                        null
                    }
                }
                vodList.add(vod)
            }
            homeHotVodAdapter?.setNewData(vodList)
        }
    }

    override fun getLayoutResID(): Int = R.layout.fragment_user

    private fun jumpSearch(vod: Movie.Video) {
        val newIntent = if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
            Intent(mContext, FastSearchActivity::class.java)
        } else {
            Intent(mContext, SearchActivity::class.java)
        }
        newIntent.putExtra("title", vod.name)
        newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        mActivity.startActivity(newIntent)
    }

    override fun init() {
        EventBus.getDefault().register(this)

        tvLive = findViewById(R.id.tvLive)
        tvSearch = findViewById(R.id.tvSearch)
        tvSetting = findViewById(R.id.tvSetting)
        tvCollect = findViewById(R.id.tvFavorite)
        tvHistory = findViewById(R.id.tvHistory)
        tvPush = findViewById(R.id.tvPush)

        listOf(tvLive, tvSearch, tvSetting, tvHistory, tvPush, tvCollect).forEach { view ->
            view?.setOnClickListener(this)
            view?.setOnFocusChangeListener(focusChangeListener)
        }

        tvHotList = findViewById(R.id.tvHotList)

        if (Hawk.get(HawkConfig.HOME_REC, 0) == 1 && homeSourceRec != null) {
            style = ImgUtil.initStyle()
        }

        val tvRate = when (Hawk.get(HawkConfig.HOME_REC, 0)) {
            0 -> "豆瓣热播"
            1 -> if (homeSourceRec != null) "站点推荐" else "豆瓣热播"
            else -> ""
        }

        homeHotVodAdapter = HomeHotVodAdapter(style, tvRate).apply {
            setOnItemClickListener { adapter, view, position ->
                if (ApiConfig.get().sourceBeanList.isEmpty()) return@setOnItemClickListener

                val vod = adapter.getItem(position) as? Movie.Video ?: return@setOnItemClickListener

                if (!vod.id.isNullOrEmpty() && Hawk.get(HawkConfig.HOME_REC, 0) == 2 && HawkConfig.hotVodDelete) {
                    homeHotVodAdapter?.remove(position)
                    val vodInfo = RoomDataManger.getVodInfo(vod.sourceKey, vod.id)
                    vodInfo?.let {
                        RoomDataManger.deleteVodRecord(vod.sourceKey, it)
                    }
                    Toast.makeText(mContext, "已删除当前记录", Toast.LENGTH_SHORT).show()
                } else if (!vod.id.isNullOrEmpty()) {
                    val bundle = Bundle().apply {
                        putString("id", vod.id)
                        putString("sourceKey", vod.sourceKey)
                    }
                    val sourceBean = ApiConfig.get().getSource(vod.sourceKey)
                    if (sourceBean != null) {
                        bundle.putString("picture", vod.pic)
                        jumpActivity(DetailActivity::class.java, bundle)
                    } else {
                        jumpSearch(vod)
                    }
                } else {
                    jumpSearch(vod)
                }
            }

            setOnItemLongClickListener { adapter, view, position ->
                if (ApiConfig.get().sourceBeanList.isEmpty()) return@setOnItemLongClickListener false

                val vod = adapter.getItem(position) as? Movie.Video ?: return@setOnItemLongClickListener false

                if (!vod.id.isNullOrEmpty() && Hawk.get(HawkConfig.HOME_REC, 0) == 2) {
                    HawkConfig.hotVodDelete = !HawkConfig.hotVodDelete
                    homeHotVodAdapter?.notifyDataSetChanged()
                } else {
                    val bundle = Bundle().apply {
                        putString("title", vod.name)
                    }
                    jumpActivity(FastSearchActivity::class.java, bundle)
                }
                true
            }
        }

        tvHotList?.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                itemView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(300)
                    ?.setInterpolator(BounceInterpolator())?.start()
            }

            override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                itemView?.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.setDuration(300)
                    ?.setInterpolator(BounceInterpolator())?.start()
            }

            override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
                // No action
            }
        })

        tvHotList?.adapter = homeHotVodAdapter

        initHomeHotVod(homeHotVodAdapter!!)
    }

    private fun initHomeHotVod(adapter: HomeHotVodAdapter) {
        when (Hawk.get(HawkConfig.HOME_REC, 0)) {
            1 -> {
                homeSourceRec?.let {
                    adapter.setNewData(it)
                    return
                }
            }
            2 -> return
        }
        setDouBanData(adapter)
    }

    private fun setDouBanData(adapter: HomeHotVodAdapter) {
        try {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DATE)
            val today = "${year}${month}${day}"
            val requestDay: String = Hawk.get("home_hot_day", "")

            if (requestDay == today) {
                val json: String = Hawk.get("home_hot", "")
                if (json.isNotEmpty()) {
                    val hotMovies = loadHots(json)
                    if (!hotMovies.isNullOrEmpty()) {
                        adapter.setNewData(hotMovies)
                        return
                    }
                }
            }

            val doubanUrl = "https://movie.douban.com/j/new_search_subjects?sort=U&range=0,10&tags=&playable=1&start=0&year_range=$year,$year"

            OkGo.get<String>(doubanUrl)
                .headers("User-Agent", UA.randomOne())
                .execute(object : AbsCallback<String>() {
                    override fun onSuccess(response: Response<String>) {
                        val netJson = response.body()
                        Hawk.put("home_hot_day", today)
                        Hawk.put("home_hot", netJson)
                        mActivity.runOnUiThread {
                            adapter.setNewData(loadHots(netJson))
                        }
                    }

                    override fun convertResponse(response: okhttp3.Response): String {
                        return response.body?.string() ?: ""
                    }
                })
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    private fun loadHots(json: String): ArrayList<Movie.Video> {
        val result = ArrayList<Movie.Video>()
        try {
            val infoJson = Gson().fromJson(json, JsonObject::class.java)
            val array = infoJson.getAsJsonArray("data")
            val limit = Math.min(array.size(), 25)

            for (i in 0 until limit) {
                val obj = array[i].asJsonObject
                val vod = Movie.Video().apply {
                    name = obj.get("title").asString
                    note = obj.get("rate").asString
                    if (!note.isNullOrEmpty()) {
                        note = "$note 分"
                    }
                    pic = obj.get("cover").asString +
                            "@User-Agent=" + UA.randomOne() +
                            "@Referer=https://www.douban.com/"
                }
                result.add(vod)
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
        return result
    }

    private val focusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
        if (hasFocus) {
            v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300)
                .setInterpolator(BounceInterpolator()).start()
        } else {
            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300)
                .setInterpolator(BounceInterpolator()).start()
        }
    }

    override fun onClick(v: View) {
        HawkConfig.hotVodDelete = false
        FastClickCheckUtil.check(v)

        when (v.id) {
            R.id.tvLive -> {
                val liveGroupList: JsonArray = Hawk.get(HawkConfig.LIVE_GROUP_LIST, JsonArray())
                if (liveGroupList.isEmpty()) {
                    Toast.makeText(mContext, "直播源为空", Toast.LENGTH_SHORT).show()
                } else {
                    jumpActivity(LivePlayActivity::class.java)
                }
            }
            R.id.tvSearch -> jumpActivity(SearchActivity::class.java)
            R.id.tvSetting -> jumpActivity(SettingActivity::class.java)
            R.id.tvHistory -> jumpActivity(HistoryActivity::class.java)
            R.id.tvPush -> jumpActivity(PushActivity::class.java)
            R.id.tvFavorite -> jumpActivity(CollectActivity::class.java)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun server(event: ServerEvent) {
        if (event.type == ServerEvent.SERVER_CONNECTION) {
            // Handle server connection event
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}
