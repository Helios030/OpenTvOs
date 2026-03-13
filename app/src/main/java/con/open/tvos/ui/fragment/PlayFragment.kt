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
