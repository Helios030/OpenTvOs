package con.open.tvos.ui.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.IntEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.CountDownTimer
import android.os.Handler
import android.util.Base64
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import com.squareup.picasso.Picasso
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.App
import con.open.tvos.base.BaseActivity
import con.open.tvos.bean.*
import con.open.tvos.crawler.Spider
import con.open.tvos.player.controller.LiveController
import con.open.tvos.ui.adapter.*
import con.open.tvos.ui.dialog.LivePasswordDialog
import con.open.tvos.ui.tv.widget.ViewObj
import con.open.tvos.util.DefaultConfig
import con.open.tvos.util.EpgUtil
import con.open.tvos.util.FastClickCheckUtil
import con.open.tvos.util.HawkConfig
import con.open.tvos.util.LOG
import con.open.tvos.util.PlayerHelper
import con.open.tvos.util.live.TxtSubscribe
import con.open.tvos.util.urlhttp.CallBackUtil
import con.open.tvos.util.urlhttp.UrlHttpUtil
import org.apache.commons.lang3.StringUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import xyz.doikki.videoplayer.player.VideoView
import java.net.URLEncoder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author pj567
 * @date :2021/1/12
 * @description:
 */
class LivePlayActivity : BaseActivity() {

    companion object {
        var context: Context? = null
        private const val postTimeout = 6000
        private const val LONG_PRESS_DELAY = 800L

        // EPG by 龍
        private var channel_Name: LiveChannelItem? = null
        private var hsEpg: Hashtable<String, ArrayList<Epginfo>> = Hashtable()

        // laodao 7day replay
        var formatDate: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        var formatDate1: SimpleDateFormat = SimpleDateFormat("MM-dd")
        var day: String = formatDate.format(Date())
        var nowday: Date = Date()

        var currentChannelGroupIndex = 0

        // kenson
        var shiyi_time: String? = null // 时移时间
        var shiyi_time_c = 0 // 时移时间差值
        var playUrl: String? = null

        // 计算两个时间相差的秒数
        fun getTime(startTime: String, endTime: String): Long {
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val eTime = try { df.parse(endTime)?.time ?: 0 } catch (e: ParseException) { 0 }
            val sTime = try { df.parse(startTime)?.time ?: 0 } catch (e: ParseException) { 0 }
            return (eTime - sTime) / 1000
        }
    }

    private var mVideoView: VideoView<xyz.doikki.videoplayer.player.AbstractPlayer>? = null
    private var tvChannelInfo: TextView? = null
    private var tvTime: TextView? = null
    private var tvNetSpeed: TextView? = null
    private var tvLeftChannelListLayout: LinearLayout? = null
    private var mChannelGroupView: TvRecyclerView? = null
    private var mLiveChannelView: TvRecyclerView? = null
    private var liveChannelGroupAdapter: LiveChannelGroupAdapter? = null
    private var liveChannelItemAdapter: LiveChannelItemAdapter? = null

    private var tvRightSettingLayout: LinearLayout? = null
    private var mSettingGroupView: TvRecyclerView? = null
    private var mSettingItemView: TvRecyclerView? = null
    private var liveSettingGroupAdapter: LiveSettingGroupAdapter? = null
    private var liveSettingItemAdapter: LiveSettingItemAdapter? = null
    private var liveSettingGroupList: MutableList<LiveSettingGroup> = ArrayList()

    private var mHandler = Handler()
    private var mmHandler = Handler()

    private var liveChannelGroupList: MutableList<LiveChannelGroup> = ArrayList()
    private var currentLiveChannelIndex = -1
    private var currentLiveLookBackIndex = -1
    private var currentLiveChangeSourceTimes = 0
    private var currentLiveChannelItem: LiveChannelItem? = null
    private var livePlayerManager = LivePlayerManager()
    private var channelGroupPasswordConfirmed: MutableList<Int> = ArrayList()

