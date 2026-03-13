package con.open.tvos.ui.fragment

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.FileCallback
import com.lzy.okgo.model.Progress
import com.lzy.okgo.model.Response
import com.orhanobut.hawk.Hawk
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.BaseActivity
import con.open.tvos.base.BaseLazyFragment
import con.open.tvos.bean.IJKCode
import con.open.tvos.bean.SourceBean
import con.open.tvos.event.RefreshEvent
import con.open.tvos.player.thirdparty.RemoteTVBox
import con.open.tvos.ui.activity.HomeActivity
import con.open.tvos.ui.activity.SettingActivity
import con.open.tvos.ui.adapter.ApiHistoryDialogAdapter
import con.open.tvos.ui.adapter.SelectDialogAdapter
import con.open.tvos.ui.dialog.*
import con.open.tvos.util.*
import org.greenrobot.eventbus.EventBus
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.File

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
class ModelSettingFragment : BaseLazyFragment() {

    private lateinit var tvDebugOpen: TextView
    private lateinit var tvMediaCodec: TextView
    private lateinit var tvParseWebView: TextView
    private lateinit var tvPlay: TextView
    private lateinit var tvRender: TextView
    private lateinit var tvScale: TextView
    private lateinit var tvApi: TextView
    private lateinit var tvHomeApi: TextView
    private lateinit var tvDns: TextView
    private lateinit var tvHomeRec: TextView
    private lateinit var tvHistoryNum: TextView
    private lateinit var tvSearchView: TextView
    private lateinit var tvShowPreviewText: TextView
    private lateinit var tvFastSearchText: TextView
    private lateinit var tvm3u8AdText: TextView
    private lateinit var tvRecStyleText: TextView
    private lateinit var tvIjkCachePlay: TextView
    private lateinit var tvHomeDefaultShow: TextView

    companion object {
        fun newInstance(): ModelSettingFragment {
            return ModelSettingFragment()
        }

        var loadingSearchRemoteTvDialog: SearchRemoteTvDialog? = null
        var remoteTvHostList: MutableList<String> = ArrayList()
        var foundRemoteTv = false
    }

    override fun getLayoutResID(): Int {
        return R.layout.fragment_model
    }

    override fun init() {
        tvFastSearchText = findViewById(R.id.showFastSearchText)
        tvFastSearchText.text = if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) "开启" else "关闭"
        
        tvm3u8AdText = findViewById(R.id.m3u8AdText)
        tvm3u8AdText.text = if (Hawk.get(HawkConfig.M3U8_PURIFY, false)) "开启" else "关闭"
        
        tvRecStyleText = findViewById(R.id.showRecStyleText)
        tvRecStyleText.text = if (Hawk.get(HawkConfig.HOME_REC_STYLE, false)) "是" else "否"
        
        tvShowPreviewText = findViewById(R.id.showPreviewText)
        tvShowPreviewText.text = if (Hawk.get(HawkConfig.SHOW_PREVIEW, true)) "开启" else "关闭"
        
        tvDebugOpen = findViewById(R.id.tvDebugOpen)
        tvParseWebView = findViewById(R.id.tvParseWebView)
        tvMediaCodec = findViewById(R.id.tvMediaCodec)
        tvPlay = findViewById(R.id.tvPlay)
        tvRender = findViewById(R.id.tvRenderType)
        tvScale = findViewById(R.id.tvScaleType)
        tvApi = findViewById(R.id.tvApi)
        tvHomeApi = findViewById(R.id.tvHomeApi)
        tvDns = findViewById(R.id.tvDns)
        tvHomeRec = findViewById(R.id.tvHomeRec)
        tvHistoryNum = findViewById(R.id.tvHistoryNum)
        tvSearchView = findViewById(R.id.tvSearchView)
        tvIjkCachePlay = findViewById(R.id.tvIjkCachePlay)
        
