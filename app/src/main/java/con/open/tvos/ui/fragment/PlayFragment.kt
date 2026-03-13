package con.open.tvos.ui.fragment

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import con.open.tvos.crawler.Spider
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.App
import con.open.tvos.base.BaseLazyFragment
import con.open.tvos.bean.ParseBean
import con.open.tvos.bean.SourceBean
import con.open.tvos.bean.Subtitle
import con.open.tvos.bean.VodInfo
import con.open.tvos.cache.CacheManager
import con.open.tvos.event.RefreshEvent
import con.open.tvos.player.ExoPlayer
import con.open.tvos.player.IjkMediaPlayer
import con.open.tvos.player.MyVideoView
import con.open.tvos.player.TrackInfo
import con.open.tvos.player.TrackInfoBean
import con.open.tvos.player.controller.VodController
import con.open.tvos.server.ControlManager
import con.open.tvos.ui.adapter.SelectDialogAdapter
import con.open.tvos.ui.dialog.SearchSubtitleDialog
import con.open.tvos.ui.dialog.SelectDialog
import con.open.tvos.ui.dialog.SubtitleDialog
import con.open.tvos.util.AdBlocker
import con.open.tvos.util.DefaultConfig
import con.open.tvos.util.FileUtils
import con.open.tvos.util.HawkConfig
import con.open.tvos.util.LOG
import con.open.tvos.util.MD5
import con.open.tvos.util.PlayerHelper
import con.open.tvos.util.VideoParseRuler
import con.open.tvos.util.XWalkUtils
import con.open.tvos.util.parser.SuperParse
import con.open.tvos.util.thunder.Jianpian
import con.open.tvos.util.thunder.Thunder
import con.open.tvos.viewmodel.SourceViewModel
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.HttpHeaders
import com.lzy.okgo.model.Response
import com.obsez.android.lib.filechooser.ChooserDialog
import com.orhanobut.hawk.Hawk
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONException
import org.json.JSONObject
import org.xwalk.core.XWalkJavascriptResult
import org.xwalk.core.XWalkResourceClient
import org.xwalk.core.XWalkSettings
import org.xwalk.core.XWalkUIClient
import org.xwalk.core.XWalkView
import org.xwalk.core.XWalkWebResourceRequest
import org.xwalk.core.XWalkWebResourceResponse
import java.io.ByteArrayInputStream
import java.io.File
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import me.jessyan.autosize.AutoSize
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkTimedText
import xyz.doikki.videoplayer.player.AbstractPlayer
import xyz.doikki.videoplayer.player.ProgressManager

class PlayFragment : BaseLazyFragment() {

    private var mVideoView: MyVideoView? = null
    private var mPlayLoadTip: TextView? = null
    private var mPlayLoadErr: ImageView? = null
    private var mPlayLoading: ProgressBar? = null
    private lateinit var mController: VodController
    private var sourceViewModel: SourceViewModel? = null
    private var mHandler: Handler? = null

    private val videoDuration: Long = -1

    override fun getLayoutResID(): Int = R.layout.activity_play

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refresh(event: RefreshEvent) {
        if (event.type == RefreshEvent.TYPE_SUBTITLE_SIZE_CHANGE) {
            mController.mSubtitleView.setTextSize(event.obj as Int)
        }
    }

    override fun init() {
        initView()
        initViewModel()
        initData()
        Hawk.put(HawkConfig.PLAYER_IS_LIVE, false)
    }