    // EPG by 龍
    private var countDownTimer: CountDownTimer? = null
    private var ll_right_top_loading: View? = null
    private var ll_right_top_huikan: View? = null
    private var divLoadEpg: View? = null
    private var divLoadEpgleft: View? = null
    private var divEpg: LinearLayout? = null
    private var ll_epg: RelativeLayout? = null
    private var tv_channelnum: TextView? = null
    private var tip_chname: TextView? = null
    private var tip_epg1: TextView? = null
    private var tip_epg2: TextView? = null
    private var tv_srcinfo: TextView? = null
    private var tv_curepg_left: TextView? = null
    private var tv_nextepg_left: TextView? = null
    private var myAdapter: MyEpgAdapter? = null
    private var tv_right_top_channel_name: TextView? = null
    private var tv_right_top_epg_name: TextView? = null
    private var iv_circle_bg: ImageView? = null
    private var tv_shownum: TextView? = null
    private var txtNoEpg: TextView? = null
    private var iv_back_bg: ImageView? = null

    private var objectAnimator: ObjectAnimator? = null
    var epgStringAddress = ""

    private var mEpgDateGridView: TvRecyclerView? = null
    private var mRightEpgList: TvRecyclerView? = null
    private var liveEpgDateAdapter: LiveEpgDateAdapter? = null
    private var epgListAdapter: LiveEpgAdapter? = null

    private var liveDayList: MutableList<LiveDayListGroup> = ArrayList()

    private var isSHIYI = false
    private var isBack = false

    private var imgLiveIcon: ImageView? = null
    private var liveIconNullBg: FrameLayout? = null
    private var liveIconNullText: TextView? = null
    private var timeFormat = SimpleDateFormat("yyyy-MM-dd")
    private var backcontroller: View? = null
    private var countDownTimer3: CountDownTimer? = null
    private val videoWidth = 1920
    private val videoHeight = 1080
    private var tv_currentpos: TextView? = null
    private var tv_duration: TextView? = null
    private var sBar: SeekBar? = null
    private var iv_playpause: View? = null
    private var iv_play: View? = null
    private var show = false

    // 遥控器数字键输入的要切换的频道号码
    private var selectedChannelNumber = 0
    private var tvSelectedChannel: TextView? = null

    private var mLastChannelGroupIndex = -1
    private var mLastChannelList: MutableList<LiveChannelItem> = ArrayList()

    private var mLongPressRunnable: Runnable? = null

    private var catchup: JsonObject? = null
    private var hasCatchup = false
    private var logoUrl: String? = null

    // 获取EPG并存储
    private var epgdata: MutableList<Epginfo> = ArrayList()

    override fun getLayoutResID(): Int {
        return R.layout.activity_live_play
    }

