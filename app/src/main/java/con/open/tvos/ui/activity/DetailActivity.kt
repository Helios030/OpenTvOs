package con.open.tvos.ui.activity

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearSmoothScroller
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.App
import con.open.tvos.base.BaseActivity
import con.open.tvos.bean.AbsXml
import con.open.tvos.bean.Movie
import con.open.tvos.bean.SourceBean
import con.open.tvos.bean.VodInfo
import con.open.tvos.cache.RoomDataManger
import con.open.tvos.event.RefreshEvent
import con.open.tvos.picasso.RoundTransformation
import con.open.tvos.ui.adapter.SeriesAdapter
import con.open.tvos.ui.adapter.SeriesFlagAdapter
import con.open.tvos.ui.dialog.DescDialog
import con.open.tvos.ui.dialog.QuickSearchDialog
import con.open.tvos.ui.fragment.PlayFragment
import con.open.tvos.util.DefaultConfig
import con.open.tvos.util.FastClickCheckUtil
import con.open.tvos.util.HawkConfig
import con.open.tvos.util.MD5
import con.open.tvos.util.SearchHelper
import con.open.tvos.util.SubtitleHelper
import con.open.tvos.viewmodel.SourceViewModel
import com.lzy.okgo.OkGo
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import com.squareup.picasso.Picasso
import me.jessyan.autosize.utils.AutoSizeUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */
class DetailActivity : BaseActivity() {
    private lateinit var llLayout: LinearLayout
    private lateinit var llPlayerFragmentContainer: FragmentContainerView
    private lateinit var llPlayerFragmentContainerBlock: View
    private lateinit var llPlayerPlace: View
    private var playFragment: PlayFragment? = null
    private lateinit var ivThumb: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvYear: TextView
    private lateinit var tvSite: TextView
    private lateinit var tvArea: TextView
    private lateinit var tvLang: TextView
    private lateinit var tvType: TextView
    private lateinit var tvActor: TextView
    private lateinit var tvDirector: TextView
    private lateinit var tvPlayUrl: TextView
    private lateinit var tvDes: TextView
    private lateinit var tvPlay: TextView
    private lateinit var tvDesc: TextView
    private lateinit var tvSeriesSort: TextView
    private lateinit var tvQuickSearch: TextView
    private lateinit var tvCollect: TextView
    private lateinit var mGridViewFlag: TvRecyclerView
    private lateinit var mGridView: TvRecyclerView
    private lateinit var mSeriesGroupView: TvRecyclerView
    private lateinit var mEmptyPlayList: LinearLayout
    private lateinit var tvSeriesGroup: LinearLayout
    private lateinit var sourceViewModel: SourceViewModel
    private var mVideo: Movie.Video? = null
    private var vodInfo: VodInfo? = null
    private lateinit var seriesFlagAdapter: SeriesFlagAdapter
    private lateinit var seriesGroupAdapter: BaseQuickAdapter<String, BaseViewHolder>
    private lateinit var seriesAdapter: SeriesAdapter
    var vodId: String? = null
    var sourceKey: String? = null
    var firstsourceKey: String? = null
    var seriesSelect = false
    private var seriesFlagFocus: View? = null
    private var isReverse = false
    private var preFlag = ""
    private var firstReverse = false
    private var mGridViewLayoutMgr: V7GridLayoutManager? = null
    private var mCheckSources: HashMap<String, String>? = null
    private val seriesGroupOptions = ArrayList<String>()
    private var currentSeriesGroupView: View? = null
    private var GroupCount = 0
    var showPreview = Hawk.get(HawkConfig.SHOW_PREVIEW, true)

    private var smoothScroller: LinearSmoothScroller? = null

    override fun getLayoutResID(): Int = R.layout.activity_detail

    override fun init() {
        EventBus.getDefault().register(this)
        initView()
        initViewModel()
        initData()
    }

