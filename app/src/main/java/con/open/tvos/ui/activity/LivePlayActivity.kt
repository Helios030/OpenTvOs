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

    private var epgdata: MutableList<Epginfo> = ArrayList()

    private fun showEpg(date: Date, arrayList: ArrayList<Epginfo>?) {
        if (arrayList != null && arrayList.isNotEmpty()) {
            epgdata = arrayList
            epgListAdapter?.CanBack(currentLiveChannelItem?.getinclude_back() ?: false)
            epgListAdapter?.setNewData(epgdata)

            var i = -1
            var size = epgdata.size - 1
            while (size >= 0) {
                if (Date().compareTo(epgdata[size].startdateTime) >= 0) {
                    break
                }
                size--
            }
            i = size
            if (i >= 0 && Date().compareTo(epgdata[i].enddateTime) <= 0) {
                mRightEpgList?.setSelectedPosition(i)
                mRightEpgList?.setSelection(i)
                epgListAdapter?.setSelectedEpgIndex(i)
                val finalI = i
                mRightEpgList?.post {
                    mRightEpgList?.smoothScrollToPosition(finalI)
                }
            }
        }
    }

    private fun getFirstPartBeforeSpace(str: String?): String {
        if (str.isNullOrEmpty()) return str ?: ""
        val spaceIndex = str.indexOf(' ')
        return if (spaceIndex == -1) str else str.substring(0, spaceIndex)
    }

    @SuppressLint("SimpleDateFormat")
    fun getEpg(date: Date) {
        val channelName = channel_Name?.getChannelName() ?: return
        val channelNameReal = getFirstPartBeforeSpace(channelName)
        val timeFormat = SimpleDateFormat("yyyy-MM-dd")
        timeFormat.timeZone = TimeZone.getTimeZone("GMT+8:00")
        var epgTagName = channelNameReal
        if (logoUrl.isNullOrEmpty()) {
            val epgInfo = EpgUtil.getEpgInfo(channelNameReal)
            if (epgInfo != null && epgInfo[1].isNotEmpty()) {
                epgTagName = epgInfo[1]
            }
            updateChannelIcon(channelName, epgInfo?.get(0))
        } else if (logoUrl == "false") {
            updateChannelIcon(channelName, null)
        } else {
            val logo = logoUrl!!.replace("{name}", epgTagName)
            updateChannelIcon(channelName, logo)
        }
        epgListAdapter?.CanBack(currentLiveChannelItem?.getinclude_back() ?: false)
        val url = if (epgStringAddress.contains("{name}") && epgStringAddress.contains("{date}")) {
            epgStringAddress.replace("{name}", URLEncoder.encode(epgTagName)).replace("{date}", timeFormat.format(date))
        } else {
            "$epgStringAddress?ch=${URLEncoder.encode(epgTagName)}&date=${timeFormat.format(date)}"
        }

        val savedEpgKey = channelName + "_" + liveEpgDateAdapter?.getItem(liveEpgDateAdapter!!.getSelectedIndex())?.getDatePresented()
        if (hsEpg.containsKey(savedEpgKey)) {
            showEpg(date, hsEpg[savedEpgKey])
            showBottomEpg()
            return
        }

        UrlHttpUtil.get(url, object : CallBackUtil.CallBackString() {
            override fun onFailure(i: Int, str: String?) {}

            override fun onResponse(paramString: String?) {
                LOG.i("echo-epgTagName:$channelNameReal")
                val arrayList = ArrayList<Epginfo>()
                try {
                    if (paramString != null && paramString.contains("epg_data")) {
                        val jSONArray = JSONObject(paramString).optJSONArray("epg_data")
                        if (jSONArray != null) {
                            for (b in 0 until jSONArray.length()) {
                                val jSONObject = jSONArray.getJSONObject(b)
                                val epgbcinfo = Epginfo(date, jSONObject.optString("title"), date, jSONObject.optString("start"), jSONObject.optString("end"), b)
                                arrayList.add(epgbcinfo)
                            }
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                hsEpg[savedEpgKey] = arrayList
                showEpg(date, arrayList)
                showBottomEpg()
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun showBottomEpg() {
        if (isSHIYI) return

        if (channel_Name?.getChannelName() != null) {
            tip_chname?.text = channel_Name!!.getChannelName()
            tv_channelnum?.text = "" + channel_Name!!.getChannelNum()
            val tv_current_program_name = findViewById<TextView>(R.id.tv_current_program_name)
            val tv_next_program_name = findViewById<TextView>(R.id.tv_next_program_name)
            tip_epg1?.text = "暂无信息"
            tv_current_program_name?.text = ""
            tip_epg2?.text = "开源测试软件"
            tv_next_program_name?.text = ""

            val savedEpgKey = channel_Name!!.getChannelName() + "_" + liveEpgDateAdapter?.getItem(liveEpgDateAdapter!!.getSelectedIndex())?.getDatePresented()

            if (hsEpg.containsKey(savedEpgKey)) {
                val arrayList = hsEpg[savedEpgKey]
                if (arrayList != null && arrayList.isNotEmpty()) {
                    val date = Date()
                    var size = arrayList.size - 1
                    var hasInfo = false
                    while (size >= 0) {
                        if (date.after(arrayList[size].startdateTime) && date.before(arrayList[size].enddateTime)) {
                            tip_epg1?.text = "${arrayList[size].start}-${arrayList[size].end}"
                            tv_current_program_name?.text = arrayList[size].title
                            if (size != arrayList.size - 1) {
                                tip_epg2?.text = "${arrayList[size + 1].start}-${arrayList[size + 1].end}"
                                tv_next_program_name?.text = arrayList[size + 1].title
                            } else {
                                tip_epg2?.text = "${arrayList[size].end}-23:59"
                                tv_next_program_name?.text = "精彩节目-暂无节目预告信息"
                            }
                            hasInfo = true
                            break
                        } else {
                            size--
                        }
                    }
                    if (!hasInfo) {
                        tip_epg1?.text = "00:00-${arrayList[0].start}"
                        tv_current_program_name?.text = "精彩节目-暂无节目预告信息"
                        tip_epg2?.text = "${arrayList[0].start}-${arrayList[0].end}"
                        tv_next_program_name?.text = arrayList[0].title
                    }
                }
                epgListAdapter?.CanBack(currentLiveChannelItem?.getinclude_back() ?: false)
                epgListAdapter?.setNewData(arrayList)
            } else {
                val selectedIndex = liveEpgDateAdapter?.getSelectedIndex() ?: -1
                if (selectedIndex < 0) getEpg(Date())
            }

            countDownTimer?.cancel()
            if (tip_epg1?.text?.toString() != "暂无信息") {
                ll_right_top_loading?.visibility = View.VISIBLE
                ll_epg?.visibility = View.VISIBLE
                countDownTimer = object : CountDownTimer(postTimeout.toLong(), 1000) {
                    override fun onTick(j: Long) {}
                    override fun onFinish() {
                        ll_right_top_loading?.visibility = View.GONE
                        ll_right_top_huikan?.visibility = View.GONE
                        ll_epg?.visibility = View.GONE
                    }
                }
                countDownTimer?.start()
            } else {
                ll_right_top_loading?.visibility = View.GONE
                ll_right_top_huikan?.visibility = View.GONE
                ll_epg?.visibility = View.GONE
            }

            if (channel_Name == null || channel_Name!!.getSourceNum() <= 0) {
                findViewById<TextView>(R.id.tv_source).text = "1/1"
            } else {
                findViewById<TextView>(R.id.tv_source).text = "[线路${channel_Name!!.getSourceIndex() + 1}/${channel_Name!!.getSourceNum()}]"
            }
            tv_right_top_channel_name?.text = channel_Name!!.getChannelName()
            tv_right_top_epg_name?.text = channel_Name!!.getChannelName()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateChannelIcon(channelName: String, logoUrl: String?) {
        if (StringUtils.isEmpty(logoUrl)) {
            liveIconNullBg?.visibility = View.VISIBLE
            liveIconNullText?.visibility = View.VISIBLE
            imgLiveIcon?.visibility = View.INVISIBLE
            liveIconNullText?.text = "" + channel_Name!!.getChannelNum()
        } else {
            imgLiveIcon?.visibility = View.VISIBLE
            Picasso.get().load(logoUrl).into(imgLiveIcon)
            liveIconNullBg?.visibility = View.INVISIBLE
            liveIconNullText?.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun divLoadEpgRight(view: View) {
        mHandler.removeCallbacks(mHideChannelListRun)
        mHandler.postDelayed(mHideChannelListRun, postTimeout.toLong())
        mChannelGroupView?.visibility = View.GONE
        divEpg?.visibility = View.VISIBLE
        divLoadEpgleft?.visibility = View.VISIBLE
        divLoadEpg?.visibility = View.GONE
        mRightEpgList?.setSelectedPosition(epgListAdapter?.getSelectedIndex() ?: 0)
        epgListAdapter?.notifyDataSetChanged()
    }

    fun divLoadEpgLeft(view: View) {
        mHandler.removeCallbacks(mHideChannelListRun)
        mHandler.postDelayed(mHideChannelListRun, postTimeout.toLong())
        mChannelGroupView?.visibility = View.VISIBLE
        divEpg?.visibility = View.GONE
        divLoadEpgleft?.visibility = View.GONE
        divLoadEpg?.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        when {
            tvLeftChannelListLayout?.visibility == View.VISIBLE -> {
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.post(mHideChannelListRun)
            }
            tvRightSettingLayout?.visibility == View.VISIBLE -> {
                mHandler.removeCallbacks(mHideSettingLayoutRun)
                mHandler.post(mHideSettingLayoutRun)
            }
            backcontroller?.visibility == View.VISIBLE -> {
                backcontroller?.visibility = View.GONE
            }
            isBack -> {
                isBack = false
                playPreSource()
            }
            else -> {
                mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun)
                mHandler.removeCallbacks(mUpdateNetSpeedRun)
                super.onBackPressed()
            }
        }
    }

    private val mPlaySelectedChannel = Runnable {
        var currentTotal = 0
        var groupIndex = 0
        var channelIndex = -1
        for (group in liveChannelGroupList) {
            val groupChannelCount = group.getLiveChannels().size
            if (currentTotal + groupChannelCount >= selectedChannelNumber) {
                channelIndex = selectedChannelNumber - currentTotal - 1
                break
            }
            currentTotal += groupChannelCount
            groupIndex++
        }
        tvSelectedChannel?.visibility = View.INVISIBLE
        tvSelectedChannel?.text = ""
        if (channelIndex >= 0) {
            loadChannelGroupDataAndPlay(groupIndex, channelIndex)
        } else {
            playChannel(currentChannelGroupIndex, currentLiveChannelIndex, false)
        }
        selectedChannelNumber = 0
    }

    @SuppressLint("SetTextI18n")
    private fun numericKeyDown(digit: Int) {
        selectedChannelNumber = selectedChannelNumber * 10 + digit
        tvSelectedChannel?.text = selectedChannelNumber.toString()
        ll_right_top_loading?.visibility = View.GONE
        ll_right_top_huikan?.visibility = View.GONE
        tvSelectedChannel?.visibility = View.VISIBLE

        mHandler.removeCallbacks(mPlaySelectedChannel)
        mHandler.postDelayed(mPlaySelectedChannel, 2500)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_INFO || keyCode == KeyEvent.KEYCODE_HELP) {
                showSettingGroup()
            } else if (!isListOrSettingLayoutVisible()) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)) playNext() else playPrevious()
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)) playPrevious() else playNext()
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (isBack) showProgressBars(true) else playPreSource()
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (isBack) showProgressBars(true) else playNextSource()
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {}
                    else -> {
                        val digit = when {
                            keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> keyCode - KeyEvent.KEYCODE_0
                            keyCode in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> keyCode - KeyEvent.KEYCODE_NUMPAD_0
                            else -> return super.dispatchKeyEvent(event)
                        }
                        numericKeyDown(digit)
                    }
                }
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (!isListOrSettingLayoutVisible()) {
                if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) && event.repeatCount == 0) {
                    showChannelList()
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) && event.repeatCount == 0) {
            mLongPressRunnable = Runnable {
                showSettingGroup()
            }
            mmHandler.postDelayed(mLongPressRunnable!!, LONG_PRESS_DELAY)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            mLongPressRunnable?.let {
                mmHandler.removeCallbacks(it)
                mLongPressRunnable = null
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        mVideoView?.resume()
    }

    override fun onPause() {
        super.onPause()
        mVideoView?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mVideoView?.let {
            it.release()
            mVideoView = null
        }
    }

    private fun showChannelList() {
        if (liveChannelGroupList.isEmpty()) return
        if (tvRightSettingLayout?.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideSettingLayoutRun)
            mHandler.post(mHideSettingLayoutRun)
            return
        }
        if (tvLeftChannelListLayout?.visibility == View.INVISIBLE) {
            if (currentLiveLookBackIndex > -1) {
                mRightEpgList?.setSelectedPosition(currentLiveLookBackIndex)
                mRightEpgList?.post {
                    mRightEpgList?.smoothScrollToPosition(currentLiveLookBackIndex)
                }
            }
            refreshChannelList(currentChannelGroupIndex)
            mHandler.postDelayed(mFocusCurrentChannelAndShowChannelList, 50)
        } else {
            mHandler.removeCallbacks(mHideChannelListRun)
            mHandler.post(mHideChannelListRun)
        }
    }

    private fun refreshChannelList(currentChannelGroupIndex: Int) {
        val newChannels = getLiveChannels(currentChannelGroupIndex)
        if (currentChannelGroupIndex == mLastChannelGroupIndex && isSameData(newChannels, mLastChannelList)) {
            return
        }
        if (currentLiveChannelIndex > -1) {
            mLiveChannelView?.scrollToPosition(currentLiveChannelIndex)
            mLiveChannelView?.setSelection(currentLiveChannelIndex)
        }
        mChannelGroupView?.scrollToPosition(currentChannelGroupIndex)
        mChannelGroupView?.setSelection(currentChannelGroupIndex)
        mLastChannelGroupIndex = currentChannelGroupIndex
        mLastChannelList = ArrayList(newChannels)
        liveChannelItemAdapter?.setNewData(newChannels)
    }

    private fun isSameData(list1: List<LiveChannelItem>, list2: List<LiveChannelItem>): Boolean {
        if (list1 === list2) return true
        if (list1.size != list2.size) return false
        for (i in list1.indices) {
            if (list1[i] != list2[i]) return false
        }
        return true
    }

    private val mFocusCurrentChannelAndShowChannelList = Runnable {
        if (mChannelGroupView!!.isScrolling || mLiveChannelView!!.isScrolling || mChannelGroupView!!.isComputingLayout || mLiveChannelView!!.isComputingLayout) {
            mHandler.postDelayed(this, 100)
        } else {
            liveChannelGroupAdapter?.setSelectedGroupIndex(currentChannelGroupIndex)
            liveChannelItemAdapter?.setSelectedChannelIndex(currentLiveChannelIndex)
            val holder = mLiveChannelView?.findViewHolderForAdapterPosition(currentLiveChannelIndex)
            holder?.itemView?.requestFocus()
            tvLeftChannelListLayout?.visibility = View.VISIBLE
            val viewObj = ViewObj(tvLeftChannelListLayout!!, tvLeftChannelListLayout!!.layoutParams as ViewGroup.MarginLayoutParams)
            val animator = ObjectAnimator.ofObject(viewObj, "marginLeft", IntEvaluator(), -tvLeftChannelListLayout!!.layoutParams.width, 0)
            animator.duration = 200
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mHandler.removeCallbacks(mHideChannelListRun)
                    mHandler.postDelayed(mHideChannelListRun, postTimeout.toLong())
                }
            })
            animator.start()
        }
    }

    private val mHideChannelListRun = Runnable {
        val params = tvLeftChannelListLayout?.layoutParams as? ViewGroup.MarginLayoutParams ?: return@Runnable
        if (tvLeftChannelListLayout?.visibility == View.VISIBLE) {
            val viewObj = ViewObj(tvLeftChannelListLayout!!, params)
            val animator = ObjectAnimator.ofObject(viewObj, "marginLeft", IntEvaluator(), 0, -tvLeftChannelListLayout!!.layoutParams.width)
            animator.duration = 200
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    tvLeftChannelListLayout?.visibility = View.INVISIBLE
                }
            })
            animator.start()
        }
    }

    private fun showChannelInfo() {
        tvChannelInfo?.text = String.format(Locale.getDefault(), "%d %s %s(%d/%d)",
            currentLiveChannelItem!!.getChannelNum(),
            currentLiveChannelItem!!.getChannelName(),
            currentLiveChannelItem!!.getSourceName(),
            currentLiveChannelItem!!.getSourceIndex() + 1,
            currentLiveChannelItem!!.getSourceNum())

        val lParams = FrameLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        if (tvRightSettingLayout?.visibility == View.VISIBLE) {
            lParams.gravity = Gravity.LEFT
            lParams.leftMargin = 60
            lParams.topMargin = 30
        } else {
            lParams.gravity = Gravity.RIGHT
            lParams.rightMargin = 60
            lParams.topMargin = 30
        }
        tvChannelInfo?.layoutParams = lParams

        tvChannelInfo?.visibility = View.VISIBLE
        mHandler.removeCallbacks(mHideChannelInfoRun)
        mHandler.postDelayed(mHideChannelInfoRun, 3000)
    }

    private val mHideChannelInfoRun = Runnable {
        tvChannelInfo?.visibility = View.INVISIBLE
    }

    private fun initLiveObj() {
        val position = Hawk.get(HawkConfig.LIVE_GROUP_INDEX, 0)
        val liveGroups: JsonArray = Hawk.get(HawkConfig.LIVE_GROUP_LIST, JsonArray())
        val livesOBJ = liveGroups[position].asJsonObject
        val type = if (livesOBJ.has("type")) livesOBJ.get("type").asString else "0"

        if (livesOBJ.has("catchup")) {
            catchup = livesOBJ.getAsJsonObject("catchup")
            LOG.i("echo-catchup :" + catchup.toString())
            hasCatchup = true
        }
        if (livesOBJ.has("logo")) {
            logoUrl = livesOBJ.get("logo").asString
        }
        if (type == "3") {
            var pyJar = ""
            if (livesOBJ.has("jar")) {
                pyJar = if (livesOBJ.has("jar")) livesOBJ.get("jar").asString else ""
            } else if (livesOBJ.has("api")) {
                pyJar = if (livesOBJ.has("api")) livesOBJ.get("api").asString else ""
                var ext = ""
                if (livesOBJ.has("ext") && (livesOBJ.get("ext").isJsonObject || livesOBJ.get("ext").isJsonArray)) {
                    ext = livesOBJ.get("ext").toString()
                } else {
                    ext = DefaultConfig.safeJsonString(livesOBJ, "ext", "")
                }
                LOG.i("echo-ext:$ext")
                if (ext.isNotEmpty()) pyJar = "$pyJar?extend=$ext"
            }
            ApiConfig.get().setLiveJar(pyJar)
        }
    }

    private fun liveWebHeader(): HashMap<String, String>? {
        return Hawk.get(HawkConfig.LIVE_WEB_HEADER)
    }

    private fun playChannel(channelGroupIndex: Int, liveChannelIndex: Int, changeSource: Boolean): Boolean {
        if ((channelGroupIndex == currentChannelGroupIndex && liveChannelIndex == currentLiveChannelIndex && !changeSource)
            || (changeSource && currentLiveChannelItem?.getSourceNum() == 1)) {
            return true
        }
        mVideoView?.release()
        if (!changeSource) {
            currentChannelGroupIndex = channelGroupIndex
            currentLiveChannelIndex = liveChannelIndex
            currentLiveChannelItem = getLiveChannels(currentChannelGroupIndex)[currentLiveChannelIndex]
            Hawk.put(HawkConfig.LIVE_CHANNEL, currentLiveChannelItem!!.getChannelName())
            livePlayerManager.getLiveChannelPlayer(mVideoView, currentLiveChannelItem!!.getChannelName())
        }

        channel_Name = currentLiveChannelItem
        currentLiveLookBackIndex = -1
        epgListAdapter?.setSelectedEpgIndex(-1)
        isSHIYI = false
        isBack = false
        if (hasCatchup || currentLiveChannelItem?.getUrl()?.contains("PLTV/") == true || currentLiveChannelItem?.getUrl()?.contains("TVOD/") == true) {
            currentLiveChannelItem?.setinclude_back(true)
        } else {
            currentLiveChannelItem?.setinclude_back(false)
        }
        showBottomEpg()
        getEpg(Date())
        backcontroller?.visibility = View.GONE
        ll_right_top_huikan?.visibility = View.GONE
        mVideoView?.let {
            liveWebHeader()?.let { header -> LOG.i("echo-${header.toString()}") }
            it.setUrl(currentLiveChannelItem!!.getUrl(), liveWebHeader())
            it.start()
        }
        return true
    }

    private fun playNext() {
        if (!isCurrentLiveChannelValid()) return
        val groupChannelIndex = getNextChannel(1)
        playChannel(groupChannelIndex[0], groupChannelIndex[1], false)
    }

    private fun playPrevious() {
        if (!isCurrentLiveChannelValid()) return
        val groupChannelIndex = getNextChannel(-1)
        playChannel(groupChannelIndex[0], groupChannelIndex[1], false)
    }

    fun playPreSource() {
        if (!isCurrentLiveChannelValid()) return
        currentLiveChannelItem?.preSource()
        playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true)
    }

    fun playNextSource() {
        if (!isCurrentLiveChannelValid()) return
        currentLiveChannelItem?.nextSource()
        playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true)
    }

    private fun showSettingGroup() {
        if (tvLeftChannelListLayout?.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun)
            mHandler.post(mHideChannelListRun)
        }
        if (tvRightSettingLayout?.visibility == View.INVISIBLE) {
            if (!isCurrentLiveChannelValid()) return
            loadCurrentSourceList()
            liveSettingGroupAdapter?.setNewData(liveSettingGroupList)
            selectSettingGroup(0, false)
            mSettingGroupView?.scrollToPosition(0)
            mSettingItemView?.scrollToPosition(currentLiveChannelItem!!.getSourceIndex())
            mHandler.postDelayed(mFocusAndShowSettingGroup, 50)
        } else {
            mHandler.removeCallbacks(mHideSettingLayoutRun)
            mHandler.post(mHideSettingLayoutRun)
        }
    }

    private val mFocusAndShowSettingGroup = Runnable {
        if (mSettingGroupView!!.isScrolling || mSettingItemView!!.isScrolling || mSettingGroupView!!.isComputingLayout || mSettingItemView!!.isComputingLayout) {
            mHandler.postDelayed(this, 100)
        } else {
            val holder = mSettingGroupView?.findViewHolderForAdapterPosition(0)
            holder?.itemView?.requestFocus()
            tvRightSettingLayout?.visibility = View.VISIBLE
            val params = tvRightSettingLayout?.layoutParams as? ViewGroup.MarginLayoutParams
            if (tvRightSettingLayout?.visibility == View.VISIBLE && params != null) {
                val viewObj = ViewObj(tvRightSettingLayout!!, params)
                val animator = ObjectAnimator.ofObject(viewObj, "marginRight", IntEvaluator(), -tvRightSettingLayout!!.layoutParams.width, 0)
                animator.duration = 200
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        mHandler.postDelayed(mHideSettingLayoutRun, postTimeout.toLong())
                    }
                })
                animator.start()
            }
        }
    }

    private val mHideSettingLayoutRun = Runnable {
        val params = tvRightSettingLayout?.layoutParams as? ViewGroup.MarginLayoutParams ?: return@Runnable
        if (tvRightSettingLayout?.visibility == View.VISIBLE) {
            val viewObj = ViewObj(tvRightSettingLayout!!, params)
            val animator = ObjectAnimator.ofObject(viewObj, "marginRight", IntEvaluator(), 0, -tvRightSettingLayout!!.layoutParams.width)
            animator.duration = 200
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    tvRightSettingLayout?.visibility = View.INVISIBLE
                    liveSettingGroupAdapter?.setSelectedGroupIndex(-1)
                }
            })
            animator.start()
        }
    }

    private fun initEpgListView() {
        mRightEpgList?.setHasFixedSize(true)
        mRightEpgList?.setLayoutManager(V7LinearLayoutManager(this, 1, false))
        epgListAdapter = LiveEpgAdapter()
        mRightEpgList?.setAdapter(epgListAdapter)

        mRightEpgList?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, postTimeout.toLong())
            }
        })

        mRightEpgList?.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                epgListAdapter?.setFocusedEpgIndex(-1)
            }

            override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, postTimeout.toLong())
                epgListAdapter?.setFocusedEpgIndex(position)
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
                if (position == currentLiveLookBackIndex) return
                currentLiveLookBackIndex = position
                val date = if (liveEpgDateAdapter?.getSelectedIndex() ?: -1 < 0) Date() else liveEpgDateAdapter!!.getData()[liveEpgDateAdapter!!.getSelectedIndex()].getDateParamVal()
                val dateFormat = SimpleDateFormat("yyyyMMdd")
                dateFormat.timeZone = TimeZone.getTimeZone("GMT+8:00")
                val selectedData = epgListAdapter?.getItem(position) ?: return
                val targetDate = dateFormat.format(date)
                val shiyiStartdate = targetDate + selectedData.originStart.replace(":", "") + "30"
                val shiyiEnddate = targetDate + selectedData.originEnd.replace(":", "") + "30"
                val now = Date()
                if (Date().compareTo(selectedData.startdateTime) < 0) return
                epgListAdapter?.setSelectedEpgIndex(position)
                if (now.compareTo(selectedData.startdateTime) >= 0 && now.compareTo(selectedData.enddateTime) <= 0) {
                    mVideoView?.release()
                    isSHIYI = false
                    mVideoView?.setUrl(currentLiveChannelItem!!.getUrl(), liveWebHeader())
                    mVideoView?.start()
                    epgListAdapter?.setShiyiSelection(-1, false, timeFormat.format(date))
                    epgListAdapter?.notifyDataSetChanged()
                    showProgressBars(false)
                    return
                }
                var shiyiUrl = currentLiveChannelItem!!.getUrl()
                if (now.compareTo(selectedData.startdateTime) < 0) {
                } else if (hasCatchup || shiyiUrl.contains("PLTV/") || shiyiUrl.contains("TVOD/")) {
                    shiyiUrl = shiyiUrl.replace("/PLTV/", "/TVOD/")
                    mHandler.removeCallbacks(mHideChannelListRun)
                    mHandler.postDelayed(mHideChannelListRun, 100)
                    mVideoView?.release()
                    shiyi_time = "$shiyiStartdate-$shiyiEnddate"
                    isSHIYI = true
                    if (hasCatchup) {
                        val replace = catchup!!.get("replace").asString
                        val source = catchup!!.get("source").asString
                        val parts = replace.split(",")
                        val left = if (parts.isNotEmpty()) parts[0].trim() else ""
                        val right = if (parts.size > 1) parts[1].trim() else ""
                        shiyiUrl = shiyiUrl.replace(left.toRegex(), right)
                        val startHHmm = selectedData.originStart.replace(":", "")
                        val endHHmm = selectedData.originEnd.replace(":", "")
                        val pattern = Pattern.compile("\\\$\\\$\{\\\((b|e)\\\)(.*?)\\\}")
                        val matcher = pattern.matcher(source)
                        val valueMap = hashMapOf<String, String>()
                        valueMap["b"] = targetDate + "T" + startHHmm
                        valueMap["e"] = targetDate + "T" + endHHmm
                        val result = StringBuffer()
                        while (matcher.find()) {
                            val type = matcher.group(1)
                            val replacement = valueMap[type]
                            if (replacement != null) {
                                matcher.appendReplacement(result, replacement)
                            }
                        }
                        matcher.appendTail(result)
                        LOG.i("echo-shiyiurl:$shiyiUrl")
                        if (shiyiUrl.endsWith("&")) shiyiUrl = shiyiUrl.substring(0, shiyiUrl.length - 1)
                        shiyiUrl += result.toString()
                    } else {
                        if (shiyiUrl.indexOf("?") <= 0) {
                            shiyiUrl += "?playseek=$shiyi_time"
                        } else if (shiyiUrl.indexOf("playseek") > 0) {
                            shiyiUrl = shiyiUrl.replace("playseek=(.*)".toRegex(), "playseek=$shiyi_time")
                        } else {
                            shiyiUrl += "&playseek=$shiyi_time"
                        }
                    }
                    LOG.i("echo-回看地址playUrl : $shiyiUrl")
                    playUrl = shiyiUrl

                    mVideoView?.setUrl(playUrl, liveWebHeader())
                    mVideoView?.start()
                    epgListAdapter?.setShiyiSelection(position, true, timeFormat.format(date))
                    epgListAdapter?.notifyDataSetChanged()
                    mRightEpgList?.setSelectedPosition(position)
                    mRightEpgList?.post {
                        mRightEpgList?.smoothScrollToPosition(position)
                    }
                    shiyi_time_c = getTime(formatDate.format(nowday) + " " + selectedData.start + ":30", formatDate.format(nowday) + " " + selectedData.end + ":30").toInt()
                    val lp = iv_play?.layoutParams
                    lp?.width = videoHeight / 7
                    lp?.height = videoHeight / 7
                    sBar = findViewById(R.id.pb_progressbar)
                    sBar?.max = shiyi_time_c * 1000
                    sBar?.progress = mVideoView?.currentPosition?.toInt() ?: 0
                    tv_currentpos?.text = durationToString(mVideoView?.currentPosition?.toInt() ?: 0)
                    tv_duration?.text = durationToString(shiyi_time_c * 1000)
                    showProgressBars(true)
                    isBack = true
                }
            }
        })

        epgListAdapter?.setOnItemClickListener { adapter, view, position ->
            if (position == currentLiveLookBackIndex) return@setOnItemClickListener
            currentLiveLookBackIndex = position
            val date = if (liveEpgDateAdapter?.getSelectedIndex() ?: -1 < 0) Date() else liveEpgDateAdapter!!.getData()[liveEpgDateAdapter!!.getSelectedIndex()].getDateParamVal()
            val dateFormat = SimpleDateFormat("yyyyMMdd")
            dateFormat.timeZone = TimeZone.getTimeZone("GMT+8:00")
            val selectedData = epgListAdapter?.getItem(position) ?: return@setOnItemClickListener
            val targetDate = dateFormat.format(date)
            val shiyiStartdate = targetDate + selectedData.originStart.replace(":", "") + "00"
            val shiyiEnddate = targetDate + selectedData.originEnd.replace(":", "") + "00"
            val now = Date()
            if (Date().compareTo(selectedData.startdateTime) < 0) return@setOnItemClickListener
            epgListAdapter?.setSelectedEpgIndex(position)
            if (now.compareTo(selectedData.startdateTime) >= 0 && now.compareTo(selectedData.enddateTime) <= 0) {
                mVideoView?.release()
                isSHIYI = false
                mVideoView?.setUrl(currentLiveChannelItem!!.getUrl(), liveWebHeader())
                mVideoView?.start()
                epgListAdapter?.setShiyiSelection(-1, false, timeFormat.format(date))
                epgListAdapter?.notifyDataSetChanged()
                showProgressBars(false)
                return@setOnItemClickListener
            }
            var shiyiUrl = currentLiveChannelItem!!.getUrl()
            if (now.compareTo(selectedData.startdateTime) < 0) {
            } else if (hasCatchup || shiyiUrl.contains("PLTV/") || shiyiUrl.contains("TVOD/")) {
                shiyiUrl = shiyiUrl.replace("/PLTV/", "/TVOD/")
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, 100)
                mVideoView?.release()
                shiyi_time = "$shiyiStartdate-$shiyiEnddate"
                isSHIYI = true
                if (hasCatchup) {
                    val replace = catchup!!.get("replace").asString
                    val source = catchup!!.get("source").asString
                    val parts = replace.split(",")
                    val left = if (parts.isNotEmpty()) parts[0].trim() else ""
                    val right = if (parts.size > 1) parts[1].trim() else ""
                    shiyiUrl = shiyiUrl.replace(left.toRegex(), right)
                    val startHHmm = selectedData.originStart.replace(":", "")
                    val endHHmm = selectedData.originEnd.replace(":", "")
                    val pattern = Pattern.compile("\\\$\\\$\{\\\((b|e)\\\)(.*?)\\\}")
                    val matcher = pattern.matcher(source)
                    val valueMap = hashMapOf<String, String>()
                    valueMap["b"] = targetDate + "T" + startHHmm
                    valueMap["e"] = targetDate + "T" + endHHmm
                    val result = StringBuffer()
                    while (matcher.find()) {
                        val type = matcher.group(1)
                        val replacement = valueMap[type]
                        if (replacement != null) {
                            matcher.appendReplacement(result, replacement)
                        }
                    }
                    matcher.appendTail(result)
                    LOG.i("echo-shiyiurl:$shiyiUrl")
                    if (shiyiUrl.endsWith("&")) shiyiUrl = shiyiUrl.substring(0, shiyiUrl.length - 1)
                    shiyiUrl += result.toString()
                } else {
                    if (shiyiUrl.indexOf("?") <= 0) {
                        shiyiUrl += "?playseek=$shiyi_time"
                    } else if (shiyiUrl.indexOf("playseek") > 0) {
                        shiyiUrl = shiyiUrl.replace("playseek=(.*)".toRegex(), "playseek=$shiyi_time")
                    } else {
                        shiyiUrl += "&playseek=$shiyi_time"
                    }
                }
                LOG.i("echo-回看地址playUrl : $shiyiUrl")
                playUrl = shiyiUrl
                liveWebHeader()?.let { LOG.i("echo-liveWebHeader : ${it.toString()}") }
                mVideoView?.setUrl(playUrl, liveWebHeader())
                mVideoView?.start()
                epgListAdapter?.setShiyiSelection(position, true, timeFormat.format(date))
                epgListAdapter?.notifyDataSetChanged()
                mRightEpgList?.setSelectedPosition(position)
                mRightEpgList?.post {
                    mRightEpgList?.smoothScrollToPosition(position)
                }
                shiyi_time_c = getTime(formatDate.format(nowday) + " " + selectedData.start + ":00", formatDate.format(nowday) + " " + selectedData.end + ":00").toInt()
                val lp = iv_play?.layoutParams
                lp?.width = videoHeight / 7
                lp?.height = videoHeight / 7
                sBar = findViewById(R.id.pb_progressbar)
                sBar?.max = shiyi_time_c * 1000
                sBar?.progress = mVideoView?.currentPosition?.toInt() ?: 0
                tv_currentpos?.text = durationToString(mVideoView?.currentPosition?.toInt() ?: 0)
                tv_duration?.text = durationToString(shiyi_time_c * 1000)
                showProgressBars(true)
                isBack = true
            }
        }
    }

    private fun initDayList() {
        liveDayList.clear()
        val daylist = LiveDayListGroup()
        val newday = Date(nowday.time)
        val day = formatDate1.format(newday)
        LOG.i("echo-date$day")
        daylist.setGroupIndex(0)
        daylist.setGroupName(day)
        liveDayList.add(daylist)
    }

    private fun initEpgDateView() {
        mEpgDateGridView?.setHasFixedSize(true)
        mEpgDateGridView?.setLayoutManager(V7LinearLayoutManager(this, 1, false))
        liveEpgDateAdapter = LiveEpgDateAdapter()
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        val datePresentFormat = SimpleDateFormat("MM-dd")
        calendar.add(Calendar.DAY_OF_MONTH, 0)
        for (i in 0 until 1) {
            val dateIns = calendar.time
            val epgDate = LiveEpgDate()
            epgDate.setIndex(i)
            epgDate.setDatePresented(datePresentFormat.format(dateIns))
            epgDate.setDateParamVal(dateIns)
            liveEpgDateAdapter?.addData(epgDate)
        }
        mEpgDateGridView?.adapter = liveEpgDateAdapter
        mEpgDateGridView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, postTimeout.toLong())
            }
        })
        liveEpgDateAdapter?.setSelectedIndex(0)
        mEpgDateGridView?.visibility = View.GONE
    }

    private fun initVideoView() {
        val controller = LiveController(this)
        controller.setListener(object : LiveController.LiveControlListener {
            override fun singleTap(): Boolean {
                showChannelList()
                return true
            }

            override fun longPress() {
                if (isBack) {
                    showProgressBars(true)
                } else {
                    showSettingGroup()
                }
            }

            override fun playStateChanged(playState: Int) {
                mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun)
                when (playState) {
                    VideoView.STATE_IDLE, VideoView.STATE_PAUSED -> {}
                    VideoView.STATE_PREPARED, VideoView.STATE_BUFFERED, VideoView.STATE_PLAYING -> {
                        currentLiveChangeSourceTimes = 0
                    }
                    VideoView.STATE_ERROR, VideoView.STATE_PLAYBACK_COMPLETED -> {
                        mHandler.postDelayed(mConnectTimeoutChangeSourceRun, 3500)
                    }
                    VideoView.STATE_PREPARING, VideoView.STATE_BUFFERING -> {
                        mHandler.postDelayed(mConnectTimeoutChangeSourceRun, (Hawk.get(HawkConfig.LIVE_CONNECT_TIMEOUT, 1) + 1) * 5000L)
                    }
                    else -> LOG.i("echo-Unexpected live_play state: $playState")
                }
            }

            override fun changeSource(direction: Int) {
                if (direction > 0) {
                    if (isBack) showProgressBars(true) else playNextSource()
                } else {
                    playPreSource()
                }
            }
        })
        controller.setCanChangePosition(false)
        controller.setEnableInNormal(true)
        controller.setGestureEnabled(true)
        controller.setDoubleTapTogglePlayEnabled(false)
        mVideoView?.setVideoController(controller)
        mVideoView?.setProgressManager(null)
    }

    private val mConnectTimeoutChangeSourceRun = Runnable {
        currentLiveChangeSourceTimes++
        if (currentLiveChannelItem?.getSourceNum() == currentLiveChangeSourceTimes) {
            currentLiveChangeSourceTimes = 0
            val groupChannelIndex = getNextChannel(if (Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)) -1 else 1)
            playChannel(groupChannelIndex[0], groupChannelIndex[1], false)
        } else {
            playNextSource()
        }
    }

    private fun initChannelGroupView() {
        mChannelGroupView?.setHasFixedSize(true)
        mChannelGroupView?.setLayoutManager(V7LinearLayoutManager(this, 1, false))

        liveChannelGroupAdapter = LiveChannelGroupAdapter()
        mChannelGroupView?.adapter = liveChannelGroupAdapter
        mChannelGroupView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, postTimeout.toLong())
            }
        })

        mChannelGroupView?.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {}
            override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                selectChannelGroup(position, true, -1)
            }
            override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
                if (isNeedInputPassword(position)) {
                    showPasswordDialog(position, -1)
                }
            }
        })

        liveChannelGroupAdapter?.setOnItemClickListener { adapter, view, position ->
            FastClickCheckUtil.check(view)
            selectChannelGroup(position, false, -1)
        }
    }

    private fun selectChannelGroup(groupIndex: Int, focus: Boolean, liveChannelIndex: Int) {
        mLastChannelGroupIndex = groupIndex
        if (focus) {
            liveChannelGroupAdapter?.setFocusedGroupIndex(groupIndex)
            liveChannelItemAdapter?.setFocusedChannelIndex(-1)
        }
        if ((groupIndex > -1 && groupIndex != liveChannelGroupAdapter!!.getSelectedGroupIndex()) || isNeedInputPassword(groupIndex)) {
            liveChannelGroupAdapter?.setSelectedGroupIndex(groupIndex)
            if (isNeedInputPassword(groupIndex)) {
                showPasswordDialog(groupIndex, liveChannelIndex)
                return
            }
            loadChannelGroupDataAndPlay(groupIndex, liveChannelIndex)
        }
        if (tvLeftChannelListLayout?.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun)
            mHandler.postDelayed(mHideChannelListRun, postTimeout.toLong())
        }
    }

    private fun initLiveChannelView() {
        mLiveChannelView?.setHasFixedSize(true)
        mLiveChannelView?.setLayoutManager(V7LinearLayoutManager(this, 1, false))

        liveChannelItemAdapter = LiveChannelItemAdapter()
        mLiveChannelView?.adapter = liveChannelItemAdapter
        mLiveChannelView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideChannelListRun)
                mHandler.postDelayed(mHideChannelListRun, postTimeout.toLong())
            }
        })

        mLiveChannelView?.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {}
            override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                if (position < 0) return
                liveChannelGroupAdapter?.setFocusedGroupIndex(-1)
                liveChannelItemAdapter?.setFocusedChannelIndex(position)
            }
            override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
                clickLiveChannel(position)
            }
        })

        liveChannelItemAdapter?.setOnItemClickListener { adapter, view, position ->
            FastClickCheckUtil.check(view)
            liveChannelItemAdapter?.setSelectedChannelIndex(position)
            clickLiveChannel(position)
        }
    }

    private fun clickLiveChannel(position: Int) {
        if (tvLeftChannelListLayout?.visibility == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun)
            mHandler.postDelayed(mHideChannelListRun, postTimeout.toLong())
        }
        playChannel(liveChannelGroupAdapter!!.getSelectedGroupIndex(), position, false)
    }

    private fun initSettingGroupView() {
        mSettingGroupView?.setHasFixedSize(true)
        mSettingGroupView?.setLayoutManager(V7LinearLayoutManager(this, 1, false))

        liveSettingGroupAdapter = LiveSettingGroupAdapter()
        mSettingGroupView?.adapter = liveSettingGroupAdapter
        mSettingGroupView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideSettingLayoutRun)
                mHandler.postDelayed(mHideSettingLayoutRun, postTimeout.toLong())
            }
        })

        mSettingGroupView?.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {}
            override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                selectSettingGroup(position, true)
            }
            override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {}
        })

        liveSettingGroupAdapter?.setOnItemClickListener { adapter, view, position ->
            FastClickCheckUtil.check(view)
            selectSettingGroup(position, false)
        }
    }

    private fun selectSettingGroup(position: Int, focus: Boolean) {
        if (!isCurrentLiveChannelValid()) return
        if (focus) {
            liveSettingGroupAdapter?.setFocusedGroupIndex(position)
            liveSettingItemAdapter?.setFocusedItemIndex(-1)
        }
        if (position == liveSettingGroupAdapter!!.getSelectedGroupIndex() || position < -1) return

        liveSettingGroupAdapter?.setSelectedGroupIndex(position)
        liveSettingItemAdapter?.setNewData(liveSettingGroupList[position].getLiveSettingItems())

        when (position) {
            0 -> liveSettingItemAdapter?.selectItem(currentLiveChannelItem!!.getSourceIndex(), true, false)
            1 -> liveSettingItemAdapter?.selectItem(livePlayerManager.getLivePlayerScale(), true, true)
            2 -> liveSettingItemAdapter?.selectItem(livePlayerManager.getLivePlayerType(), true, true)
        }
        var scrollToPosition = liveSettingItemAdapter!!.getSelectedItemIndex()
        if (scrollToPosition < 0) scrollToPosition = 0
        mSettingItemView?.scrollToPosition(scrollToPosition)
        mHandler.removeCallbacks(mHideSettingLayoutRun)
        mHandler.postDelayed(mHideSettingLayoutRun, postTimeout.toLong())
    }

    private fun initSettingItemView() {
        mSettingItemView?.setHasFixedSize(true)
        mSettingItemView?.setLayoutManager(V7LinearLayoutManager(this, 1, false))

        liveSettingItemAdapter = LiveSettingItemAdapter()
        mSettingItemView?.adapter = liveSettingItemAdapter
        mSettingItemView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                mHandler.removeCallbacks(mHideSettingLayoutRun)
                mHandler.postDelayed(mHideSettingLayoutRun, postTimeout.toLong())
            }
        })

        mSettingItemView?.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {}
            override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                if (position < 0) return
                liveSettingGroupAdapter?.setFocusedGroupIndex(-1)
                liveSettingItemAdapter?.setFocusedItemIndex(position)
                mHandler.removeCallbacks(mHideSettingLayoutRun)
                mHandler.postDelayed(mHideSettingLayoutRun, postTimeout.toLong())
            }
            override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
                clickSettingItem(position)
            }
        })

        liveSettingItemAdapter?.setOnItemClickListener { adapter, view, position ->
            FastClickCheckUtil.check(view)
            clickSettingItem(position)
        }
    }

    private fun clickSettingItem(position: Int) {
        val settingGroupIndex = liveSettingGroupAdapter!!.getSelectedGroupIndex()
        if (settingGroupIndex < 4) {
            if (position == liveSettingItemAdapter!!.getSelectedItemIndex()) return
            liveSettingItemAdapter?.selectItem(position, true, true)
        }
        when (settingGroupIndex) {
            0 -> {
                currentLiveChannelItem?.setSourceIndex(position)
                playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true)
            }
            1 -> livePlayerManager.changeLivePlayerScale(mVideoView, position, currentLiveChannelItem!!.getChannelName())
            2 -> {
                mVideoView?.release()
                livePlayerManager.changeLivePlayerType(mVideoView, position, currentLiveChannelItem!!.getChannelName())
                mVideoView?.setUrl(currentLiveChannelItem!!.getUrl(), liveWebHeader())
                mVideoView?.start()
            }
            3 -> Hawk.put(HawkConfig.LIVE_CONNECT_TIMEOUT, position)
            4 -> {
                var select = false
                when (position) {
                    0 -> {
                        select = !Hawk.get(HawkConfig.LIVE_SHOW_TIME, false)
                        Hawk.put(HawkConfig.LIVE_SHOW_TIME, select)
                        showTime()
                    }
                    1 -> {
                        select = !Hawk.get(HawkConfig.LIVE_SHOW_NET_SPEED, false)
                        Hawk.put(HawkConfig.LIVE_SHOW_NET_SPEED, select)
                        showNetSpeed()
                    }
                    2 -> {
                        select = !Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)
                        Hawk.put(HawkConfig.LIVE_CHANNEL_REVERSE, select)
                    }
                    3 -> {
                        select = !Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false)
                        Hawk.put(HawkConfig.LIVE_CROSS_GROUP, select)
                    }
                }
                liveSettingItemAdapter?.selectItem(position, select, false)
            }
            5 -> {
                if (mVideoView != null) {
                    mVideoView?.release()
                    mVideoView = null
                }
                if (position == Hawk.get(HawkConfig.LIVE_GROUP_INDEX, 0)) return
                val liveGroups: JsonArray = Hawk.get(HawkConfig.LIVE_GROUP_LIST, JsonArray())
                val livesOBJ = liveGroups[position].asJsonObject
                liveSettingItemAdapter?.selectItem(position, true, true)
                Hawk.put(HawkConfig.LIVE_GROUP_INDEX, position)
                ApiConfig.get().loadLiveApi(livesOBJ)
                recreate()
                return
            }
        }
        mHandler.removeCallbacks(mHideSettingLayoutRun)
        mHandler.postDelayed(mHideSettingLayoutRun, postTimeout.toLong())
    }

    private fun initLiveChannelList() {
        val list = ApiConfig.get().getChannelGroupList()
        if (list.isEmpty()) {
            setDefaultLiveChannelList()
            return
        }
        initLiveObj()
        if (list.size == 1 && list[0].getGroupName().startsWith("http://127.0.0.1")) {
            loadProxyLives(list[0].getGroupName())
        } else {
            liveChannelGroupList.clear()
            liveChannelGroupList.addAll(list)
            showSuccess()
            initLiveState()
        }
    }

    private fun loadProxyLives(url: String) {
        var urlVar = url
        try {
            val parsedUrl = Uri.parse(urlVar)
            urlVar = String(Base64.decode(parsedUrl.getQueryParameter("ext"), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
        } catch (th: Throwable) {
            if (!urlVar.startsWith("http://127.0.0.1")) {
                setDefaultLiveChannelList()
                return
            }
        }
        showLoading()

        LOG.i("echo-live-url:$urlVar")

        if (urlVar.contains(".py")) {
            if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(App.getInstance(), "该源需要存储权限", Toast.LENGTH_SHORT).show()
                setDefaultLiveChannelList()
                return
            }
            val finalUrl = urlVar
            val waitResponse = Runnable {
                val executor = Executors.newSingleThreadExecutor()
                val future = executor.submit(Callable<String> {
                    LOG.i("echo--loadProxyLives-json--")
                    val sp = ApiConfig.get().getPyCSP(finalUrl)
                    val json = sp.liveContent(finalUrl)
                    LOG.i("echo--loadProxyLives-json--$json")
                    json
                })
                var sortJson: String? = null
                try {
                    sortJson = future.get(10, TimeUnit.SECONDS)
                } catch (e: TimeoutException) {
                    e.printStackTrace()
                    future.cancel(true)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } finally {
                    if (sortJson.isNullOrEmpty()) {
                        mHandler.post { setDefaultLiveChannelList() }
                        return@Runnable
                    }
                    val linkedHashMap = LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>>()
                    TxtSubscribe.parse(linkedHashMap, sortJson)
                    val livesArray = TxtSubscribe.live2JsonArray(linkedHashMap)

                    ApiConfig.get().loadLives(livesArray)
                    val list = ApiConfig.get().getChannelGroupList()
                    if (list.isEmpty()) {
                        mHandler.post { setDefaultLiveChannelList() }
                        return@Runnable
                    }
                    liveChannelGroupList.clear()
                    liveChannelGroupList.addAll(list)

                    mHandler.post {
                        this@LivePlayActivity.showSuccess()
                        initLiveState()
                    }
                    try {
                        executor.shutdown()
                    } catch (th: Throwable) {
                        th.printStackTrace()
                    }
                }
            }
            Executors.newSingleThreadExecutor().execute(waitResponse)
        } else {
            OkGo.get<String>(urlVar).execute(object : AbsCallback<String> {
                override fun convertResponse(response: okhttp3.Response): String? {
                    return response.body?.string()
                }

                override fun onSuccess(response: Response<String>) {
                    val linkedHashMap = LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>>()
                    TxtSubscribe.parse(linkedHashMap, response.body())
                    val livesArray = TxtSubscribe.live2JsonArray(linkedHashMap)

                    ApiConfig.get().loadLives(livesArray)
                    val list = ApiConfig.get().getChannelGroupList()
                    if (list.isEmpty()) {
                        mHandler.post { setDefaultLiveChannelList() }
                        return
                    }
                    liveChannelGroupList.clear()
                    liveChannelGroupList.addAll(list)

                    mHandler.post {
                        this@LivePlayActivity.showSuccess()
                        initLiveState()
                    }
                }

                override fun onError(response: Response<String>) {
                    mHandler.post { setDefaultLiveChannelList() }
                }
            })
        }
    }

    private fun initLiveState() {
        val lastChannelName = Hawk.get(HawkConfig.LIVE_CHANNEL, "")

        var lastChannelGroupIndex = -1
        var lastLiveChannelIndex = -1
        for (liveChannelGroup in liveChannelGroupList) {
            for (liveChannelItem in liveChannelGroup.getLiveChannels()) {
                if (liveChannelItem.getChannelName() == lastChannelName) {
                    lastChannelGroupIndex = liveChannelGroup.getGroupIndex()
                    lastLiveChannelIndex = liveChannelItem.getChannelIndex()
                    break
                }
            }
            if (lastChannelGroupIndex != -1) break
        }
        if (lastChannelGroupIndex == -1) {
            lastChannelGroupIndex = getFirstNoPasswordChannelGroup()
            if (lastChannelGroupIndex == -1) lastChannelGroupIndex = 0
            lastLiveChannelIndex = 0
        }

        livePlayerManager.init(mVideoView)
        showTime()
        showNetSpeed()
        tvLeftChannelListLayout?.visibility = View.INVISIBLE
        tvRightSettingLayout?.visibility = View.INVISIBLE

        liveChannelGroupAdapter?.setNewData(liveChannelGroupList)
        selectChannelGroup(lastChannelGroupIndex, false, lastLiveChannelIndex)
    }

    private fun isListOrSettingLayoutVisible(): Boolean {
        return tvLeftChannelListLayout?.visibility == View.VISIBLE || tvRightSettingLayout?.visibility == View.VISIBLE
    }

    private fun initLiveSettingGroupList() {
        liveSettingGroupList = ApiConfig.get().getLiveSettingGroupList()
        liveSettingGroupList[3].getLiveSettingItems()[Hawk.get(HawkConfig.LIVE_CONNECT_TIMEOUT, 1)].setItemSelected(true)
        liveSettingGroupList[4].getLiveSettingItems()[0].setItemSelected(Hawk.get(HawkConfig.LIVE_SHOW_TIME, false))
        liveSettingGroupList[4].getLiveSettingItems()[1].setItemSelected(Hawk.get(HawkConfig.LIVE_SHOW_NET_SPEED, false))
        liveSettingGroupList[4].getLiveSettingItems()[2].setItemSelected(Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false))
        liveSettingGroupList[4].getLiveSettingItems()[3].setItemSelected(Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false))
        liveSettingGroupList[5].getLiveSettingItems()[Hawk.get(HawkConfig.LIVE_GROUP_INDEX, 0)].setItemSelected(true)
    }

    private fun loadCurrentSourceList() {
        val currentSourceNames = currentLiveChannelItem!!.getChannelSourceNames()
        val liveSettingItemList = ArrayList<LiveSettingItem>()
        for (j in currentSourceNames.indices) {
            val liveSettingItem = LiveSettingItem()
            liveSettingItem.setItemIndex(j)
            liveSettingItem.setItemName(currentSourceNames[j])
            liveSettingItemList.add(liveSettingItem)
        }
        liveSettingGroupList[0].setLiveSettingItems(liveSettingItemList)
    }

    private fun showTime() {
        if (Hawk.get(HawkConfig.LIVE_SHOW_TIME, false)) {
            mHandler.post(mUpdateTimeRun)
            tvTime?.visibility = View.VISIBLE
        } else {
            mHandler.removeCallbacks(mUpdateTimeRun)
            tvTime?.visibility = View.GONE
        }
    }

    private val mUpdateTimeRun = Runnable {
        val day = Date()
        val df = SimpleDateFormat("hh:mm a")
        tvTime?.text = df.format(day)
        mHandler.postDelayed(this, 1000)
    }

    private fun showNetSpeed() {
        if (Hawk.get(HawkConfig.LIVE_SHOW_NET_SPEED, false)) {
            mHandler.post(mUpdateNetSpeedRun)
            tvNetSpeed?.visibility = View.VISIBLE
        } else {
            mHandler.removeCallbacks(mUpdateNetSpeedRun)
            tvNetSpeed?.visibility = View.GONE
        }
    }

    private val mUpdateNetSpeedRun = Runnable {
        if (mVideoView == null) return@Runnable
        val speed = PlayerHelper.getDisplaySpeed(mVideoView!!.getTcpSpeed(), true)
        tvNetSpeed?.text = speed
        mHandler.postDelayed(this, 1000)
    }

    private fun showPasswordDialog(groupIndex: Int, liveChannelIndex: Int) {
        if (tvLeftChannelListLayout?.visibility == View.VISIBLE)
            mHandler.removeCallbacks(mHideChannelListRun)

        val dialog = LivePasswordDialog(this)
        dialog.setOnListener(object : LivePasswordDialog.OnListener {
            override fun onChange(password: String) {
                if (password == liveChannelGroupList[groupIndex].getGroupPassword()) {
                    channelGroupPasswordConfirmed.add(groupIndex)
                    loadChannelGroupDataAndPlay(groupIndex, liveChannelIndex)
                } else {
                    Toast.makeText(App.getInstance(), "密码错误", Toast.LENGTH_SHORT).show()
                }

                if (tvLeftChannelListLayout?.visibility == View.VISIBLE)
                    mHandler.postDelayed(mHideChannelListRun, postTimeout.toLong())
            }

            override fun onCancel() {
                if (tvLeftChannelListLayout?.visibility == View.VISIBLE) {
                    val groupIndex = liveChannelGroupAdapter!!.getSelectedGroupIndex()
                    liveChannelItemAdapter?.setNewData(getLiveChannels(groupIndex))
                }
            }
        })
        dialog.show()
    }

    private fun loadChannelGroupDataAndPlay(groupIndex: Int, liveChannelIndex: Int) {
        liveChannelItemAdapter?.setNewData(getLiveChannels(groupIndex))
        if (groupIndex == currentChannelGroupIndex) {
            if (currentLiveChannelIndex > -1)
                mLiveChannelView?.scrollToPosition(currentLiveChannelIndex)
            liveChannelItemAdapter?.setSelectedChannelIndex(currentLiveChannelIndex)
        } else {
            mLiveChannelView?.scrollToPosition(0)
            liveChannelItemAdapter?.setSelectedChannelIndex(-1)
        }

        if (liveChannelIndex > -1) {
            clickLiveChannel(liveChannelIndex)
            mChannelGroupView?.scrollToPosition(groupIndex)
            mLiveChannelView?.scrollToPosition(liveChannelIndex)
            playChannel(groupIndex, liveChannelIndex, false)
        }
    }

    private fun isNeedInputPassword(groupIndex: Int): Boolean {
        return liveChannelGroupList[groupIndex].getGroupPassword().isNotEmpty() && !isPasswordConfirmed(groupIndex)
    }

    private fun isPasswordConfirmed(groupIndex: Int): Boolean {
        for (confirmedNum in channelGroupPasswordConfirmed) {
            if (confirmedNum == groupIndex) return true
        }
        return false
    }

    private fun getLiveChannels(groupIndex: Int): ArrayList<LiveChannelItem> {
        return if (!isNeedInputPassword(groupIndex)) {
            liveChannelGroupList[groupIndex].getLiveChannels()
        } else {
            ArrayList()
        }
    }

    private fun getNextChannel(direction: Int): Array<Int> {
        var channelGroupIndex = currentChannelGroupIndex
        var liveChannelIndex = currentLiveChannelIndex

        if (direction > 0) {
            liveChannelIndex++
            if (liveChannelIndex >= getLiveChannels(channelGroupIndex).size) {
                liveChannelIndex = 0
                if (Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false)) {
                    do {
                        channelGroupIndex++
                        if (channelGroupIndex >= liveChannelGroupList.size) channelGroupIndex = 0
                    } while (liveChannelGroupList[channelGroupIndex].getGroupPassword().isNotEmpty() || channelGroupIndex == currentChannelGroupIndex)
                }
            }
        } else {
            liveChannelIndex--
            if (liveChannelIndex < 0) {
                if (Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false)) {
                    do {
                        channelGroupIndex--
                        if (channelGroupIndex < 0) channelGroupIndex = liveChannelGroupList.size - 1
                    } while (liveChannelGroupList[channelGroupIndex].getGroupPassword().isNotEmpty() || channelGroupIndex == currentChannelGroupIndex)
                }
                liveChannelIndex = getLiveChannels(channelGroupIndex).size - 1
            }
        }

        return arrayOf(channelGroupIndex, liveChannelIndex)
    }

    private fun getFirstNoPasswordChannelGroup(): Int {
        for (liveChannelGroup in liveChannelGroupList) {
            if (liveChannelGroup.getGroupPassword().isEmpty()) return liveChannelGroup.getGroupIndex()
        }
        return -1
    }

    private fun isCurrentLiveChannelValid(): Boolean {
        if (currentLiveChannelItem == null) {
            Toast.makeText(App.getInstance(), "请先选择频道", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun durationToString(duration: Int): String {
        val dur = duration / 1000
        val hour = dur / 3600
        val min = (dur / 60) % 60
        val sec = dur % 60
        return when {
            hour > 0 -> {
                when {
                    min > 9 && sec > 9 -> "$hour:$min:$sec"
                    min > 9 -> "$hour:$min:0$sec"
                    sec > 9 -> "$hour:0$min:$sec"
                    else -> "$hour:0$min:0$sec"
                }
            }
            else -> {
                when {
                    min > 9 && sec > 9 -> "$min:$sec"
                    min > 9 -> "$min:0$sec"
                    sec > 9 -> "0$min:$sec"
                    else -> "0$min:0$sec"
                }
            }
        }
    }

    fun showProgressBars(show: Boolean) {
        sBar?.requestFocus()
        if (show) {
            ll_right_top_huikan?.visibility = View.VISIBLE
            backcontroller?.visibility = View.VISIBLE
            ll_epg?.visibility = View.GONE
        } else {
            backcontroller?.visibility = View.GONE
            ll_right_top_huikan?.visibility = View.GONE
            if (tip_epg1?.text?.toString() != "暂无信息") {
                ll_epg?.visibility = View.VISIBLE
            }
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
                if (fromUser && countDownTimer != null) {
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

        if (mVideoView!!.isPlaying()) {
            iv_play?.visibility = View.INVISIBLE
            iv_playpause?.background = ContextCompat.getDrawable(this@LivePlayActivity.context!!, R.drawable.vod_pause)
        } else {
            iv_play?.visibility = View.VISIBLE
            iv_playpause?.background = ContextCompat.getDrawable(this@LivePlayActivity.context!!, R.drawable.icon_play)
        }

        if (countDownTimer3 == null) {
            countDownTimer3 = object : CountDownTimer(postTimeout.toLong(), 1000) {
                override fun onTick(arg0: Long) {
                    if (mVideoView != null) {
                        sBar?.progress = mVideoView?.currentPosition?.toInt() ?: 0
                        tv_currentpos?.text = durationToString(mVideoView?.currentPosition?.toInt() ?: 0)
                    }
                }

                override fun onFinish() {
                    if (backcontroller?.visibility == View.VISIBLE) {
                        backcontroller?.visibility = View.GONE
                    }
                }
            }
        } else {
            countDownTimer3?.cancel()
        }
        countDownTimer3?.start()
    }

    private fun setDefaultLiveChannelList() {
        liveChannelGroupList.clear()
        val defaultGroup = LiveChannelGroup()
        defaultGroup.setGroupIndex(0)
        defaultGroup.setGroupName("default group")
        defaultGroup.setGroupPassword("")
        val defaultChannel = LiveChannelItem()
        defaultChannel.setChannelName("default channel")
        defaultChannel.setChannelIndex(0)
        defaultChannel.setChannelNum(1)
        val defaultSourceNames = ArrayList<String>()
        val defaultSourceUrls = ArrayList<String>()
        defaultSourceNames.add("default source")
        defaultSourceUrls.add("http://default.play.url/stream")
        defaultChannel.setChannelSourceNames(defaultSourceNames)
        defaultChannel.setChannelUrls(defaultSourceUrls)
        val channels = ArrayList<LiveChannelItem>()
        channels.add(defaultChannel)
        defaultGroup.setLiveChannels(channels)
        liveChannelGroupList.add(defaultGroup)
        showSuccess()
        initLiveState()
    }
}