    override fun init() {
        context = this
        epgStringAddress = Hawk.get(HawkConfig.EPG_URL, "")
        if (epgStringAddress.isEmpty() || epgStringAddress.length < 5)
            epgStringAddress = "http://epg.51zmt.top:8000/api/diyp/"

        setLoadSir(findViewById(R.id.live_root))
        mVideoView = findViewById(R.id.mVideoView)

        tvLeftChannelListLayout = findViewById(R.id.tvLeftChannnelListLayout)
        mChannelGroupView = findViewById(R.id.mGroupGridView)
        mLiveChannelView = findViewById(R.id.mChannelGridView)
        tvRightSettingLayout = findViewById(R.id.tvRightSettingLayout)
        mSettingGroupView = findViewById(R.id.mSettingGroupView)
        mSettingItemView = findViewById(R.id.mSettingItemView)
        tvChannelInfo = findViewById(R.id.tvChannel)
        tvTime = findViewById(R.id.tvTime)
        tvNetSpeed = findViewById(R.id.tvNetSpeed)

        // EPG findViewById by 龍
        tip_chname = findViewById(R.id.tv_channel_bar_name)
        tv_channelnum = findViewById(R.id.tv_channel_bottom_number)
        tip_epg1 = findViewById(R.id.tv_current_program_time)
        tip_epg2 = findViewById(R.id.tv_next_program_time)
        tv_srcinfo = findViewById(R.id.tv_source)
        tv_curepg_left = findViewById(R.id.tv_current_program)
        tv_nextepg_left = findViewById(R.id.tv_next_program)
        ll_epg = findViewById(R.id.ll_epg)
        tv_right_top_channel_name = findViewById(R.id.tv_right_top_channel_name)
        tv_right_top_epg_name = findViewById(R.id.tv_right_top_epg_name)
        iv_circle_bg = findViewById(R.id.iv_circle_bg)
        iv_back_bg = findViewById(R.id.iv_back_bg)
        tv_shownum = findViewById(R.id.tv_shownum)
        txtNoEpg = findViewById(R.id.txtNoEpg)
        ll_right_top_loading = findViewById(R.id.ll_right_top_loading)
        ll_right_top_huikan = findViewById(R.id.ll_right_top_huikan)
        divLoadEpg = findViewById(R.id.divLoadEpg)
        divLoadEpgleft = findViewById(R.id.divLoadEpgleft)
        divEpg = findViewById(R.id.divEPG)

        // 右上角图片旋转
        objectAnimator = ObjectAnimator.ofFloat(iv_circle_bg, "rotation", 360.0f)
        objectAnimator?.duration = postTimeout.toLong()
        objectAnimator?.repeatCount = -1
        objectAnimator?.start()

        // laodao 7day replay
        mEpgDateGridView = findViewById(R.id.mEpgDateGridView)
        Hawk.put(HawkConfig.NOW_DATE, formatDate.format(Date()))
        day = formatDate.format(Date())
        nowday = Date()

        mRightEpgList = findViewById(R.id.lv_epg)
        imgLiveIcon = findViewById(R.id.img_live_icon)
        liveIconNullBg = findViewById(R.id.live_icon_null_bg)
        liveIconNullText = findViewById(R.id.live_icon_null_text)
        imgLiveIcon?.visibility = View.INVISIBLE
        liveIconNullText?.visibility = View.INVISIBLE
        liveIconNullBg?.visibility = View.INVISIBLE

        sBar = findViewById(R.id.pb_progressbar)
        tv_currentpos = findViewById(R.id.tv_currentpos)
        backcontroller = findViewById(R.id.backcontroller)
        tv_duration = findViewById(R.id.tv_duration)
        iv_playpause = findViewById(R.id.iv_playpause)
        iv_play = findViewById(R.id.iv_play)

        tvSelectedChannel = findViewById(R.id.tv_selected_channel)

        if (show) {
            backcontroller?.visibility = View.VISIBLE
            ll_epg?.visibility = View.GONE
        } else {
            backcontroller?.visibility = View.GONE
            ll_epg?.visibility = View.VISIBLE
        }

        iv_play?.setOnClickListener {
            mVideoView?.start()
            iv_play?.visibility = View.INVISIBLE
            countDownTimer?.start()
            iv_playpause?.background = ContextCompat.getDrawable(this@LivePlayActivity.context!!, R.drawable.vod_pause)
        }

        iv_playpause?.setOnClickListener {
            if (mVideoView!!.isPlaying()) {
                mVideoView?.pause()
                countDownTimer?.cancel()
                iv_play?.visibility = View.VISIBLE
                iv_playpause?.background = ContextCompat.getDrawable(this@LivePlayActivity.context!!, R.drawable.icon_play)
            } else {
                mVideoView?.start()
                iv_play?.visibility = View.INVISIBLE
                countDownTimer?.start()
                iv_playpause?.background = ContextCompat.getDrawable(this@LivePlayActivity.context!!, R.drawable.vod_pause)
            }
        }

        sBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                countDownTimer?.let {
                    mVideoView?.seekTo(progress.toLong())
                    countDownTimer?.cancel()
                    countDownTimer?.start()
                }
            }
        })

        sBar?.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (mVideoView!!.isPlaying()) {
                        mVideoView?.pause()
                        countDownTimer?.cancel()
                        iv_play?.visibility = View.VISIBLE
                        iv_playpause?.background = ContextCompat.getDrawable(this@LivePlayActivity.context!!, R.drawable.icon_play)
                    } else {
                        mVideoView?.start()
                        iv_play?.visibility = View.INVISIBLE
                        countDownTimer?.start()
                        iv_playpause?.background = ContextCompat.getDrawable(this@LivePlayActivity.context!!, R.drawable.vod_pause)
                    }
                }
            }
            false
        }

        initEpgDateView()
        initEpgListView()
        initDayList()
        initVideoView()
        initChannelGroupView()
        initLiveChannelView()
        initSettingGroupView()
        initSettingItemView()
        initLiveChannelList()
        initLiveSettingGroupList()
        Hawk.put(HawkConfig.PLAYER_IS_LIVE, true)
    }