    private fun initView() {
        llLayout = findViewById(R.id.llLayout)
        llPlayerPlace = findViewById(R.id.previewPlayerPlace)
        llPlayerFragmentContainer = findViewById(R.id.previewPlayer)
        llPlayerFragmentContainerBlock = findViewById(R.id.previewPlayerBlock)
        ivThumb = findViewById(R.id.ivThumb)
        llPlayerPlace.visibility = if (showPreview) View.VISIBLE else View.GONE
        ivThumb.visibility = if (!showPreview) View.VISIBLE else View.GONE
        tvName = findViewById(R.id.tvName)
        tvYear = findViewById(R.id.tvYear)
        tvSite = findViewById(R.id.tvSite)
        tvArea = findViewById(R.id.tvArea)
        tvLang = findViewById(R.id.tvLang)
        tvType = findViewById(R.id.tvType)
        tvActor = findViewById(R.id.tvActor)
        tvDirector = findViewById(R.id.tvDirector)
        tvPlayUrl = findViewById(R.id.tvPlayUrl)
        tvDes = findViewById(R.id.tvDes)
        tvPlay = findViewById(R.id.tvPlay)
        tvDesc = findViewById(R.id.tvDesc)
        tvSeriesSort = findViewById(R.id.mSeriesSortTv)
        tvCollect = findViewById(R.id.tvCollect)
        tvQuickSearch = findViewById(R.id.tvQuickSearch)
        mEmptyPlayList = findViewById(R.id.mEmptyPlaylist)
        mGridView = findViewById(R.id.mGridView)
        mGridView.setHasFixedSize(false)
        mGridViewLayoutMgr = V7GridLayoutManager(mContext, 6)
        mGridView.layoutManager = mGridViewLayoutMgr

        smoothScroller = object : LinearSmoothScroller(mContext) {
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 100f / displayMetrics.densityDpi
            }

            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                return mGridViewLayoutMgr?.computeScrollVectorForPosition(targetPosition)
            }
        }

        seriesAdapter = SeriesAdapter(mGridViewLayoutMgr!!)
        mGridView.adapter = seriesAdapter
        mGridViewFlag = findViewById(R.id.mGridViewFlag)
        mGridViewFlag.setHasFixedSize(true)
        mGridViewFlag.layoutManager = V7LinearLayoutManager(mContext, 0, false)
        seriesFlagAdapter = SeriesFlagAdapter()
        mGridViewFlag.adapter = seriesFlagAdapter
        isReverse = false
        firstReverse = false
        preFlag = ""
        if (showPreview) {
            playFragment = PlayFragment()
            supportFragmentManager.beginTransaction().add(R.id.previewPlayer, playFragment!!).commit()
            supportFragmentManager.beginTransaction().show(playFragment!!).commitAllowingStateLoss()
            tvPlay.text = "全屏"
        }
        llPlayerFragmentContainerBlock.isFocusable = showPreview

        mSeriesGroupView = findViewById(R.id.mSeriesGroupView)
        tvSeriesGroup = findViewById(R.id.mSeriesGroupTv)
        mSeriesGroupView.setHasFixedSize(true)
        mSeriesGroupView.layoutManager = V7LinearLayoutManager(mContext, 0, false)
        seriesGroupAdapter = object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_series_group, seriesGroupOptions) {
            override fun convert(helper: BaseViewHolder, item: String) {
                val tvSeries = helper.getView<TextView>(R.id.tvSeriesGroup)
                tvSeries.text = item
                if (helper.layoutPosition == data.size - 1) {
                    helper.itemView.id = View.generateViewId()
                    helper.itemView.nextFocusRightId = helper.itemView.id
                } else {
                    helper.itemView.nextFocusRightId = View.NO_ID
                }
            }
        }
        mSeriesGroupView.adapter = seriesGroupAdapter

        llPlayerFragmentContainerBlock.setOnClickListener {
            toggleFullPreview()
            if (firstReverse) {
                jumpToPlay()
                firstReverse = false
            }
        }

        tvPlay.setOnClickListener { v ->
            FastClickCheckUtil.check(v)
            if (showPreview) {
                toggleFullPreview()
                if (firstReverse) {
                    jumpToPlay()
                    firstReverse = false
                }
            } else {
                jumpToPlay()
            }
        }

        tvQuickSearch.setOnClickListener {
            startQuickSearch()
            val quickSearchDialog = QuickSearchDialog(this@DetailActivity)
            EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH, quickSearchData))
            EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, quickSearchWord))
            quickSearchDialog.show()
            if (pauseRunnable != null && pauseRunnable!!.size > 0) {
                searchExecutorService = Executors.newFixedThreadPool(5)
                for (runnable in pauseRunnable!!) {
                    searchExecutorService!!.execute(runnable)
                }
                pauseRunnable = null
            }
            quickSearchDialog.setOnDismissListener {
                try {
                    if (searchExecutorService != null) {
                        pauseRunnable = searchExecutorService!!.shutdownNow()
                        searchExecutorService = null
                    }
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }
        }

        tvCollect.setOnClickListener {
            val text = tvCollect.text.toString()
            if ("加入收藏" == text) {
                RoomDataManger.insertVodCollect(sourceKey!!, vodInfo!!)
                Toast.makeText(this@DetailActivity, "已加入收藏夹", Toast.LENGTH_SHORT).show()
                tvCollect.text = "取消收藏"
            } else {
                RoomDataManger.deleteVodCollect(sourceKey!!, vodInfo!!)
                Toast.makeText(this@DetailActivity, "已移除收藏夹", Toast.LENGTH_SHORT).show()
                tvCollect.text = "加入收藏"
            }
        }

        tvPlayUrl.setOnClickListener {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(null, tvPlayUrl.text.toString().replace("播放地址：", "")))
            Toast.makeText(this@DetailActivity, "已复制", Toast.LENGTH_SHORT).show()
        }

        tvSeriesSort.setOnClickListener {
            if (vodInfo != null && vodInfo!!.seriesMap.size > 0) {
                vodInfo!!.reverseSort = !vodInfo!!.reverseSort
                isReverse = !isReverse
                tvSeriesSort.text = if (isReverse) "倒序" else "正序"
                vodInfo!!.reverse()
                vodInfo!!.playIndex = (vodInfo!!.seriesMap[vodInfo!!.playFlag]!!.size - 1) - vodInfo!!.playIndex
                firstReverse = !firstReverse
                setSeriesGroupOptions()
                seriesAdapter.notifyDataSetChanged()

                customSeriesScrollPos(vodInfo!!.playIndex)
                if (currentSeriesGroupView != null) {
                    val txtView = currentSeriesGroupView!!.findViewById<TextView>(R.id.tvSeriesGroup)
                    txtView.setTextColor(Color.WHITE)
                }
            }
        }

        tvDesc.setOnClickListener { v ->
            runOnUiThread {
                FastClickCheckUtil.check(v)
                val dialog = DescDialog(mContext)
                dialog.setDescribe(removeHtmlTag(mVideo!!.des))
                dialog.show()
            }
        }

        mGridView.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                seriesSelect = false
            }

            override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                seriesSelect = true
            }

            override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
            }
        })

        mGridViewFlag.setOnItemListener(object : TvRecyclerView.OnItemListener {
            private fun refresh(itemView: View, position: Int) {
                val newFlag = seriesFlagAdapter.data[position].name
                if (vodInfo != null && vodInfo!!.playFlag != newFlag) {
                    for (i in 0 until vodInfo!!.seriesFlags.size) {
                        val flag = vodInfo!!.seriesFlags[i]
                        if (flag.name == vodInfo!!.playFlag) {
                            flag.selected = false
                            seriesFlagAdapter.notifyItemChanged(i)
                            break
                        }
                    }
                    val flag = vodInfo!!.seriesFlags[position]
                    flag.selected = true
                    if (vodInfo!!.seriesMap[vodInfo!!.playFlag]!!.size > vodInfo!!.playIndex) {
                        vodInfo!!.seriesMap[vodInfo!!.playFlag]!![vodInfo!!.playIndex].selected = false
                    }
                    vodInfo!!.playFlag = newFlag
                    seriesFlagAdapter.notifyItemChanged(position)
                    refreshList()
                    mGridView.clearFocus()
                }
                seriesFlagFocus = itemView
            }

            override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
            }

            override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                refresh(itemView!!, position)
            }

            override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
                refresh(itemView!!, position)
            }
        })

        seriesAdapter.setOnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            if (vodInfo != null && vodInfo!!.seriesMap[vodInfo!!.playFlag]!!.size > 0) {
                var reload = false
                var isAllowFull = false
                for (j in 0 until vodInfo!!.seriesMap[vodInfo!!.playFlag]!!.size) {
                    seriesAdapter.data[j].selected = false
                    seriesAdapter.notifyItemChanged(j)
                }
                if (vodInfo!!.playIndex != position) {
                    seriesAdapter.data[position].selected = true
                    seriesAdapter.notifyItemChanged(position)
                    vodInfo!!.playIndex = position
                    reload = true
                }
                if (preFlag.isNotEmpty() && vodInfo!!.playFlag != preFlag) {
                    reload = true
                    isAllowFull = true
                }

                seriesAdapter.data[vodInfo!!.playIndex].selected = true
                seriesAdapter.notifyItemChanged(vodInfo!!.playIndex)
                if (showPreview && !fullWindows && !isAllowFull && playFragment!!.player.isPlaying) toggleFullPreview()
                if (!showPreview || reload) {
                    jumpToPlay()
                    firstReverse = false
                }
            }
        }

        mSeriesGroupView.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                val txtView = itemView!!.findViewById<TextView>(R.id.tvSeriesGroup)
                txtView.setTextColor(Color.WHITE)
            }

            override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                val txtView = itemView!!.findViewById<TextView>(R.id.tvSeriesGroup)
                txtView.setTextColor(mContext.resources.getColor(R.color.color_02F8E1))
                if (vodInfo != null && vodInfo!!.seriesMap[vodInfo!!.playFlag]!!.size > 0) {
                    val targetPos = position * GroupCount
                    customSeriesScrollPos(targetPos)
                }
                currentSeriesGroupView = itemView
            }

            override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
            }
        })

        tvSeriesSort.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                tvSeriesSort.setTextColor(mContext.resources.getColor(R.color.color_02F8E1))
                if (vodInfo != null && vodInfo!!.seriesMap[vodInfo!!.playFlag]!!.size > 0) {
                    val firstVisible = mGridView.firstVisiblePosition
                    val lastVisible = mGridView.lastVisiblePosition
                    if (vodInfo!!.playIndex < firstVisible || vodInfo!!.playIndex > lastVisible) {
                        customSeriesScrollPos(vodInfo!!.playIndex)
                    }
                }
            } else {
                tvSeriesSort.setTextColor(Color.WHITE)
            }
        }

        seriesGroupAdapter.setOnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            val newTxtView = view.findViewById<TextView>(R.id.tvSeriesGroup)
            newTxtView.setTextColor(mContext.resources.getColor(R.color.color_02F8E1))
            if (vodInfo != null && vodInfo!!.seriesMap[vodInfo!!.playFlag]!!.size > 0) {
                val targetPos = position * GroupCount + 1
                customSeriesScrollPos(targetPos)
            }
            if (currentSeriesGroupView != null) {
                val txtView = currentSeriesGroupView!!.findViewById<TextView>(R.id.tvSeriesGroup)
                txtView.setTextColor(Color.WHITE)
            }
            currentSeriesGroupView = view
        }

        if (showPreview) {
            llPlayerFragmentContainerBlock.requestFocus()
        } else {
            tvPlay.requestFocus()
        }
        setLoadSir(llLayout)
    }

    private fun customSeriesScrollPos(targetPos: Int) {
        mGridViewLayoutMgr!!.scrollToPositionWithOffset(if (targetPos > 10) targetPos - 10 else 0, 0)
        mGridView.postDelayed({
            smoothScroller!!.targetPosition = targetPos
            mGridViewLayoutMgr!!.startSmoothScroll(smoothScroller)
            mGridView.smoothScrollToPosition(targetPos)
        }, 50)
    }

    private fun initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch()
    }

    private var pauseRunnable: MutableList<Runnable>? = null

    private fun jumpToPlay() {
        if (vodInfo != null && vodInfo!!.seriesMap[vodInfo!!.playFlag]!!.size > 0) {
            preFlag = vodInfo!!.playFlag
            setTextShow(tvPlayUrl, "播放地址：", vodInfo!!.seriesMap[vodInfo!!.playFlag]!![vodInfo!!.playIndex].url)
            val bundle = Bundle()
            insertVod(firstsourceKey!!, vodInfo!!)
            bundle.putString("sourceKey", sourceKey)
            App.getInstance().setVodInfo(vodInfo)
            if (showPreview) {
                if (previewVodInfo == null) {
                    try {
                        val bos = ByteArrayOutputStream()
                        val oos = ObjectOutputStream(bos)
                        oos.writeObject(vodInfo)
                        oos.flush()
                        oos.close()
                        val ois = ObjectInputStream(ByteArrayInputStream(bos.toByteArray()))
                        previewVodInfo = ois.readObject() as VodInfo
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (previewVodInfo != null) {
                    previewVodInfo!!.playerCfg = vodInfo!!.playerCfg
                    previewVodInfo!!.playFlag = vodInfo!!.playFlag
                    previewVodInfo!!.playIndex = vodInfo!!.playIndex
                    previewVodInfo!!.seriesMap = vodInfo!!.seriesMap
                    App.getInstance().setVodInfo(previewVodInfo)
                }
                playFragment!!.setData(bundle)
            } else {
                jumpActivity(PlayActivity::class.java, bundle)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshList() {
        if (vodInfo!!.seriesMap[vodInfo!!.playFlag]!!.size <= vodInfo!!.playIndex) {
            vodInfo!!.playIndex = 0
        }

        if (vodInfo!!.seriesMap[vodInfo!!.playFlag] != null) {
            var canSelect = true
            for (j in 0 until vodInfo!!.seriesMap[vodInfo!!.playFlag]!!.size) {
                if (vodInfo!!.seriesMap[vodInfo!!.playFlag]!![j].selected) {
                    canSelect = false
                    break
                }
            }
            if (canSelect) vodInfo!!.seriesMap[vodInfo!!.playFlag]!![vodInfo!!.playIndex].selected = true
        }

        val pFont = Paint()
        val rect = Rect()

        val list = vodInfo!!.seriesMap[vodInfo!!.playFlag]
        val listSize = list!!.size
        var w = 1
        for (i in 0 until listSize) {
            val name = list[i].name
            pFont.getTextBounds(name, 0, name.length, rect)
            if (w < rect.width()) {
                w = rect.width()
            }
        }
        w += 32
        val screenWidth = windowManager.defaultDisplay.width / 3
        var offset = screenWidth / w
        if (offset <= 2) offset = 2
        if (offset > 6) offset = 6
        mGridViewLayoutMgr!!.spanCount = offset
        seriesAdapter.setNewData(vodInfo!!.seriesMap[vodInfo!!.playFlag])

        setSeriesGroupOptions()

        mGridView.postDelayed({
            customSeriesScrollPos(vodInfo!!.playIndex)
        }, 100)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setSeriesGroupOptions() {
        val list = vodInfo!!.seriesMap[vodInfo!!.playFlag]
        val listSize = list!!.size
        val offset = mGridViewLayoutMgr!!.spanCount
        seriesGroupOptions.clear()
        GroupCount = if (offset == 3 || offset == 6) 30 else 20
        if (listSize > 100 && listSize <= 400) GroupCount = 60
        if (listSize > 400) GroupCount = 120
        if (listSize > 1) {
            tvSeriesGroup.visibility = View.VISIBLE
            val remainedOptionSize = listSize % GroupCount
            val optionSize = listSize / GroupCount

            for (i in 0 until optionSize) {
                if (vodInfo!!.reverseSort)
                    seriesGroupOptions.add(String.format("%d - %d", listSize - (i * GroupCount + 1) + 1, listSize - (i * GroupCount + GroupCount) + 1))
                else
                    seriesGroupOptions.add(String.format("%d - %d", i * GroupCount + 1, i * GroupCount + GroupCount))
            }
            if (remainedOptionSize > 0) {
                if (vodInfo!!.reverseSort)
                    seriesGroupOptions.add(String.format("%d - %d", listSize - (optionSize * GroupCount + 1) + 1, listSize - (optionSize * GroupCount + remainedOptionSize) + 1))
                else
                    seriesGroupOptions.add(String.format("%d - %d", optionSize * GroupCount + 1, optionSize * GroupCount + remainedOptionSize))
            }

            seriesGroupAdapter.notifyDataSetChanged()
        } else {
            tvSeriesGroup.visibility = View.GONE
        }
    }

    private fun setTextShow(view: TextView, tag: String, info: String?) {
        if (info.isNullOrEmpty() || info.trim().isEmpty()) {
            view.visibility = View.GONE
            return
        }
        view.visibility = View.VISIBLE
        view.text = Html.fromHtml(getHtml(tag, info))
    }

    private fun removeHtmlTag(info: String?): String {
        if (info == null) return ""
        return info.replace("\\<.*?\\>".toRegex(), "").replace("\\s".toRegex(), "")
    }

    private fun initViewModel() {
        sourceViewModel = ViewModelProvider(this)[SourceViewModel::class.java]
        sourceViewModel.detailResult.observe(this) { absXml ->
            if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size > 0) {
                showSuccess()
                if (!TextUtils.isEmpty(absXml.msg) && absXml.msg != "数据列表") {
                    Toast.makeText(this@DetailActivity, absXml.msg, Toast.LENGTH_SHORT).show()
                    showEmpty()
                    return@observe
                }
                mVideo = absXml.movie.videoList[0]
                mVideo!!.id = vodId!!
                if (TextUtils.isEmpty(mVideo!!.name)) mVideo!!.name = "TVBox"
                vodInfo = VodInfo()
                if ((mVideo!!.pic.isNullOrEmpty()) && vod_picture.isNotEmpty()) {
                    mVideo!!.pic = vod_picture
                }
                vodInfo!!.setVideo(mVideo!!)
                vodInfo!!.sourceKey = mVideo!!.sourceKey
                sourceKey = mVideo!!.sourceKey

                tvName.text = mVideo!!.name
                setTextShow(tvSite, "来源：", ApiConfig.get().getSource(firstsourceKey!!).name)
                setTextShow(tvYear, "年份：", if (mVideo!!.year == 0) "" else mVideo!!.year.toString())
                setTextShow(tvArea, "地区：", mVideo!!.area)
                setTextShow(tvLang, "语言：", mVideo!!.lang)
                if (firstsourceKey != sourceKey) {
                    setTextShow(tvType, "类型：", "[" + ApiConfig.get().getSource(sourceKey!!).name + "] 解析")
                } else {
                    setTextShow(tvType, "类型：", mVideo!!.type)
                }
                setTextShow(tvActor, "演员：", mVideo!!.actor)
                setTextShow(tvDirector, "导演：", mVideo!!.director)
                setTextShow(tvDes, "内容简介：", removeHtmlTag(mVideo!!.des))
                if (!TextUtils.isEmpty(mVideo!!.pic)) {
                    Picasso.get()
                        .load(DefaultConfig.checkReplaceProxy(mVideo!!.pic))
                        .transform(
                            RoundTransformation(MD5.string2MD5(mVideo!!.pic))
                                .centerCorp(true)
                                .override(AutoSizeUtils.mm2px(mContext, 300f), AutoSizeUtils.mm2px(mContext, 400f))
                                .roundRadius(AutoSizeUtils.mm2px(mContext, 10f), RoundTransformation.RoundType.ALL)
                        )
                        .placeholder(R.drawable.img_loading_placeholder)
                        .noFade()
                        .error(R.drawable.img_loading_placeholder)
                        .into(ivThumb)
                } else {
                    ivThumb.setImageResource(R.drawable.img_loading_placeholder)
                }

                if (vodInfo!!.seriesMap != null && vodInfo!!.seriesMap.size > 0) {
                    mGridViewFlag.visibility = View.VISIBLE
                    mGridView.visibility = View.VISIBLE
                    tvPlay.visibility = View.VISIBLE
                    mEmptyPlayList.visibility = View.GONE

                    val vodInfoRecord = RoomDataManger.getVodInfo(sourceKey!!, vodId!!)
                    if (vodInfoRecord != null) {
                        vodInfo!!.playIndex = Math.max(vodInfoRecord.playIndex, 0)
                        vodInfo!!.playFlag = vodInfoRecord.playFlag
                        vodInfo!!.playerCfg = vodInfoRecord.playerCfg
                        vodInfo!!.reverseSort = vodInfoRecord.reverseSort
                    } else {
                        vodInfo!!.playIndex = 0
                        vodInfo!!.playFlag = null
                        vodInfo!!.playerCfg = ""
                        vodInfo!!.reverseSort = false
                    }

                    if (vodInfo!!.reverseSort) {
                        vodInfo!!.reverse()
                    }

                    if (vodInfo!!.playFlag == null || !vodInfo!!.seriesMap.containsKey(vodInfo!!.playFlag))
                        vodInfo!!.playFlag = vodInfo!!.seriesMap.keys.toTypedArray()[0]

                    var flagScrollTo = 0
                    for (j in 0 until vodInfo!!.seriesFlags.size) {
                        val flag = vodInfo!!.seriesFlags[j]
                        if (flag.name == vodInfo!!.playFlag) {
                            flagScrollTo = j
                            flag.selected = true
                        } else
                            flag.selected = false
                    }
                    setTextShow(tvPlayUrl, "播放地址：", vodInfo!!.seriesMap[vodInfo!!.playFlag]!![0].url)
                    seriesFlagAdapter.setNewData(vodInfo!!.seriesFlags)
                    mGridViewFlag.scrollToPosition(flagScrollTo)

                    refreshList()
                    if (showPreview) {
                        jumpToPlay()
                        llPlayerFragmentContainer.visibility = View.VISIBLE
                        llPlayerFragmentContainerBlock.visibility = View.VISIBLE
                        toggleSubtitleTextSize()
                    }
                } else {
                    mGridViewFlag.visibility = View.GONE
                    mGridView.visibility = View.GONE
                    tvSeriesGroup.visibility = View.GONE
                    tvPlay.visibility = View.GONE
                    mEmptyPlayList.visibility = View.VISIBLE
                }
            } else {
                showEmpty()
                llPlayerFragmentContainer.visibility = View.GONE
                llPlayerFragmentContainerBlock.visibility = View.GONE
            }
        }
    }

    private fun getHtml(label: String, content: String?): String {
        val safeContent = content ?: ""
        return "$label<font color=\"#FFFFFF\">$safeContent</font>"
    }

    private var vod_picture = ""
    private fun initData() {
        val intent = intent
        if (intent != null && intent.extras != null) {
            val bundle = intent.extras!!
            vod_picture = bundle.getString("picture", "")
            loadDetail(bundle.getString("id", null), bundle.getString("sourceKey", ""))
        }
    }

    private fun loadDetail(vid: String?, key: String) {
        if (vid != null) {
            vodId = vid
            sourceKey = key
            firstsourceKey = key
            showLoading()
            sourceViewModel.getDetail(sourceKey, vodId)
            val isVodCollect = RoomDataManger.isVodCollect(sourceKey!!, vodId)
            if (isVodCollect) {
                tvCollect.text = "取消收藏"
            } else {
                tvCollect.text = "加入收藏"
            }
        }
    }

    private var isFirstLoad = true
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refresh(event: RefreshEvent) {
        if (event.type == RefreshEvent.TYPE_REFRESH) {
            if (event.obj != null) {
                if (event.obj is Int) {
                    val index = event.obj as Int
                    for (j in 0 until vodInfo!!.seriesMap[vodInfo!!.playFlag]!!.size) {
                        seriesAdapter.data[j].selected = false
                        seriesAdapter.notifyItemChanged(j)
                    }
                    seriesAdapter.data[index].selected = true
                    seriesAdapter.notifyItemChanged(index)
                    if (!isFirstLoad) mGridView.setSelection(index)
                    vodInfo!!.playIndex = index
                    insertVod(firstsourceKey!!, vodInfo!!)
                    isFirstLoad = false
                } else if (event.obj is JSONObject) {
                    vodInfo!!.playerCfg = event.obj.toString()
                    insertVod(firstsourceKey!!, vodInfo!!)
                } else if (event.obj is String) {
                    val url = event.obj as String
                    setTvPlayUrl(url)
                }
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_SELECT) {
            if (event.obj != null) {
                val video = event.obj as Movie.Video
                loadDetail(video.id, video.sourceKey)
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_WORD_CHANGE) {
            if (event.obj != null) {
                val word = event.obj as String
                switchSearchWord(word)
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_RESULT) {
            try {
                searchData(if (event.obj == null) null else event.obj as AbsXml)
            } catch (e: Exception) {
                searchData(null)
            }
        }
    }

    private var searchTitle = ""
    private var hadQuickStart = false
    private val quickSearchData = ArrayList<Movie.Video>()
    private val quickSearchWord = ArrayList<String>()
    private var searchExecutorService: ExecutorService? = null

    private fun switchSearchWord(word: String) {
        OkGo.getInstance().cancelTag("quick_search")
        quickSearchData.clear()
        searchTitle = word
        searchResult()
    }

    private fun startQuickSearch() {
        initCheckedSourcesForSearch()
        if (hadQuickStart) return
        hadQuickStart = true
        OkGo.getInstance().cancelTag("quick_search")
        quickSearchWord.clear()
        searchTitle = mVideo!!.name
        quickSearchData.clear()
        quickSearchWord.addAll(SearchHelper.splitWords(searchTitle))
        searchResult()
    }

    private fun searchResult() {
        try {
            if (searchExecutorService != null) {
                searchExecutorService!!.shutdownNow()
                searchExecutorService = null
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
        searchExecutorService = Executors.newFixedThreadPool(5)
        val searchRequestList = ArrayList<SourceBean>()
        searchRequestList.addAll(ApiConfig.get().sourceBeanList)
        val home = ApiConfig.get().homeSourceBean
        searchRequestList.remove(home)
        searchRequestList.add(0, home)

        val siteKey = ArrayList<String>()
        for (bean in searchRequestList) {
            if (!bean.isSearchable || !bean.isQuickSearch) {
                continue
            }
            if (mCheckSources != null && !mCheckSources!!.containsKey(bean.key)) {
                continue
            }
            siteKey.add(bean.key)
        }
        for (key in siteKey) {
            searchExecutorService!!.execute {
                sourceViewModel.getQuickSearch(key, searchTitle)
            }
        }
    }

    private fun searchData(absXml: AbsXml?) {
        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size > 0) {
            val data = ArrayList<Movie.Video>()
            for (video in absXml.movie.videoList) {
                if (video.sourceKey == sourceKey && video.id == vodId) continue
                data.add(video)
            }
            quickSearchData.addAll(data)
            EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH, data))
        }
    }

    private fun insertVod(sourceKey: String, vodInfo: VodInfo) {
        try {
            vodInfo.playNote = vodInfo.seriesMap[vodInfo.playFlag]!![vodInfo.playIndex].name
        } catch (th: Throwable) {
            vodInfo.playNote = ""
        }
        RoomDataManger.insertVodRecord(sourceKey, vodInfo)
        EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH))
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (searchExecutorService != null) {
                searchExecutorService!!.shutdownNow()
                searchExecutorService = null
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
        OkGo.getInstance().cancelTag("fenci")
        OkGo.getInstance().cancelTag("detail")
        OkGo.getInstance().cancelTag("quick_search")
        EventBus.getDefault().unregister(this)
    }

    override fun onBackPressed() {
        if (fullWindows) {
            if (playFragment?.onBackPressed() == true) return
            toggleFullPreview()
            val list = vodInfo!!.seriesMap[vodInfo!!.playFlag]
            tvSeriesGroup.visibility = if (list!!.size > 1) View.VISIBLE else View.GONE
            mGridView.requestFocus()
            return
        }
        if (seriesSelect) {
            if (seriesFlagFocus != null && !seriesFlagFocus!!.isFocused) {
                seriesFlagFocus!!.requestFocus()
                return
            }
        }
        if (showPreview && playFragment != null) playFragment!!.setPlayTitle(false)
        super.onBackPressed()
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && playFragment != null && fullWindows) {
            if (playFragment!!.dispatchKeyEvent(event)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && playFragment != null && fullWindows) {
            if (playFragment!!.onKeyDown(keyCode, event)) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && playFragment != null && fullWindows) {
            if (playFragment!!.onKeyUp(keyCode, event)) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private var previewVodInfo: VodInfo? = null
    private var fullWindows = false
    private var windowsPreview: ViewGroup.LayoutParams? = null
    private var windowsFull: ViewGroup.LayoutParams? = null

    private fun toggleFullPreview() {
        if (windowsPreview == null) {
            windowsPreview = llPlayerFragmentContainer.layoutParams
        }
        if (windowsFull == null) {
            windowsFull = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        fullWindows = !fullWindows
        llPlayerFragmentContainer.layoutParams = if (fullWindows) windowsFull!! else windowsPreview!!
        llPlayerFragmentContainerBlock.visibility = if (fullWindows) View.GONE else View.VISIBLE
        mGridView.visibility = if (fullWindows) View.GONE else View.VISIBLE
        mGridViewFlag.visibility = if (fullWindows) View.GONE else View.VISIBLE
        tvSeriesGroup.visibility = if (fullWindows) View.GONE else View.VISIBLE
        toggleSubtitleTextSize()
    }

    private fun toggleSubtitleTextSize() {
        var subtitleTextSize = SubtitleHelper.getTextSize(this)
        if (!fullWindows) {
            subtitleTextSize *= 0.6
        }
        EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SUBTITLE_SIZE_CHANGE, subtitleTextSize))
    }

    private fun setTvPlayUrl(url: String) {
        setTextShow(tvPlayUrl, "播放地址：", url)
    }
}