        tvMediaCodec.text = Hawk.get(HawkConfig.IJK_CODEC, "硬解码")
        tvDebugOpen.text = if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) "已打开" else "已关闭"
        tvParseWebView.text = if (Hawk.get(HawkConfig.PARSE_WEBVIEW, true)) "系统自带" else "XWalkView"
        tvApi.text = Hawk.get(HawkConfig.API_URL, "")

        tvDns.text = OkGoHelper.dnsHttpsList[Hawk.get(HawkConfig.DOH_URL, 0)]
        tvHomeRec.text = getHomeRecName(Hawk.get(HawkConfig.HOME_REC, 0))
        tvHistoryNum.text = HistoryHelper.getHistoryNumName(Hawk.get(HawkConfig.HISTORY_NUM, 0))
        tvSearchView.text = getSearchView(Hawk.get(HawkConfig.SEARCH_VIEW, 0))
        tvHomeApi.text = ApiConfig.get().homeSourceBean.name
        tvScale.text = PlayerHelper.getScaleName(Hawk.get(HawkConfig.PLAY_SCALE, 0))
        tvPlay.text = PlayerHelper.getPlayerName(Hawk.get(HawkConfig.PLAY_TYPE, 0))
        tvRender.text = PlayerHelper.getRenderName(Hawk.get(HawkConfig.PLAY_RENDER, 0))
        tvIjkCachePlay.text = if (Hawk.get(HawkConfig.IJK_CACHE_PLAY, false)) "开启" else "关闭"
        
        tvHomeDefaultShow = findViewById(R.id.tvHomeText)
        tvHomeDefaultShow.text = if (Hawk.get(HawkConfig.DEFAULT_LOAD_LIVE, false)) "直播" else "点播"

        findViewById<View>(R.id.llDebug).setOnClickListener {
            FastClickCheckUtil.check(it)
            Hawk.put(HawkConfig.DEBUG_OPEN, !Hawk.get(HawkConfig.DEBUG_OPEN, false))
            tvDebugOpen.text = if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) "已打开" else "已关闭"
        }

        findViewById<View>(R.id.llParseWebVew).setOnClickListener {
            FastClickCheckUtil.check(it)
            val useSystem = !Hawk.get(HawkConfig.PARSE_WEBVIEW, true)
            Hawk.put(HawkConfig.PARSE_WEBVIEW, useSystem)
            tvParseWebView.text = if (Hawk.get(HawkConfig.PARSE_WEBVIEW, true)) "系统自带" else "XWalkView"
            if (!useSystem) {
                Toast.makeText(mContext, "注意: XWalkView只适用于部分低Android版本，Android5.0以上推荐使用系统自带", Toast.LENGTH_LONG).show()
                val dialog = XWalkInitDialog(mContext)
                dialog.setOnListener(object : XWalkInitDialog.OnListener {
                    override fun onchange() {}
                })
                dialog.show()
            }
        }

        findViewById<View>(R.id.llBackup).setOnClickListener {
            FastClickCheckUtil.check(it)
            BackupDialog(mActivity).show()
        }

        findViewById<View>(R.id.llAbout).setOnClickListener {
            FastClickCheckUtil.check(it)
            AboutDialog(mActivity).show()
        }

        findViewById<View>(R.id.llWp).setOnClickListener {
            FastClickCheckUtil.check(it)
            if (ApiConfig.get().wallpaper.isNotEmpty()) {
                OkGo.get<File>(ApiConfig.get().wallpaper)
                    .execute(object : FileCallback(requireActivity().filesDir.absolutePath, "wp") {
                        override fun onSuccess(response: Response<File>) {
                            (requireActivity() as BaseActivity).changeWallpaper(true)
                        }

                        override fun onError(response: Response<File>) {
                            super.onError(response)
                        }

                        override fun downloadProgress(progress: Progress) {
                            super.downloadProgress(progress)
                        }
                    })
            }
        }

        findViewById<View>(R.id.llWpRecovery).setOnClickListener {
            FastClickCheckUtil.check(it)
            val wp = File(requireActivity().filesDir.absolutePath + "/wp")
            if (wp.exists()) wp.delete()
            (requireActivity() as BaseActivity).changeWallpaper(true)
        }

        findViewById<View>(R.id.llHomeApi).setOnClickListener {
            FastClickCheckUtil.check(it)
            val sites = ApiConfig.get().switchSourceBeanList
            if (sites.isNotEmpty()) {
                val dialog = SelectDialog<SourceBean>(mActivity)
                dialog.setTip("请选择首页数据源")
                var select = sites.indexOf(ApiConfig.get().homeSourceBean)
                if (select < 0) select = 0
                dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<SourceBean> {
                    override fun click(value: SourceBean, pos: Int) {
                        ApiConfig.get().setSourceBean(value)
                        tvHomeApi.text = ApiConfig.get().homeSourceBean.name

                        val intent = Intent(mContext, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                        val bundle = Bundle()
                        bundle.putBoolean("useCache", true)
                        intent.putExtras(bundle)
                        startActivity(intent)
                    }

                    override fun getDisplay(val: SourceBean): String {
                        return val.name
                    }
                }, object : DiffUtil.ItemCallback<SourceBean>() {
                    override fun areItemsTheSame(oldItem: SourceBean, newItem: SourceBean): Boolean {
                        return oldItem === newItem
                    }

                    override fun areContentsTheSame(oldItem: SourceBean, newItem: SourceBean): Boolean {
                        return oldItem.key == newItem.key
                    }
                }, sites, select)
                dialog.show()
            }
        }

        findViewById<View>(R.id.llDns).setOnClickListener {
            FastClickCheckUtil.check(it)
            val dohUrl = Hawk.get(HawkConfig.DOH_URL, 0)

            val dialog = SelectDialog<String>(mActivity)
            dialog.setTip("请选择安全DNS")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<String> {
                override fun click(value: String, pos: Int) {
                    tvDns.text = OkGoHelper.dnsHttpsList[pos]
                    Hawk.put(HawkConfig.DOH_URL, pos)
                    IjkMediaPlayer.toggleDotPort(pos > 0)
                }

                override fun getDisplay(val: String): String {
                    return `val`
                }
            }, object : DiffUtil.ItemCallback<String>() {
                override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                    return oldItem == newItem
                }
            }, OkGoHelper.dnsHttpsList, dohUrl)
            dialog.show()
        }

        findViewById<View>(R.id.llApi).setOnClickListener {
            FastClickCheckUtil.check(it)
            val dialog = ApiDialog(mActivity)
            EventBus.getDefault().register(dialog)
            dialog.setOnListener(object : ApiDialog.OnListener {
                override fun onchange(api: String) {
                    Hawk.put(HawkConfig.API_URL, api)
                    tvApi.text = api
                }
            })
            dialog.setOnDismissListener(DialogInterface.OnDismissListener {
                (mActivity as BaseActivity).hideSysBar()
                EventBus.getDefault().unregister(it)
            })
            dialog.show()
        }

        findViewById<View>(R.id.llApiHistory).setOnClickListener {
            val history: ArrayList<String> = Hawk.get(HawkConfig.API_HISTORY, ArrayList())
            if (history.isEmpty()) return@setOnClickListener
            val current: String = Hawk.get(HawkConfig.API_URL, "")
            var idx = 0
            if (history.contains(current)) idx = history.indexOf(current)
            val dialog = ApiHistoryDialog(mActivity)
            dialog.setTip("历史配置列表")
            dialog.setAdapter(object : ApiHistoryDialogAdapter.SelectDialogInterface {
                override fun click(value: String) {
                    Hawk.put(HawkConfig.API_URL, value)
                    Hawk.put(HawkConfig.LIVE_API_URL, value)
                    HistoryHelper.setLiveApiHistory(value)
                    tvApi.text = value
                    dialog.dismiss()
                }

                override fun del(value: String, data: ArrayList<String>) {
                    Hawk.put(HawkConfig.API_HISTORY, data)
                }
            }, history, idx)
            dialog.show()
        }

        findViewById<View>(R.id.llMediaCodec).setOnClickListener {
            val ijkCodes = ApiConfig.get().ijkCodes
            if (ijkCodes.isNullOrEmpty()) return@setOnClickListener
            FastClickCheckUtil.check(it)

            var defaultPos = 0
            val ijkSel: String = Hawk.get(HawkConfig.IJK_CODEC, "硬解码")
            for (j in ijkCodes.indices) {
                if (ijkSel == ijkCodes[j].name) {
                    defaultPos = j
                    break
                }
            }

            val dialog = SelectDialog<IJKCode>(mActivity)
            dialog.setTip("请选择IJK解码")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<IJKCode> {
                override fun click(value: IJKCode, pos: Int) {
                    value.selected(true)
                    tvMediaCodec.text = value.name
                }

                override fun getDisplay(val: IJKCode): String {
                    return val.name
                }
            }, object : DiffUtil.ItemCallback<IJKCode>() {
                override fun areItemsTheSame(oldItem: IJKCode, newItem: IJKCode): Boolean {
                    return oldItem === newItem
                }

                override fun areContentsTheSame(oldItem: IJKCode, newItem: IJKCode): Boolean {
                    return oldItem.name == newItem.name
                }
            }, ijkCodes, defaultPos)
            dialog.show()
        }

        findViewById<View>(R.id.llScale).setOnClickListener {
            FastClickCheckUtil.check(it)
            val defaultPos = Hawk.get(HawkConfig.PLAY_SCALE, 0)
            val players = arrayListOf(0, 1, 2, 3, 4, 5)
            val dialog = SelectDialog<Int>(mActivity)
            dialog.setTip("请选择默认画面缩放")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int> {
                override fun click(value: Int, pos: Int) {
                    Hawk.put(HawkConfig.PLAY_SCALE, value)
                    tvScale.text = PlayerHelper.getScaleName(value)
                }

                override fun getDisplay(val: Int): String {
                    return PlayerHelper.getScaleName(`val`)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, players, defaultPos)
            dialog.show()
        }

        findViewById<View>(R.id.llPlay).setOnClickListener {
            FastClickCheckUtil.check(it)
            val playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0)
            var defaultPos = 0
            val players = PlayerHelper.getExistPlayerTypes()
            val renders = ArrayList<Int>()
            for (p in players.indices) {
                renders.add(p)
                if (players[p] == playerType) {
                    defaultPos = p
                }
            }
            val dialog = SelectDialog<Int>(mActivity)
            dialog.setTip("请选择默认播放器")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int> {
                override fun click(value: Int, pos: Int) {
                    val thisPlayerType = players[pos]
                    Hawk.put(HawkConfig.PLAY_TYPE, thisPlayerType)
                    tvPlay.text = PlayerHelper.getPlayerName(thisPlayerType)
                    PlayerHelper.init()
                }

                override fun getDisplay(val: Int): String {
                    val playerType = players[`val`]
                    return PlayerHelper.getPlayerName(playerType)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, renders, defaultPos)
            dialog.show()
        }

        findViewById<View>(R.id.llRender).setOnClickListener {
            FastClickCheckUtil.check(it)
            val defaultPos = Hawk.get(HawkConfig.PLAY_RENDER, 0)
            val renders = arrayListOf(0, 1)
            val dialog = SelectDialog<Int>(mActivity)
            dialog.setTip("请选择默认渲染方式")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int> {
                override fun click(value: Int, pos: Int) {
                    Hawk.put(HawkConfig.PLAY_RENDER, value)
                    tvRender.text = PlayerHelper.getRenderName(value)
                    PlayerHelper.init()
                }

                override fun getDisplay(val: Int): String {
                    return PlayerHelper.getRenderName(`val`)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, renders, defaultPos)
            dialog.show()
        }

        findViewById<View>(R.id.llHomeRec).setOnClickListener {
            FastClickCheckUtil.check(it)
            val defaultPos = Hawk.get(HawkConfig.HOME_REC, 0)
            val types = arrayListOf(0, 1, 2)
            val dialog = SelectDialog<Int>(mActivity)
            dialog.setTip("请选择首页列表数据")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int> {
                override fun click(value: Int, pos: Int) {
                    Hawk.put(HawkConfig.HOME_REC, value)
                    tvHomeRec.text = getHomeRecName(value)
                }

                override fun getDisplay(val: Int): String {
                    return getHomeRecName(`val`)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, types, defaultPos)
            dialog.show()
        }

        findViewById<View>(R.id.llSearchView).setOnClickListener {
            FastClickCheckUtil.check(it)
            val defaultPos = Hawk.get(HawkConfig.SEARCH_VIEW, 0)
            val types = arrayListOf(0, 1)
            val dialog = SelectDialog<Int>(mActivity)
            dialog.setTip("请选择搜索视图")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int> {
                override fun click(value: Int, pos: Int) {
                    Hawk.put(HawkConfig.SEARCH_VIEW, value)
                    tvSearchView.text = getSearchView(value)
                }

                override fun getDisplay(val: Int): String {
                    return getSearchView(`val`)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, types, defaultPos)
            dialog.show()
        }

        SettingActivity.callback = object : SettingActivity.DevModeCallback {
            override fun onChange() {
                findViewById<View>(R.id.llDebug).visibility = View.VISIBLE
            }
        }

        findViewById<View>(R.id.showPreview).setOnClickListener {
            FastClickCheckUtil.check(it)
            Hawk.put(HawkConfig.SHOW_PREVIEW, !Hawk.get(HawkConfig.SHOW_PREVIEW, true))
            tvShowPreviewText.text = if (Hawk.get(HawkConfig.SHOW_PREVIEW, true)) "开启" else "关闭"
        }

        findViewById<View>(R.id.llHistoryNum).setOnClickListener {
            FastClickCheckUtil.check(it)
            val defaultPos = Hawk.get(HawkConfig.HISTORY_NUM, 0)
            val types = arrayListOf(0, 1, 2)
            val dialog = SelectDialog<Int>(mActivity)
            dialog.setTip("保留历史记录数量")
            dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<Int> {
                override fun click(value: Int, pos: Int) {
                    Hawk.put(HawkConfig.HISTORY_NUM, value)
                    tvHistoryNum.text = HistoryHelper.getHistoryNumName(value)
                }

                override fun getDisplay(val: Int): String {
                    return HistoryHelper.getHistoryNumName(`val`)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, types, defaultPos)
            dialog.show()
        }

        findViewById<View>(R.id.showFastSearch).setOnClickListener {
            FastClickCheckUtil.check(it)
            Hawk.put(HawkConfig.FAST_SEARCH_MODE, !Hawk.get(HawkConfig.FAST_SEARCH_MODE, false))
            tvFastSearchText.text = if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) "开启" else "关闭"
        }

        findViewById<View>(R.id.m3u8Ad).setOnClickListener {
            FastClickCheckUtil.check(it)
            val isPurify = Hawk.get(HawkConfig.M3U8_PURIFY, false)
            Hawk.put(HawkConfig.M3U8_PURIFY, !isPurify)
            tvm3u8AdText.text = if (!isPurify) "开启" else "关闭"
        }

        findViewById<View>(R.id.llHomeRecStyle).setOnClickListener {
            FastClickCheckUtil.check(it)
            Hawk.put(HawkConfig.HOME_REC_STYLE, !Hawk.get(HawkConfig.HOME_REC_STYLE, false))
            tvRecStyleText.text = if (Hawk.get(HawkConfig.HOME_REC_STYLE, false)) "是" else "否"
        }

        findViewById<View>(R.id.llSearchTv).setOnClickListener { view ->
            FastClickCheckUtil.check(view)
            loadingSearchRemoteTvDialog = SearchRemoteTvDialog(mActivity)
            EventBus.getDefault().register(loadingSearchRemoteTvDialog!!)
            loadingSearchRemoteTvDialog!!.setTip("搜索附近TVBox")
            loadingSearchRemoteTvDialog!!.setOnDismissListener(DialogInterface.OnDismissListener {
                EventBus.getDefault().unregister(loadingSearchRemoteTvDialog)
            })
            loadingSearchRemoteTvDialog!!.show()

            val tv = RemoteTVBox()
            remoteTvHostList = ArrayList()
            foundRemoteTv = false
            view.postDelayed({
                Thread {
                    RemoteTVBox.searchAvalible(object : RemoteTVBox.Callback {
                        override fun found(viewHost: String, end: Boolean) {
                            remoteTvHostList.add(viewHost)
                            if (end) {
                                foundRemoteTv = true
                                EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SETTING_SEARCH_TV))
                            }
                        }

                        override fun fail(all: Boolean, end: Boolean) {
                            if (end) {
                                foundRemoteTv = if (all) false else true
                                EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SETTING_SEARCH_TV))
                            }
                        }
                    })
                }.start()
            }, 500)
        }

        //下次进入
        findViewById<View>(R.id.tvHomeLive).setOnClickListener {
            FastClickCheckUtil.check(it)
            Hawk.put(HawkConfig.DEFAULT_LOAD_LIVE, !Hawk.get(HawkConfig.DEFAULT_LOAD_LIVE, false))
            tvHomeDefaultShow.text = if (Hawk.get(HawkConfig.DEFAULT_LOAD_LIVE, false)) "直播" else "点播"
        }

        findViewById<View>(R.id.llIjkCachePlay).setOnClickListener { view -> onClickIjkCachePlay(view) }
        findViewById<View>(R.id.llClearCache).setOnClickListener { view -> onClickClearCache(view) }
    }

    private fun onClickIjkCachePlay(v: View) {
        FastClickCheckUtil.check(v)
        Hawk.put(HawkConfig.IJK_CACHE_PLAY, !Hawk.get(HawkConfig.IJK_CACHE_PLAY, false))
        tvIjkCachePlay.text = if (Hawk.get(HawkConfig.IJK_CACHE_PLAY, false)) "开启" else "关闭"
    }

    private fun onClickClearCache(v: View) {
        FastClickCheckUtil.check(v)
        val cachePath = FileUtils.getCachePath()
        val cacheDir = File(cachePath)
        val cspCachePath = FileUtils.getFilePath() + "/csp/"
        val cspCacheDir = File(cspCachePath)
        if (!cacheDir.exists() && !cspCacheDir.exists()) return
        Thread {
            try {
                if (cacheDir.exists()) FileUtils.cleanDirectory(cacheDir)
                if (cspCacheDir.exists()) FileUtils.cleanDirectory(cspCacheDir)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        Toast.makeText(context, "播放&JAR缓存已清空", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        SettingActivity.callback = null
    }

    private fun getHomeRecName(type: Int): String {
        return when (type) {
            1 -> "站点推荐"
            2 -> "观看历史"
            else -> "豆瓣热播"
        }
    }

    private fun getSearchView(type: Int): String {
        return if (type == 0) "文字列表" else "缩略图"
    }
}
