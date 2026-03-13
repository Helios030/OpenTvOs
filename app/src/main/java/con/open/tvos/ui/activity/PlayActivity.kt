package con.open.tvos.ui.activity

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import con.open.tvos.crawler.Spider
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.App
import con.open.tvos.base.BaseActivity
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
import org.jetbrains.annotations.NotNull
import org.json.JSONException
import org.json.JSONObject
import org.xwalk.core.*
import java.io.ByteArrayInputStream
import java.io.File
import java.util.HashMap
import java.util.Iterator
import java.util.LinkedHashMap
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import me.jessyan.autosize.AutoSize
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkTimedText
import xyz.doikki.videoplayer.player.AbstractPlayer
import xyz.doikki.videoplayer.player.ProgressManager

class PlayActivity : BaseActivity() {
    private lateinit var mVideoView: MyVideoView
    private lateinit var mPlayLoadTip: TextView
    private lateinit var mPlayLoadErr: ImageView
    private lateinit var mPlayLoading: ProgressBar
    private lateinit var mController: VodController
    private lateinit var sourceViewModel: SourceViewModel
    private lateinit var mHandler: Handler

    private var videoDuration: Long = -1

    override fun getLayoutResID(): Int = R.layout.activity_play

    override fun init() {
        initView()
        initViewModel()
        initData()
        Hawk.put(HawkConfig.PLAYER_IS_LIVE, false)
    }