    fun getSavedProgress(url: String?): Long {
        var st = 0
        try {
            st = mVodPlayerCfg?.getInt("st") ?: 0
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val skip = st * 1000L
        val theCache = url?.let { CacheManager.getCache(MD5.string2MD5(it)) }
        if (theCache == null) {
            return skip
        }
        val rec: Long = when (theCache) {
            is Long -> theCache
            is String -> {
                try {
                    theCache.toLong()
                } catch (e: NumberFormatException) {
                    LOG.i("echo-String value is not a valid long.")
                    0L
                }
            }
            else -> {
                LOG.i("echo-Value cannot be converted to long.")
                0L
            }
        }
        return maxOf(rec, skip)
    }

    private fun initView() {
        EventBus.getDefault().register(this)
        mHandler = Handler(Looper.getMainLooper()) { msg ->
            when (msg.what) {
                100 -> {
                    stopParse()
                    errorWithRetry("嗅探错误", false)
                }
            }
            false
        }
        mVideoView = findViewById(R.id.mVideoView)
        mPlayLoadTip = findViewById(R.id.play_load_tip)
        mPlayLoading = findViewById(R.id.play_loading)
        mPlayLoadErr = findViewById(R.id.play_load_error)
        mController = VodController(requireContext())
        mController.setCanChangePosition(true)
        mController.setEnableInNormal(true)
        mController.setGestureEnabled(true)
        val progressManager = object : ProgressManager() {
            override fun saveProgress(url: String, progress: Long) {
                CacheManager.save(MD5.string2MD5(url), progress)
            }

            override fun getSavedProgress(url: String): Long {
                return this@PlayFragment.getSavedProgress(url)
            }
        }
        mVideoView?.setProgressManager(progressManager)
        mController.setListener(object : VodController.VodControlListener {
            override fun playNext(rmProgress: Boolean) {
                val preProgressKey = progressKey
                this@PlayFragment.playNext(rmProgress)
                if (rmProgress && preProgressKey != null) {
                    CacheManager.delete(MD5.string2MD5(preProgressKey), 0)
                }
            }

            override fun playPre() {
                this@PlayFragment.playPrevious()
            }

            override fun changeParse(pb: ParseBean) {
                autoRetryCount = 0
                doParse(pb)
            }

            override fun updatePlayerCfg() {
                mVodInfo?.playerCfg = mVodPlayerCfg.toString()
                mVodPlayerCfg?.let { EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, it)) }
            }

            override fun replay(replay: Boolean) {
                autoRetryCount = 0
                if (replay) {
                    play(true)
                } else {
                    if (!webPlayUrl.isNullOrEmpty()) {
                        stopParse()
                        initParseLoadFound()
                        mVideoView?.release()
                        goPlayUrl(webPlayUrl, webHeaderMap)
                    } else {
                        play(false)
                    }
                }
            }

            override fun errReplay() {
                errorWithRetry("视频播放出错", false)
            }

            override fun selectSubtitle() {
                try {
                    selectMySubtitle()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun selectAudioTrack() {
                selectMyAudioTrack()
            }

            override fun prepared() {
                initSubtitleView()
            }

            override fun startPlayUrl(url: String, headers: HashMap<String, String>) {
                goPlayUrl(url, headers)
            }

            override fun setAllowSwitchPlayer(isAllow: Boolean) {
                allowSwitchPlayer = isAllow
            }
        })
        mVideoView?.setVideoController(mController)
    }

    private fun setSubtitle(path: String?) {
        if (!path.isNullOrEmpty()) {
            mController.mSubtitleView.visibility = View.GONE
            mController.mSubtitleView.setSubtitlePath(path)
            mController.mSubtitleView.visibility = View.VISIBLE
        }
    }

    @Throws(Exception::class)
    private fun selectMySubtitle() {
        val subtitleDialog = SubtitleDialog(activity)
        val playerType = mVodPlayerCfg?.getInt("pl") ?: 0
        if (mController.mSubtitleView.hasInternal && playerType == 1) {
            subtitleDialog.selectInternal.visibility = View.VISIBLE
        } else {
            subtitleDialog.selectInternal.visibility = View.GONE
        }
        subtitleDialog.setSubtitleViewListener(object : SubtitleDialog.SubtitleViewListener {
            override fun setTextSize(size: Int) {
                mController.mSubtitleView.setTextSize(size)
            }

            override fun setSubtitleDelay(milliseconds: Int) {
                mController.mSubtitleView.setSubtitleDelay(milliseconds)
            }

            override fun selectInternalSubtitle() {
                selectMyInternalSubtitle()
            }

            override fun setTextStyle(style: Int) {
                setSubtitleViewTextStyle(style)
            }
        })
        subtitleDialog.setSearchSubtitleListener(object : SubtitleDialog.SearchSubtitleListener {
            override fun openSearchSubtitleDialog() {
                val searchSubtitleDialog = SearchSubtitleDialog(activity)
                searchSubtitleDialog.setSubtitleLoader(object : SearchSubtitleDialog.SubtitleLoader {
                    override fun loadSubtitle(subtitle: Subtitle) {
                        if (!isAdded) return
                        requireActivity().runOnUiThread {
                            val zimuUrl = subtitle.url
                            LOG.i("echo-Remote Subtitle Url: $zimuUrl")
                            setSubtitle(zimuUrl)
                            searchSubtitleDialog.dismiss()
                        }
                    }
                })
                if (mVodInfo?.playFlag?.contains("Ali") == true || mVodInfo?.playFlag?.contains("parse") == true) {
                    searchSubtitleDialog.setSearchWord(mVodInfo?.playNote ?: "")
                } else {
                    searchSubtitleDialog.setSearchWord(mVodInfo?.name ?: "")
                }
                searchSubtitleDialog.show()
            }
        })
        subtitleDialog.setLocalFileChooserListener(object : SubtitleDialog.LocalFileChooserListener {
            override fun openLocalFileChooserDialog() {
                ChooserDialog(activity)
                    .withFilter(false, false, "srt", "ass", "scc", "stl", "ttml")
                    .withStartFile("/storage/emulated/0/Download")
                    .withChosenListener(object : ChooserDialog.Result() {
                        override fun onChoosePath(path: String, pathFile: File) {
                            LOG.i("echo-Local Subtitle Path: $path")
                            setSubtitle(path)
                        }
                    })
                    .build()
                    .show()
            }
        })
        subtitleDialog.show()
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    private fun setSubtitleViewTextStyle(style: Int) {
        context?.let { ctx ->
            if (style == 0) {
                mController.mSubtitleView.setTextColor(ctx.resources.getColorStateList(R.color.color_FFFFFF))
            } else if (style == 1) {
                mController.mSubtitleView.setTextColor(ctx.resources.getColorStateList(R.color.color_FFB6C1))
            }
        }
    }

    private fun selectMyAudioTrack() {
        val mediaPlayer = mVideoView?.getMediaPlayer() ?: return
        val trackInfo: TrackInfo? = when (mediaPlayer) {
            is IjkMediaPlayer -> mediaPlayer.trackInfo
            is ExoPlayer -> mediaPlayer.trackInfo
            else -> null
        }
        if (trackInfo == null) {
            Toast.makeText(mContext, "没有音轨", Toast.LENGTH_SHORT).show()
            return
        }
        val bean = trackInfo.audio
        if (bean.isEmpty()) return
        val dialog = SelectDialog<TrackInfoBean>(activity)
        dialog.setTip("切换音轨")
        dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<TrackInfoBean> {
            override fun click(value: TrackInfoBean, pos: Int) {
                try {
                    for (audio in bean) {
                        audio.selected = audio.index == value.index
                    }
                    mediaPlayer.pause()
                    val progress = mediaPlayer.currentPosition
                    when (mediaPlayer) {
                        is IjkMediaPlayer -> mediaPlayer.setTrack(value.index, progressKey)
                        is ExoPlayer -> mediaPlayer.setTrack(value.groupIndex, value.index, progressKey)
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (mediaPlayer is IjkMediaPlayer) {
                            mediaPlayer.seekTo(progress)
                        }
                        mediaPlayer.start()
                    }, 200)
                    dialog.dismiss()
                } catch (e: Exception) {
                    LOG.e("切换音轨出错")
                }
            }

            override fun getDisplay(value: TrackInfoBean): String {
                return "${value.groupIndex}${value.index} . ${value.language} : ${value.name}"
            }
        }, object : DiffUtil.ItemCallback<TrackInfoBean>() {
            override fun areItemsTheSame(@NonNull oldItem: TrackInfoBean, @NonNull newItem: TrackInfoBean): Boolean {
                return oldItem.index == newItem.index
            }

            override fun areContentsTheSame(@NonNull oldItem: TrackInfoBean, @NonNull newItem: TrackInfoBean): Boolean {
                return oldItem.index == newItem.index
            }
        }, bean, trackInfo.getAudioSelected(false))
        dialog.show()
    }

    private fun selectMyInternalSubtitle() {
        val mediaPlayer = mVideoView?.getMediaPlayer() as? IjkMediaPlayer ?: return
        val trackInfo = mediaPlayer.trackInfo
        if (trackInfo == null) {
            Toast.makeText(mContext, "没有内置字幕", Toast.LENGTH_SHORT).show()
            return
        }
        val bean = trackInfo.subtitle
        if (bean.isEmpty()) return
        val dialog = SelectDialog<TrackInfoBean>(activity)
        dialog.setTip("切换内置字幕")
        dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<TrackInfoBean> {
            override fun click(value: TrackInfoBean, pos: Int) {
                try {
                    for (subtitle in bean) {
                        subtitle.selected = subtitle.index == value.index
                    }
                    mediaPlayer.pause()
                    val progress = mediaPlayer.currentPosition
                    mController.mSubtitleView.destroy()
                    mController.mSubtitleView.clearSubtitleCache()
                    mController.mSubtitleView.isInternal = true
                    mediaPlayer.setTrack(value.index)
                    Handler(Looper.getMainLooper()).postDelayed({
                        mediaPlayer.seekTo(progress)
                        mediaPlayer.start()
                    }, 800)
                    dialog.dismiss()
                } catch (e: Exception) {
                    LOG.e("切换内置字幕出错")
                }
            }

            override fun getDisplay(value: TrackInfoBean): String {
                return "${value.index} : ${value.language}"
            }
        }, object : DiffUtil.ItemCallback<TrackInfoBean>() {
            override fun areItemsTheSame(@NonNull oldItem: TrackInfoBean, @NonNull newItem: TrackInfoBean): Boolean {
                return oldItem.index == newItem.index
            }

            override fun areContentsTheSame(@NonNull oldItem: TrackInfoBean, @NonNull newItem: TrackInfoBean): Boolean {
                return oldItem.index == newItem.index
            }
        }, bean, trackInfo.getSubtitleSelected(false))
        dialog.show()
    }

    private fun setTip(msg: String, loading: Boolean, err: Boolean) {
        if (!isAdded) return
        requireActivity().runOnUiThread {
            mPlayLoadTip?.text = msg
            mPlayLoadTip?.visibility = View.VISIBLE
            mPlayLoading?.visibility = if (loading) View.VISIBLE else View.GONE
            mPlayLoadErr?.visibility = if (err) View.VISIBLE else View.GONE
        }
    }

    private fun hideTip() {
        mPlayLoadTip?.visibility = View.GONE
        mPlayLoading?.visibility = View.GONE
        mPlayLoadErr?.visibility = View.GONE
    }

    private fun errorWithRetry(err: String, finish: Boolean) {
        if (!autoRetry()) {
            if (!isAdded) return
            requireActivity().runOnUiThread {
                if (finish) {
                    setTip(err, false, true)
                    Toast.makeText(mContext, err, Toast.LENGTH_SHORT).show()
                } else {
                    setTip(err, false, true)
                }
            }
        }
    }

    private fun playUrl(url: String, headers: HashMap<String, String>?) {
        if (!url.startsWith("data:application")) {
            EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, url))
        }
        if (!Hawk.get(HawkConfig.M3U8_PURIFY, false)) {
            goPlayUrl(url, headers)
            return
        }
        if (url.startsWith("http://127.0.0.1") || !url.contains(".m3u8")) {
            goPlayUrl(url, headers)
            return
        }
        if (DefaultConfig.noAd(mVodInfo?.playFlag)) {
            goPlayUrl(url, headers)
            return
        }
        LOG.i("echo-playM3u8:$url")
        mController.playM3u8(url, headers)
    }

    fun goPlayUrl(url: String, headers: HashMap<String, String>?) {
        LOG.i("echo-goPlayUrl:$url")
        if (autoRetryCount == 0) webPlayUrl = url
        if (mActivity == null) return
        if (!isAdded) return
        val finalUrl = url
        requireActivity().runOnUiThread {
            stopParse()
            mVideoView?.let { videoView ->
                videoView.release()
                var playUrl = finalUrl
                try {
                    val playerType = mVodPlayerCfg?.getInt("pl") ?: 0
                    if (playerType >= 10) {
                        val vs = mVodInfo?.seriesMap?.get(mVodInfo?.playFlag)?.get(mVodInfo?.playIndex ?: 0)
                        val playTitle = "${mVodInfo?.name} ${vs?.name}"
                        setTip("调用外部播放器${PlayerHelper.getPlayerName(playerType)}进行播放", true, false)
                        val progress = getSavedProgress(progressKey)
                        val callResult = PlayerHelper.runExternalPlayer(
                            playerType, requireActivity(), playUrl, playTitle, playSubtitle, headers, progress
                        )
                        setTip("调用外部播放器${PlayerHelper.getPlayerName(playerType)}${if (callResult) "成功" else "失败"}", callResult, !callResult)
                        return
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                hideTip()
                if (playUrl.startsWith("data:application/dash+xml;base64,")) {
                    PlayerHelper.updateCfg(videoView, mVodPlayerCfg, 2)
                    App.getInstance().setDashData(playUrl.split("base64,")[1])
                    playUrl = ControlManager.get().getAddress(true) + "dash/proxy.mpd"
                } else if (playUrl.contains(".mpd") || playUrl.contains("type=mpd")) {
                    PlayerHelper.updateCfg(videoView, mVodPlayerCfg, 2)
                } else {
                    PlayerHelper.updateCfg(videoView, mVodPlayerCfg)
                }
                videoView.setProgressKey(progressKey)
                if (headers != null) {
                    videoView.setUrl(playUrl, headers)
                } else {
                    videoView.setUrl(playUrl)
                }
                videoView.start()
                mController.resetSpeed()
            }
        }
    }

    private fun initSubtitleView() {
        var trackInfo: TrackInfo? = null
        val mediaPlayer = mVideoView?.getMediaPlayer()
        if (mediaPlayer is IjkMediaPlayer) {
            trackInfo = mediaPlayer.trackInfo
            if (trackInfo != null && trackInfo.subtitle.isNotEmpty()) {
                mController.mSubtitleView.hasInternal = true
            }
            mediaPlayer.loadDefaultTrack(trackInfo, progressKey)
            mediaPlayer.setOnTimedTextListener { _, text ->
                if (text == null) return@setOnTimedTextListener
                if (mController.mSubtitleView.isInternal) {
                    val subtitle = com.github.tvbox.osc.subtitle.model.Subtitle()
                    subtitle.content = text.text
                    mController.mSubtitleView.onSubtitleChanged(subtitle)
                }
            }
        }
        if (mediaPlayer is ExoPlayer) {
            mediaPlayer.loadDefaultTrack(progressKey)
        }
        mController.mSubtitleView.bindToMediaPlayer(mVideoView?.getMediaPlayer())
        mController.mSubtitleView.setPlaySubtitleCacheKey(subtitleCacheKey)
        val subtitlePathCache = CacheManager.getCache(MD5.string2MD5(subtitleCacheKey)) as? String
        if (!subtitlePathCache.isNullOrEmpty()) {
            mController.mSubtitleView.setSubtitlePath(subtitlePathCache)
        } else {
            if (!playSubtitle.isNullOrEmpty()) {
                mController.mSubtitleView.setSubtitlePath(playSubtitle)
            } else {
                if (mController.mSubtitleView.hasInternal) {
                    mController.mSubtitleView.isInternal = true
                    if (trackInfo != null && trackInfo.subtitle.isNotEmpty()) {
                        val subtitleTrackList = trackInfo.subtitle
                        val selectedIndex = trackInfo.getSubtitleSelected(true)
                        var hasCh = false
                        for (subtitleTrackInfoBean in subtitleTrackList) {
                            val lowerLang = subtitleTrackInfoBean.language.lowercase()
                            if (lowerLang.contains("zh") || lowerLang.contains("ch")) {
                                hasCh = true
                                if (selectedIndex != subtitleTrackInfoBean.index) {
                                    (mVideoView?.getMediaPlayer() as? IjkMediaPlayer)?.setTrack(subtitleTrackInfoBean.index)
                                    break
                                }
                            }
                        }
                        if (!hasCh) {
                            (mVideoView?.getMediaPlayer() as? IjkMediaPlayer)?.setTrack(subtitleTrackList[0].index)
                        }
                    }
                }
            }
        }
    }

    private fun initViewModel() {
        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)
        sourceViewModel?.playResult?.observe(this) { info ->
            webPlayUrl = null
            if (info != null) {
                try {
                    progressKey = info.optString("proKey", null)
                    val parse = info.optString("parse", "1") == "1"
                    val jx = info.optString("jx", "0") == "1"
                    playSubtitle = info.optString("subt", "")
                    if (playSubtitle?.isEmpty() == true && info.has("subs")) {
                        try {
                            val obj = info.optJSONArray("subs")?.optJSONObject(0)
                            var url = obj?.optString("url", "") ?: ""
                            if (!TextUtils.isEmpty(url) && !FileUtils.hasExtension(url)) {
                                val format = obj?.optString("format", "") ?: ""
                                val name = obj?.optString("name", "字幕") ?: "字幕"
                                val ext = when (format) {
                                    "text/x-ssa" -> ".ass"
                                    "text/vtt" -> ".vtt"
                                    "application/x-subrip" -> ".srt"
                                    "text/lrc" -> ".lrc"
                                    else -> ".srt"
                                }
                                val filename = name + if (name.lowercase().endsWith(ext)) "" else ext
                                url += "#" + mController.encodeUrl(filename)
                            }
                            playSubtitle = url
                        } catch (th: Throwable) {}
                    }
                    subtitleCacheKey = info.optString("subtKey", null)
                    val playUrl = info.optString("playUrl", "")
                    val msg = info.optString("msg", "")
                    if (msg.isNotEmpty()) {
                        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show()
                    }
                    val flag = info.optString("flag")
                    var url = info.getString("url")
                    if (url.startsWith("[")) {
                        url = mController.firstUrlByArray(url)
                    }
                    var headers: HashMap<String, String>? = null
                    webUserAgent = null
                    webHeaderMap = null
                    if (info.has("header")) {
                        try {
                            val hds = JSONObject(info.getString("header"))
                            val keys = hds.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                if (headers == null) headers = HashMap()
                                headers!![key] = hds.getString(key)
                                if (key.equals("user-agent", ignoreCase = true)) {
                                    webUserAgent = hds.getString(key).trim()
                                }
                            }
                            webHeaderMap = headers
                        } catch (th: Throwable) {}
                    }
                    if (parse || jx) {
                        val userJxList = (playUrl.isEmpty() && ApiConfig.get().vipParseFlags.contains(flag)) || jx
                        initParse(flag, userJxList, playUrl, url)
                    } else {
                        mController.showParse(false)
                        playUrl(playUrl + url, headers)
                    }
                } catch (th: Throwable) {}
            } else {
                errorWithRetry("获取播放信息错误", true)
            }
        }
    }

    fun setData(bundle: Bundle) {
        mVodInfo = App.getInstance().vodInfo
        sourceKey = bundle.getString("sourceKey") ?: ""
        sourceBean = ApiConfig.get().getSource(sourceKey)
        initPlayerCfg()
        play(false)
    }

    private fun initData() {}

    private fun initPlayerCfg() {
        try {
            mVodPlayerCfg = JSONObject(mVodInfo?.playerCfg)
        } catch (th: Throwable) {
            mVodPlayerCfg = JSONObject()
        }
        try {
            if (mVodPlayerCfg?.has("pl") == false) {
                mVodPlayerCfg?.put("pl", if (sourceBean?.playerType == -1) Hawk.get(HawkConfig.PLAY_TYPE, 1) else sourceBean?.playerType ?: 0)
            }
            if (mVodPlayerCfg?.has("pr") == false) mVodPlayerCfg?.put("pr", Hawk.get(HawkConfig.PLAY_RENDER, 0))
            if (mVodPlayerCfg?.has("ijk") == false) mVodPlayerCfg?.put("ijk", Hawk.get(HawkConfig.IJK_CODEC, "硬解码"))
            if (mVodPlayerCfg?.has("sc") == false) mVodPlayerCfg?.put("sc", Hawk.get(HawkConfig.PLAY_SCALE, 0))
            if (mVodPlayerCfg?.has("sp") == false) mVodPlayerCfg?.put("sp", 1.0f)
            if (mVodPlayerCfg?.has("st") == false) mVodPlayerCfg?.put("st", 0)
            if (mVodPlayerCfg?.has("et") == false) mVodPlayerCfg?.put("et", 0)
        } catch (th: Throwable) {}
        mVodPlayerCfg?.let { mController.setPlayerConfig(it) }
    }

    fun onBackPressed(): Boolean {
        val requestedOrientation = requireActivity().requestedOrientation
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
            requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT ||
            requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            mController.mLandscapePortraitBtn.text = "竖屏"
        }
        return mController.onBackPressed()
    }

    fun dispatchKeyEvent(event: KeyEvent?): Boolean = event?.let { mController.onKeyEvent(it) } ?: false
    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = event?.let { mController.onKeyDown(keyCode, it) } ?: false
    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean = event?.let { mController.onKeyUp(keyCode, it) } ?: false

    override fun onPause() {
        super.onPause()
        mVideoView?.pause()
    }

    override fun onResume() {
        super.onResume()
        mVideoView?.resume()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) mVideoView?.pause() else mVideoView?.resume()
        super.onHiddenChanged(hidden)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
        mVideoView?.let {
            it.release()
            mVideoView = null
        }
        stopLoadWebView(true)
        stopParse()
        mController.stopOther()
    }

    private var mVodInfo: VodInfo? = null
    private var mVodPlayerCfg: JSONObject? = null
    private var sourceKey: String = ""
    private var sourceBean: SourceBean? = null

    private fun playNext(isProgress: Boolean) {
        val hasNext = mVodInfo?.let { vod ->
            vod.seriesMap[vod.playFlag]?.let { (vod.playIndex + 1) < it.size } ?: false
        } ?: false
        if (!hasNext) {
            Toast.makeText(requireContext(), "已经是最后一集了!", Toast.LENGTH_SHORT).show()
            return
        }
        mVodInfo?.playIndex = mVodInfo?.playIndex?.plus(1) ?: 0
        play(false)
    }

    private fun playPrevious() {
        val hasPre = mVodInfo?.let { (it.playIndex - 1) >= 0 } ?: false
        if (!hasPre) {
            Toast.makeText(requireContext(), "已经是第一集了!", Toast.LENGTH_SHORT).show()
            return
        }
        mVodInfo?.playIndex = mVodInfo?.playIndex?.minus(1) ?: 0
        play(false)
    }

    private var autoRetryCount = 0
    private var lastRetryTime = 0L
    private var allowSwitchPlayer = true

    private fun autoRetry(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRetryTime > 60_000) {
            LOG.i("echo-reset-autoRetryCount")
            autoRetryCount = 0
            allowSwitchPlayer = false
        }
        lastRetryTime = currentTime
        if (!loadFoundVideoUrls.isNullOrEmpty()) {
            autoRetryFromLoadFoundVideoUrls()
            return true
        }
        if (autoRetryCount < 2) {
            if (autoRetryCount == 1) {
                play(false)
                autoRetryCount++
            } else {
                if (!webPlayUrl.isNullOrEmpty()) {
                    if (allowSwitchPlayer) {
                        if (mController.switchPlayer()) autoRetryCount++
                    } else {
                        autoRetryCount++
                        allowSwitchPlayer = true
                    }
                    stopParse()
                    initParseLoadFound()
                    mVideoView?.release()
                    playUrl(webPlayUrl, webHeaderMap)
                } else {
                    play(false)
                    autoRetryCount++
                }
            }
            return true
        }
        autoRetryCount = 0
        return false
    }

    private fun autoRetryFromLoadFoundVideoUrls() {
        val videoUrl = loadFoundVideoUrls?.poll() ?: return
        val header = loadFoundVideoUrlsHeader?.get(videoUrl)
        playUrl(videoUrl, header)
    }

    private fun initParseLoadFound() {
        loadFoundCount.set(0)
        loadFoundVideoUrls = LinkedList()
        loadFoundVideoUrlsHeader = HashMap()
    }

    fun setPlayTitle(show: Boolean) {
        val playTitleInfo = if (show && mVodInfo != null) {
            "${mVodInfo?.name} ${mVodInfo?.seriesMap?.get(mVodInfo?.playFlag)?.get(mVodInfo?.playIndex ?: 0)?.name}"
        } else ""
        mController.setTitle(playTitleInfo)
    }

    fun play(reset: Boolean) {
        if (mVodInfo == null) return
        val vs = mVodInfo?.seriesMap?.get(mVodInfo?.playFlag)?.get(mVodInfo?.playIndex ?: 0) ?: return
        EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, mVodInfo?.playIndex ?: 0))
        setTip("正在获取播放信息", true, false)
        mController.setTitle("${mVodInfo?.name} ${vs.name}")

        stopParse()
        initParseLoadFound()
        allowSwitchPlayer = true
        mController.stopOther()
        mVideoView?.release()
        subtitleCacheKey = "${mVodInfo?.sourceKey}-${mVodInfo?.id}-${mVodInfo?.playFlag}-${mVodInfo?.playIndex}-${vs.name}-subt"
        progressKey = "${mVodInfo?.sourceKey}${mVodInfo?.id}${mVodInfo?.playFlag}${mVodInfo?.playIndex}${vs.name}"
        if (reset) {
            CacheManager.delete(MD5.string2MD5(progressKey), 0)
            CacheManager.delete(MD5.string2MD5(subtitleCacheKey), 0)
        } else {
            try {
                val playerType = mVodPlayerCfg?.getInt("pl") ?: 0
                mController.mSubtitleView.visibility = if (playerType == 1) View.VISIBLE else View.GONE
            } catch (e: JSONException) { e.printStackTrace() }
        }

        if (Jianpian.isJpUrl(vs.url)) {
            mController.showParse(false)
            val decodedUrl = if (vs.url.startsWith("tvbox-xg:")) Jianpian.JPUrlDec(vs.url.substring(9)) else Jianpian.JPUrlDec(vs.url)
            playUrl(decodedUrl, null)
            return
        }
        if (Thunder.play(vs.url, object : Thunder.ThunderCallback {
            override fun status(code: Int, info: String) { setTip(info, code >= 0, code < 0) }
            override fun list(urlMap: Map<Int, String>) {}
            override fun play(url: String) { playUrl(url, null) }
        })) {
            mController.showParse(false)
            return
        }
        sourceViewModel?.getPlay(sourceKey, mVodInfo?.playFlag, progressKey, vs.url, subtitleCacheKey)
    }

    private var playSubtitle: String? = null
    private var subtitleCacheKey: String? = null
    private var progressKey: String? = null
    private var parseFlag: String? = null
    private var webUrl: String? = null
    private var webUserAgent: String? = null
    private var webHeaderMap: HashMap<String, String>? = null
    private var webPlayUrl: String? = null

    private fun initParse(flag: String, useParse: Boolean, playUrl: String, url: String) {
        parseFlag = flag
        webUrl = url
        var parseBean: ParseBean? = null
        mController.showParse(useParse)
        if (useParse) {
            parseBean = ApiConfig.get().defaultParse
        } else {
            when {
                playUrl.startsWith("json:") -> parseBean = ParseBean().apply { type = 1; setUrl(playUrl.substring(5)) }
                playUrl.startsWith("parse:") -> {
                    for (pb in ApiConfig.get().parseBeanList) if (pb.name == playUrl.substring(6)) { parseBean = pb; break }
                }
            }
            if (parseBean == null) parseBean = ParseBean().apply { type = 0; setUrl(playUrl) }
        }
        doParse(parseBean)
    }

    @Throws(JSONException::class)
    private fun jsonParse(input: String, json: String): JSONObject? {
        val jsonPlayData = JSONObject(json)
        var url = if (jsonPlayData.has("data")) jsonPlayData.getJSONObject("data").getString("url") else jsonPlayData.getString("url")
        if (url.startsWith("//")) url = "http:$url"
        if (!url.startsWith("http")) return null
        val headers = JSONObject()
        jsonPlayData.optString("user-agent", "")?.takeIf { it.trim().isNotEmpty() }?.let { headers.put("User-Agent", " $it") }
        jsonPlayData.optString("referer", "")?.takeIf { it.trim().isNotEmpty() }?.let { headers.put("Referer", " $it") }
        return JSONObject().apply { put("header", headers); put("url", url) }
    }

    private fun stopParse() {
        mHandler?.removeMessages(100)
        stopLoadWebView(false)
        OkGo.getInstance().cancelTag("json_jx")
        parseThreadPool?.let {
            try { it.shutdown(); parseThreadPool = null } catch (th: Throwable) { th.printStackTrace() }
        }
    }

    private var parseThreadPool: ExecutorService? = null

    private fun doParse(pb: ParseBean) {
        stopParse()
        initParseLoadFound()
        when (pb.type) {
            4 -> parseMix(pb, true)
            0 -> {
                setTip("正在嗅探播放地址", true, false)
                mHandler?.removeMessages(100)
                mHandler?.sendEmptyMessageDelayed(100, 20 * 1000)
                pb.ext?.let { ext ->
                    try {
                        val reqHeaders = HashMap<String, String>()
                        val jsonObject = JSONObject(ext)
                        if (jsonObject.has("header")) {
                            val headerJson = jsonObject.optJSONObject("header")
                            headerJson?.keys()?.forEach { key ->
                                if (key.equals("user-agent", ignoreCase = true)) webUserAgent = headerJson.getString(key).trim()
                                else reqHeaders[key] = headerJson.optString(key, "")
                            }
                            if (reqHeaders.isNotEmpty()) webHeaderMap = reqHeaders
                        }
                    } catch (e: Throwable) { e.printStackTrace() }
                }
                loadWebView(pb.url + webUrl)
            }
            1 -> {
                setTip("正在解析播放地址", true, false)
                val reqHeaders = HttpHeaders()
                try {
                    JSONObject(pb.ext ?: "").optJSONObject("header")?.keys()?.forEach { key ->
                        reqHeaders.put(key, JSONObject(pb.ext ?: "").optJSONObject("header")?.optString(key, "") ?: "")
                    }
                } catch (e: Throwable) { e.printStackTrace() }
                OkGo.get<String>(pb.url + mController.encodeUrl(webUrl))
                    .tag("json_jx").headers(reqHeaders)
                    .execute(object : AbsCallback<String>() {
                        override fun convertResponse(response: okhttp3.Response) = response.body?.string() ?: throw IllegalStateException("网络请求错误")
                        override fun onSuccess(response: Response<String>) {
                            try {
                                val rs = jsonParse(webUrl ?: "", response.body())
                                var headers: HashMap<String, String>? = null
                                rs?.optJSONObject("header")?.keys()?.forEach { key ->
                                    if (headers == null) headers = HashMap()
                                    headers!![key] = rs.getJSONObject("header").getString(key)
                                }
                                rs?.let { playUrl(it.getString("url"), headers) }
                            } catch (e: Throwable) { errorWithRetry("解析错误", false) }
                        }
                        override fun onError(response: Response<String>) { super.onError(response); errorWithRetry("解析错误", false) }
                    })
            }
            2 -> {
                setTip("正在解析播放地址", true, false)
                parseThreadPool = Executors.newSingleThreadExecutor()
                val jxs = LinkedHashMap<String, String>()
                ApiConfig.get().parseBeanList.filter { it.type == 1 }.forEach { jxs[it.name] = it.mixUrl() }
                parseThreadPool?.execute {
                    val rs = ApiConfig.get().jsonExt(pb.url, jxs, webUrl)
                    if (rs == null || !rs.has("url") || rs.optString("url").isEmpty()) setTip("解析错误", false, true)
                    else {
                        var headers: HashMap<String, String>? = null
                        rs.optJSONObject("header")?.keys()?.forEach { key ->
                            if (headers == null) headers = HashMap()
                            headers!![key] = rs.getJSONObject("header").getString(key)
                        }
                        if (rs.has("jxFrom") && isAdded) requireActivity().runOnUiThread {
                            Toast.makeText(mContext, "解析来自:" + rs.optString("jxFrom"), Toast.LENGTH_SHORT).show()
                        }
                        if (rs.optInt("parse", 0) == 1) loadUrl(DefaultConfig.checkReplaceProxy(rs.optString("url", "")))
                        else playUrl(rs.optString("url", ""), headers)
                    }
                }
            }
            3 -> parseMix(pb, false)
        }
    }

    private fun parseMix(pb: ParseBean, isSuper: Boolean) {
        setTip("正在解析播放地址", true, false)
        parseThreadPool = Executors.newSingleThreadExecutor()
        val jxs = LinkedHashMap<String, HashMap<String, String>>()
        val jsonJxs = LinkedHashMap<String, String>()
        var extendName = ""
        ApiConfig.get().parseBeanList.forEach { p ->
            val data = HashMap<String, String>().apply {
                put("url", p.url); put("type", p.type.toString()); put("ext", p.ext ?: "")
            }
            if (p.url == pb.url) extendName = p.name
            jxs[p.name] = data
            if (p.type == 1) jsonJxs[p.name] = p.mixUrl()
        }
        val finalExtendName = extendName
        parseThreadPool?.execute {
            if (isSuper) {
                val rs = SuperParse.parse(jxs, parseFlag + "123", webUrl)
                if (!rs.has("url") || rs.optString("url").isEmpty()) setTip("解析错误", false, true)
                else if (rs.has("parse") && rs.optInt("parse", 0) == 1) {
                    if (rs.has("ua")) webUserAgent = rs.optString("ua").trim()
                    setTip("超级解析中", true, false)
                    if (isAdded) requireActivity().runOnUiThread {
                        stopParse(); mHandler?.removeMessages(100); mHandler?.sendEmptyMessageDelayed(100, 20 * 1000)
                        loadWebView(DefaultConfig.checkReplaceProxy(rs.optString("url", "")))
                    }
                    parseThreadPool?.execute { rsJsonJX(SuperParse.doJsonJx(webUrl), true) }
                } else rsJsonJX(rs, false)
            } else {
                val rs = ApiConfig.get().jsonExtMix(parseFlag + "111", pb.url, finalExtendName, jxs, webUrl)
                if (rs == null || !rs.has("url") || rs.optString("url").isEmpty()) setTip("解析错误", false, true)
                else if (rs.has("parse") && rs.optInt("parse", 0) == 1) {
                    if (rs.has("ua")) webUserAgent = rs.optString("ua").trim()
                    if (isAdded) requireActivity().runOnUiThread {
                        stopParse(); setTip("正在嗅探播放地址", true, false)
                        mHandler?.removeMessages(100); mHandler?.sendEmptyMessageDelayed(100, 20 * 1000)
                        loadWebView(DefaultConfig.checkReplaceProxy(rs.optString("url", "")))
                    }
                } else rsJsonJX(rs, false)
            }
        }
    }

    private fun rsJsonJX(rs: JSONObject?, isSuper: Boolean) {
        if (isSuper && (rs == null || !rs.has("url"))) return
        if (isSuper) stopLoadWebView(false)
        var headers: HashMap<String, String>? = null
        rs?.optJSONObject("header")?.keys()?.forEach { key ->
            if (headers == null) headers = HashMap()
            headers!![key] = rs.getJSONObject("header").getString(key)
        }
        if (rs?.has("jxFrom") == true && isAdded) requireActivity().runOnUiThread {
            Toast.makeText(mContext, "解析来自:" + rs.optString("jxFrom"), Toast.LENGTH_SHORT).show()
        }
        rs?.let { playUrl(it.optString("url", ""), headers) }
    }

    fun getPlayer(): MyVideoView? = mVideoView

    private var mXwalkWebView: XWalkView? = null
    private var mSysWebView: WebView? = null
    private val loadedUrls = HashMap<String, Boolean>()
    private var loadFoundVideoUrls: LinkedList<String>? = null
    private var loadFoundVideoUrlsHeader: HashMap<String, HashMap<String, String>>? = null
    private val loadFoundCount = AtomicInteger(0)

    private fun loadWebView(url: String) {
        if (mSysWebView == null && mXwalkWebView == null) {
            if (!Hawk.get(HawkConfig.PARSE_WEBVIEW, true)) {
                XWalkUtils.tryUseXWalk(mContext, object : XWalkUtils.XWalkState {
                    override fun success() { initWebView(false); loadUrl(url) }
                    override fun fail() { Toast.makeText(mContext, "XWalkView不兼容，已替换为系统自带WebView", Toast.LENGTH_SHORT).show(); initWebView(true); loadUrl(url) }
                    override fun ignore() { Toast.makeText(mContext, "XWalkView运行组件未下载，已替换为系统自带WebView", Toast.LENGTH_SHORT).show(); initWebView(true); loadUrl(url) }
                })
            } else { initWebView(true); loadUrl(url) }
        } else loadUrl(url)
    }

    private fun initWebView(useSystemWebView: Boolean) {
        if (useSystemWebView) { mSysWebView = MyWebView(mContext); configWebViewSys(mSysWebView!!) }
        else { mXwalkWebView = MyXWalkView(mContext); configWebViewX5(mXwalkWebView!!) }
    }

    private fun loadUrl(url: String) {
        if (!isAdded) return
        requireActivity().runOnUiThread {
            mXwalkWebView?.let { it.stopLoading(); webUserAgent?.let { ua -> it.settings.userAgentString = ua }; if (webHeaderMap != null) it.loadUrl(url, webHeaderMap) else it.loadUrl(url) }
            mSysWebView?.let { it.stopLoading(); webUserAgent?.let { ua -> it.settings.userAgentString = ua }; if (webHeaderMap != null) it.loadUrl(url, webHeaderMap) else it.loadUrl(url) }
        }
    }

    private fun stopLoadWebView(destroy: Boolean) {
        if (mActivity == null || !isAdded) return
        requireActivity().runOnUiThread {
            mXwalkWebView?.let { it.stopLoading(); it.loadUrl("about:blank"); if (destroy) { it.clearCache(true); it.removeAllViews(); it.onDestroy(); mXwalkWebView = null } }
            mSysWebView?.let { it.stopLoading(); it.loadUrl("about:blank"); if (destroy) { it.clearCache(true); it.removeAllViews(); it.destroy(); mSysWebView = null } }
        }
    }

    private fun checkVideoFormat(url: String): Boolean = try {
        if (url.contains("url=http") || url.contains(".html")) false
        else if (sourceBean?.type == 3) ApiConfig.get().getCSP(sourceBean)?.takeIf { it.manualVideoCheck() }?.isVideoFormat(url) ?: VideoParseRuler.checkIsVideoForParse(webUrl, url)
        else VideoParseRuler.checkIsVideoForParse(webUrl, url)
    } catch (e: Exception) { false }

    internal inner class MyWebView(context: android.content.Context) : WebView(context) {
        override fun setOverScrollMode(mode: Int) { super.setOverScrollMode(mode); if (mContext is Activity) AutoSize.autoConvertDensityOfCustomAdapt(mContext as Activity, this@PlayFragment) }
        override fun dispatchKeyEvent(event: KeyEvent) = false
    }

    internal inner class MyXWalkView(context: android.content.Context) : XWalkView(context) {
        override fun setOverScrollMode(mode: Int) { super.setOverScrollMode(mode); if (mContext is Activity) AutoSize.autoConvertDensityOfCustomAdapt(mContext as Activity, this@PlayFragment) }
        override fun dispatchKeyEvent(event: KeyEvent) = false
    }