    fun getSavedProgress(url: String): Long {
        var st = 0
        try {
            st = mVodPlayerCfg.getInt("st")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val skip = st * 1000L
        if (CacheManager.getCache(MD5.string2MD5(url)) == null) {
            return skip
        }
        val rec = CacheManager.getCache(MD5.string2MD5(url)) as Long
        return if (rec < skip) skip else rec
    }

    private fun initView() {
        mHandler = Handler(Handler.Callback { msg ->
            when (msg.what) {
                100 -> {
                    stopParse()
                    errorWithRetry("嗅探错误", false)
                }
            }
            false
        })
        mVideoView = findViewById(R.id.mVideoView)
        mPlayLoadTip = findViewById(R.id.play_load_tip)
        mPlayLoading = findViewById(R.id.play_loading)
        mPlayLoadErr = findViewById(R.id.play_load_error)
        mController = VodController(this)
        mController.setCanChangePosition(true)
        mController.setEnableInNormal(true)
        mController.setGestureEnabled(true)
        val progressManager = object : ProgressManager() {
            override fun saveProgress(url: String, progress: Long) {
                if (videoDuration == 0L) return
                CacheManager.save(MD5.string2MD5(url), progress)
            }

            override fun getSavedProgress(url: String): Long {
                return this@PlayActivity.getSavedProgress(url)
            }
        }
        mVideoView.setProgressManager(progressManager)
        mController.setListener(object : VodController.VodControlListener {
            override fun playNext(rmProgress: Boolean) {
                val preProgressKey = progressKey
                this@PlayActivity.playNext(rmProgress)
                if (rmProgress && preProgressKey != null)
                    CacheManager.delete(MD5.string2MD5(preProgressKey), 0)
            }

            override fun playPre() {
                this@PlayActivity.playPrevious()
            }

            override fun changeParse(pb: ParseBean) {
                autoRetryCount = 0
                doParse(pb)
            }

            override fun updatePlayerCfg() {
                mVodInfo.playerCfg = mVodPlayerCfg.toString()
                EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, mVodPlayerCfg))
            }

            override fun replay(replay: Boolean) {
                autoRetryCount = 0
                if (replay) {
                    play(true)
                } else {
                    if (webPlayUrl != null && webPlayUrl.isNotEmpty()) {
                        stopParse()
                        initParseLoadFound()
                        if (::mVideoView.isInitialized) mVideoView.release()
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
        mVideoView.setVideoController(mController)
    }

    fun setSubtitle(path: String?) {
        if (path != null && path.isNotEmpty()) {
            mController.mSubtitleView.visibility = View.GONE
            mController.mSubtitleView.setSubtitlePath(path)
            mController.mSubtitleView.visibility = View.VISIBLE
        }
    }

    @Throws(Exception::class)
    fun selectMySubtitle() {
        val subtitleDialog = SubtitleDialog(this@PlayActivity)
        val playerType = mVodPlayerCfg.getInt("pl")
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
                val searchSubtitleDialog = SearchSubtitleDialog(this@PlayActivity)
                searchSubtitleDialog.setSubtitleLoader(object : SearchSubtitleDialog.SubtitleLoader {
                    override fun loadSubtitle(subtitle: Subtitle) {
                        runOnUiThread {
                            val zimuUrl = subtitle.url
                            LOG.i("echo-Remote Subtitle Url: $zimuUrl")
                            setSubtitle(zimuUrl)
                            searchSubtitleDialog.dismiss()
                        }
                    }
                })
                if (mVodInfo.playFlag.contains("Ali") || mVodInfo.playFlag.contains("parse")) {
                    searchSubtitleDialog.setSearchWord(mVodInfo.playNote)
                } else {
                    searchSubtitleDialog.setSearchWord(mVodInfo.name)
                }
                searchSubtitleDialog.show()
            }
        })
        subtitleDialog.setLocalFileChooserListener(object : SubtitleDialog.LocalFileChooserListener {
            override fun openLocalFileChooserDialog() {
                ChooserDialog(this@PlayActivity)
                    .withFilter(false, false, "srt", "ass", "scc", "stl", "ttml")
                    .withStartFile("/storage/emulated/0/Download")
                    .withChosenListener(object : ChooserDialog.Result {
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

    fun setSubtitleViewTextStyle(style: Int) {
        if (style == 0) {
            mController.mSubtitleView.setTextColor(baseContext.resources.getColorStateList(R.color.color_FFFFFF))
        } else if (style == 1) {
            mController.mSubtitleView.setTextColor(baseContext.resources.getColorStateList(R.color.color_FFB6C1))
        }
    }

    fun selectMyAudioTrack() {
        val mediaPlayer = mVideoView.getMediaPlayer()
        var trackInfo: TrackInfo? = null
        if (mediaPlayer is IjkMediaPlayer) {
            trackInfo = mediaPlayer.trackInfo
        }
        if (mediaPlayer is ExoPlayer) {
            trackInfo = mediaPlayer.trackInfo
        }
        if (trackInfo == null) {
            Toast.makeText(mContext, "没有音轨", Toast.LENGTH_SHORT).show()
            return
        }
        val bean = trackInfo.audio
        if (bean.size < 1) return
        val dialog = SelectDialog<TrackInfoBean>(this@PlayActivity)
        dialog.setTip("切换音轨")
        dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<TrackInfoBean> {
            override fun click(value: TrackInfoBean, pos: Int) {
                try {
                    for (audio in bean) {
                        audio.selected = audio.index == value.index
                    }
                    mediaPlayer.pause()
                    val progress = mediaPlayer.currentPosition
                    if (mediaPlayer is IjkMediaPlayer) mediaPlayer.setTrack(value.index, progressKey)
                    if (mediaPlayer is ExoPlayer) mediaPlayer.setTrack(value.groupIndex, value.index, progressKey)
                    Handler().postDelayed({
                        if (mediaPlayer is IjkMediaPlayer) mediaPlayer.seekTo(progress)
                        mediaPlayer.start()
                    }, 200)
                    dialog.dismiss()
                } catch (e: Exception) {
                    LOG.e("切换音轨出错")
                }
            }

            override fun getDisplay(`val`: TrackInfoBean): String {
                return "${`val`.groupIndex}${`val`.index} . ${`val`.language} : ${`val`.name}"
            }
        }, object : DiffUtil.ItemCallback<TrackInfoBean>() {
            override fun areItemsTheSame(@NonNull @NotNull oldItem: TrackInfoBean, @NonNull @NotNull newItem: TrackInfoBean): Boolean {
                return oldItem.index == newItem.index
            }

            override fun areContentsTheSame(@NonNull @NotNull oldItem: TrackInfoBean, @NonNull @NotNull newItem: TrackInfoBean): Boolean {
                return oldItem.index == newItem.index
            }
        }, bean, trackInfo.getAudioSelected(false))
        dialog.show()
    }

    fun selectMyInternalSubtitle() {
        val mediaPlayer = mVideoView.getMediaPlayer()
        if (mediaPlayer !is IjkMediaPlayer) {
            return
        }
        var trackInfo: TrackInfo? = null
        trackInfo = mediaPlayer.trackInfo
        if (trackInfo == null) {
            Toast.makeText(mContext, "没有内置字幕", Toast.LENGTH_SHORT).show()
            return
        }
        val bean = trackInfo.subtitle
        if (bean.size < 1) return
        val dialog = SelectDialog<TrackInfoBean>(this@PlayActivity)
        dialog.setTip("切换内置字幕")
        dialog.setAdapter(object : SelectDialogAdapter.SelectDialogInterface<TrackInfoBean> {
            override fun click(value: TrackInfoBean, pos: Int) {
                try {
                    for (subtitle in bean) {
                        subtitle.selected = subtitle.index == value.index
                    }
                    mediaPlayer.pause()
                    val progress = mediaPlayer.currentPosition
                    if (mediaPlayer is IjkMediaPlayer) {
                        mController.mSubtitleView.destroy()
                        mController.mSubtitleView.clearSubtitleCache()
                        mController.mSubtitleView.isInternal = true
                        mediaPlayer.setTrack(value.index)
                        Handler().postDelayed({
                            mediaPlayer.seekTo(progress)
                            mediaPlayer.start()
                        }, 800)
                    }
                    dialog.dismiss()
                } catch (e: Exception) {
                    LOG.e("切换内置字幕出错")
                }
            }

            override fun getDisplay(`val`: TrackInfoBean): String {
                return "${`val`.index} : ${`val`.language}"
            }
        }, object : DiffUtil.ItemCallback<TrackInfoBean>() {
            override fun areItemsTheSame(@NonNull @NotNull oldItem: TrackInfoBean, @NonNull @NotNull newItem: TrackInfoBean): Boolean {
                return oldItem.index == newItem.index
            }

            override fun areContentsTheSame(@NonNull @NotNull oldItem: TrackInfoBean, @NonNull @NotNull newItem: TrackInfoBean): Boolean {
                return oldItem.index == newItem.index
            }
        }, bean, trackInfo.getSubtitleSelected(false))
        dialog.show()
    }

    fun setTip(msg: String, loading: Boolean, err: Boolean) {
        runOnUiThread {
            mPlayLoadTip.text = msg
            mPlayLoadTip.visibility = View.VISIBLE
            mPlayLoading.visibility = if (loading) View.VISIBLE else View.GONE
            mPlayLoadErr.visibility = if (err) View.VISIBLE else View.GONE
        }
    }

    fun hideTip() {
        mPlayLoadTip.visibility = View.GONE
        mPlayLoading.visibility = View.GONE
        mPlayLoadErr.visibility = View.GONE
    }

    fun errorWithRetry(err: String, finish: Boolean) {
        if (!autoRetry()) {
            runOnUiThread {
                if (finish) {
                    setTip(err, false, true)
                    Toast.makeText(mContext, err, Toast.LENGTH_SHORT).show()
                    this@PlayActivity.finish()
                } else {
                    setTip(err, false, true)
                }
            }
        }
    }

    fun playUrl(url: String, headers: HashMap<String, String>?) {
        if (!url.startsWith("data:application")) EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, url))
        if (!Hawk.get(HawkConfig.M3U8_PURIFY, false)) {
            goPlayUrl(url, headers)
            return
        }
        if (url.startsWith("http://127.0.0.1") || !url.contains(".m3u8")) {
            goPlayUrl(url, headers)
            return
        }
        if (DefaultConfig.noAd(mVodInfo.playFlag)) {
            goPlayUrl(url, headers)
            return
        }
        LOG.i("echo-playM3u8:$url")
        mController.playM3u8(url, headers)
    }

    fun goPlayUrl(url: String, headers: HashMap<String, String>?) {
        LOG.i("echo-goPlayUrl:$url")
        if (autoRetryCount == 0) webPlayUrl = url
        val finalUrl = url
        runOnUiThread {
            stopParse()
            if (::mVideoView.isInitialized) {
                mVideoView.release()
                var playUrl = finalUrl
                try {
                    val playerType = mVodPlayerCfg.getInt("pl")
                    if (playerType >= 10) {
                        val vs = mVodInfo.seriesMap[mVodInfo.playFlag]!![mVodInfo.playIndex]
                        val playTitle = "${mVodInfo.name} ${vs.name}"
                        setTip("调用外部播放器${PlayerHelper.getPlayerName(playerType)}进行播放", true, false)
                        val progress = getSavedProgress(progressKey!!)
                        val callResult = PlayerHelper.runExternalPlayer(playerType, this@PlayActivity, playUrl, playTitle, playSubtitle, headers, progress)
                        setTip("调用外部播放器${PlayerHelper.getPlayerName(playerType)}${if (callResult) "成功" else "失败"}", callResult, !callResult)
                        return@runOnUiThread
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                hideTip()
                if (playUrl.startsWith("data:application/dash+xml;base64,")) {
                    PlayerHelper.updateCfg(mVideoView, mVodPlayerCfg, 2)
                    App.getInstance().setDashData(playUrl.split("base64,")[1])
                    playUrl = ControlManager.get().getAddress(true) + "dash/proxy.mpd"
                } else if (playUrl.contains(".mpd") || playUrl.contains("type=mpd")) {
                    PlayerHelper.updateCfg(mVideoView, mVodPlayerCfg, 2)
                } else {
                    PlayerHelper.updateCfg(mVideoView, mVodPlayerCfg)
                }
                mVideoView.setProgressKey(progressKey)
                if (headers != null) {
                    mVideoView.setUrl(playUrl, headers)
                } else {
                    mVideoView.setUrl(playUrl)
                }
                mVideoView.start()
                mController.resetSpeed()
            }
        }
    }

    private fun initSubtitleView() {
        var trackInfo: TrackInfo? = null
        if (mVideoView.getMediaPlayer() is IjkMediaPlayer) {
            trackInfo = (mVideoView.getMediaPlayer() as IjkMediaPlayer).trackInfo
            if (trackInfo != null && trackInfo.subtitle.size > 0) {
                mController.mSubtitleView.hasInternal = true
            }
            (mVideoView.getMediaPlayer() as IjkMediaPlayer).loadDefaultTrack(trackInfo, progressKey)
            (mVideoView.getMediaPlayer() as IjkMediaPlayer).setOnTimedTextListener(IMediaPlayer.OnTimedTextListener { mp, text ->
                if (text == null) return@OnTimedTextListener
                if (mController.mSubtitleView.isInternal) {
                    val subtitle = com.github.tvbox.osc.subtitle.model.Subtitle()
                    subtitle.content = text.text
                    mController.mSubtitleView.onSubtitleChanged(subtitle)
                }
            })
        }
        if (mVideoView.getMediaPlayer() is ExoPlayer) {
            (mVideoView.getMediaPlayer() as ExoPlayer).loadDefaultTrack(progressKey)
        }
        mController.mSubtitleView.bindToMediaPlayer(mVideoView.getMediaPlayer())
        mController.mSubtitleView.setPlaySubtitleCacheKey(subtitleCacheKey)
        val subtitlePathCache = CacheManager.getCache(MD5.string2MD5(subtitleCacheKey)) as? String
        if (!subtitlePathCache.isNullOrEmpty()) {
            mController.mSubtitleView.setSubtitlePath(subtitlePathCache)
        } else {
            if (playSubtitle.isNotEmpty()) {
                mController.mSubtitleView.setSubtitlePath(playSubtitle)
            } else {
                if (mController.mSubtitleView.hasInternal) {
                    mController.mSubtitleView.isInternal = true
                    if (trackInfo != null && trackInfo.subtitle.size > 0) {
                        val subtitleTrackList = trackInfo.subtitle
                        val selectedIndex = trackInfo.getSubtitleSelected(true)
                        var hasCh = false
                        for (subtitleTrackInfoBean in subtitleTrackList) {
                            val lowerLang = subtitleTrackInfoBean.language.toLowerCase()
                            if (lowerLang.startsWith("zh") || lowerLang.startsWith("ch")) {
                                hasCh = true
                                if (selectedIndex != subtitleTrackInfoBean.index) {
                                    (mVideoView.getMediaPlayer() as IjkMediaPlayer).setTrack(subtitleTrackInfoBean.index)
                                    break
                                }
                            }
                        }
                        if (!hasCh) (mVideoView.getMediaPlayer() as IjkMediaPlayer).setTrack(subtitleTrackList[0].index)
                    }
                }
            }
        }
    }

    private fun initViewModel() {
        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)
        sourceViewModel.playResult.observe(this, Observer { info ->
            webPlayUrl = null
            if (info != null) {
                try {
                    progressKey = info.optString("proKey", null)
                    val parse = info.optString("parse", "1") == "1"
                    val jx = info.optString("jx", "0") == "1"
                    playSubtitle = info.optString("subt", "")
                    if (playSubtitle.isEmpty() && info.has("subs")) {
                        try {
                            val obj = info.getJSONArray("subs").optJSONObject(0)
                            var url = obj.optString("url", "")
                            if (!TextUtils.isEmpty(url) && !FileUtils.hasExtension(url)) {
                                val format = obj.optString("format", "")
                                val name = obj.optString("name", "字幕")
                                var ext = ".srt"
                                when (format) {
                                    "text/x-ssa" -> ext = ".ass"
                                    "text/vtt" -> ext = ".vtt"
                                    "application/x-subrip" -> ext = ".srt"
                                    "text/lrc" -> ext = ".lrc"
                                }
                                val filename = name + if (name.toLowerCase().endsWith(ext)) "" else ext
                                url += "#" + mController.encodeUrl(filename)
                            }
                            playSubtitle = url
                        } catch (th: Throwable) {
                        }
                    }
                    subtitleCacheKey = info.optString("subtKey", null)
                    val playUrl = info.optString("playUrl", "")
                    val msg = info.optString("msg", "")
                    if (msg.isNotEmpty()) {
                        Toast.makeText(this@PlayActivity, msg, Toast.LENGTH_SHORT).show()
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
                                if (headers == null) {
                                    headers = HashMap()
                                }
                                headers[key] = hds.getString(key)
                                if (key.equals("user-agent", ignoreCase = true)) {
                                    webUserAgent = hds.getString(key).trim()
                                }
                            }
                            webHeaderMap = headers
                        } catch (th: Throwable) {
                        }
                    }
                    if (parse || jx) {
                        val userJxList = (playUrl.isEmpty() && ApiConfig.get().vipParseFlags.contains(flag)) || jx
                        initParse(flag, userJxList, playUrl, url)
                    } else {
                        mController.showParse(false)
                        playUrl(playUrl + url, headers)
                    }
                } catch (th: Throwable) {
                }
            } else {
                errorWithRetry("获取播放信息错误", true)
            }
        })
    }

    private fun initData() {
        val intent = intent
        if (intent != null && intent.extras != null) {
            val bundle = intent.extras!!
            mVodInfo = App.getInstance().vodInfo!!
            sourceKey = bundle.getString("sourceKey")!!
            sourceBean = ApiConfig.get().getSource(sourceKey)!!
            initPlayerCfg()
            play(false)
        }
    }

    fun initPlayerCfg() {
        try {
            mVodPlayerCfg = JSONObject(mVodInfo.playerCfg)
        } catch (th: Throwable) {
            mVodPlayerCfg = JSONObject()
        }
        try {
            if (!mVodPlayerCfg.has("pl")) {
                mVodPlayerCfg.put("pl", if (sourceBean.getPlayerType() == -1) Hawk.get(HawkConfig.PLAY_TYPE, 1) as Int else sourceBean.getPlayerType())
            }
            if (!mVodPlayerCfg.has("pr")) {
                mVodPlayerCfg.put("pr", Hawk.get(HawkConfig.PLAY_RENDER, 0))
            }
            if (!mVodPlayerCfg.has("ijk")) {
                mVodPlayerCfg.put("ijk", Hawk.get(HawkConfig.IJK_CODEC, "硬解码"))
            }
            if (!mVodPlayerCfg.has("sc")) {
                mVodPlayerCfg.put("sc", Hawk.get(HawkConfig.PLAY_SCALE, 0))
            }
            if (!mVodPlayerCfg.has("sp")) {
                mVodPlayerCfg.put("sp", 1.0f)
            }
            if (!mVodPlayerCfg.has("st")) {
                mVodPlayerCfg.put("st", 0)
            }
            if (!mVodPlayerCfg.has("et")) {
                mVodPlayerCfg.put("et", 0)
            }
        } catch (th: Throwable) {
        }
        mController.setPlayerConfig(mVodPlayerCfg)
    }

    override fun onBackPressed() {
        if (mController.onBackPressed()) {
            return
        }
        super.onBackPressed()
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event != null) {
            if (mController.onKeyEvent(event)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null) {
            if (mController.onKeyDown(keyCode, event)) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null) {
            if (mController.onKeyUp(keyCode, event)) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        if (::mVideoView.isInitialized) {
            mVideoView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mVideoView.isInitialized) {
            mVideoView.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mVideoView.isInitialized) {
            mVideoView.release()
        }
        stopLoadWebView(true)
        stopParse()
        mController.stopOther()
    }

    private lateinit var mVodInfo: VodInfo
    private lateinit var mVodPlayerCfg: JSONObject
    private lateinit var sourceKey: String
    private lateinit var sourceBean: SourceBean

    private fun playNext(isProgress: Boolean) {
        var hasNext = true
        if (!::mVodInfo.isInitialized || mVodInfo.seriesMap[mVodInfo.playFlag] == null) {
            hasNext = false
        } else {
            hasNext = mVodInfo.playIndex + 1 < mVodInfo.seriesMap[mVodInfo.playFlag]!!.size
        }
        if (!hasNext) {
            if (isProgress && ::mVodInfo.isInitialized) {
                mVodInfo.playIndex = 0
                Toast.makeText(this, "已经是最后一集了!,即将跳到第一集继续播放", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "已经是最后一集了!", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            mVodInfo.playIndex++
        }
        play(false)
    }

    private fun playPrevious() {
        var hasPre = true
        if (!::mVodInfo.isInitialized || mVodInfo.seriesMap[mVodInfo.playFlag] == null) {
            hasPre = false
        } else {
            hasPre = mVodInfo.playIndex - 1 >= 0
        }
        if (!hasPre) {
            Toast.makeText(this, "已经是第一集了!", Toast.LENGTH_SHORT).show()
            return
        }
        mVodInfo.playIndex--
        play(false)
    }

    private var autoRetryCount = 0
    private var lastRetryTime = 0L
    private var allowSwitchPlayer = true

    fun autoRetry(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRetryTime > 60_000) {
            LOG.i("echo-reset-autoRetryCount")
            autoRetryCount = 0
            allowSwitchPlayer = false
        }
        lastRetryTime = currentTime
        if (loadFoundVideoUrls != null && loadFoundVideoUrls.isNotEmpty()) {
            autoRetryFromLoadFoundVideoUrls()
            return true
        }

        if (autoRetryCount < 2) {
            if (autoRetryCount == 1) {
                play(false)
                autoRetryCount++
            } else {
                if (webPlayUrl != null) {
                    if (allowSwitchPlayer) {
                        if (mController.switchPlayer()) autoRetryCount++
                    } else {
                        autoRetryCount++
                        allowSwitchPlayer = true
                    }
                    stopParse()
                    initParseLoadFound()
                    if (::mVideoView.isInitialized) mVideoView.release()
                    playUrl(webPlayUrl, webHeaderMap)
                } else {
                    play(false)
                    autoRetryCount++
                }
            }
            return true
        } else {
            autoRetryCount = 0
            return false
        }
    }

    fun autoRetryFromLoadFoundVideoUrls() {
        val videoUrl = loadFoundVideoUrls.poll()
        val header = loadFoundVideoUrlsHeader[videoUrl]
        playUrl(videoUrl, header)
    }

    fun initParseLoadFound() {
        loadFoundCount.set(0)
        loadFoundVideoUrls = LinkedList<String>()
        loadFoundVideoUrlsHeader = HashMap<String, HashMap<String, String>>()
    }

    fun play(reset: Boolean) {
        val vs = mVodInfo.seriesMap[mVodInfo.playFlag]!![mVodInfo.playIndex]
        EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, mVodInfo.playIndex))
        setTip("正在获取播放信息", true, false)
        val playTitleInfo = "${mVodInfo.name} ${vs.name}"
        mController.setTitle(playTitleInfo)

        stopParse()
        initParseLoadFound()
        allowSwitchPlayer = true
        mController.stopOther()
        if (::mVideoView.isInitialized) mVideoView.release()
        subtitleCacheKey = "${mVodInfo.sourceKey}-${mVodInfo.id}-${mVodInfo.playFlag}-${mVodInfo.playIndex}-${vs.name}-subt"
        progressKey = "${mVodInfo.sourceKey}${mVodInfo.id}${mVodInfo.playFlag}${mVodInfo.playIndex}${vs.name}"
        if (reset) {
            CacheManager.delete(MD5.string2MD5(progressKey), 0)
            CacheManager.delete(MD5.string2MD5(subtitleCacheKey), 0)
        } else {
            try {
                val playerType = mVodPlayerCfg.getInt("pl")
                if (playerType == 1) {
                    mController.mSubtitleView.visibility = View.VISIBLE
                } else {
                    mController.mSubtitleView.visibility = View.GONE
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        if (Jianpian.isJpUrl(vs.url)) {
            val jp_url = vs.url
            mController.showParse(false)
            if (vs.url.startsWith("tvbox-xg:")) {
                playUrl(Jianpian.JPUrlDec(jp_url.substring(9)), null)
            } else {
                playUrl(Jianpian.JPUrlDec(jp_url), null)
            }
            return
        }
        if (Thunder.play(vs.url, object : Thunder.ThunderCallback {
            override fun status(code: Int, info: String) {
                if (code < 0) {
                    setTip(info, false, true)
                } else {
                    setTip(info, true, false)
                }
            }

            override fun list(urlMap: Map<Int, String>) {}

            override fun play(url: String) {
                playUrl(url, null)
            }
        })) {
            mController.showParse(false)
            return
        }
        sourceViewModel.getPlay(sourceKey, mVodInfo.playFlag, progressKey, vs.url, subtitleCacheKey)
    }

    private var playSubtitle = ""
    private var subtitleCacheKey = ""
    private var progressKey: String? = null
    private var parseFlag = ""
    private var webUrl = ""
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
            if (playUrl.startsWith("json:")) {
                parseBean = ParseBean()
                parseBean.type = 1
                parseBean.setUrl(playUrl.substring(5))
            } else if (playUrl.startsWith("parse:")) {
                val parseRedirect = playUrl.substring(6)
                for (pb in ApiConfig.get().parseBeanList) {
                    if (pb.name == parseRedirect) {
                        parseBean = pb
                        break
                    }
                }
            }
            if (parseBean == null) {
                parseBean = ParseBean()
                parseBean.type = 0
                parseBean.setUrl(playUrl)
            }
        }
        doParse(parseBean)
    }

    @Throws(JSONException::class)
    fun jsonParse(input: String, json: String): JSONObject? {
        val jsonPlayData = JSONObject(json)
        var url: String
        if (jsonPlayData.has("data")) {
            url = jsonPlayData.getJSONObject("data").getString("url")
        } else {
            url = jsonPlayData.getString("url")
        }
        if (url.startsWith("//")) {
            url = "http:$url"
        }
        if (!url.startsWith("http")) {
            return null
        }
        val headers = JSONObject()
        val ua = jsonPlayData.optString("user-agent", "")
        if (ua.trim().isNotEmpty()) {
            headers.put("User-Agent", " $ua")
        }
        val referer = jsonPlayData.optString("referer", "")
        if (referer.trim().isNotEmpty()) {
            headers.put("Referer", " $referer")
        }
        val taskResult = JSONObject()
        taskResult.put("header", headers)
        taskResult.put("url", url)
        return taskResult
    }

    fun stopParse() {
        mHandler.removeMessages(100)
        stopLoadWebView(false)
        OkGo.getInstance().cancelTag("json_jx")
        if (parseThreadPool != null) {
            try {
                parseThreadPool!!.shutdown()
                parseThreadPool = null
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
    }

    var parseThreadPool: ExecutorService? = null
