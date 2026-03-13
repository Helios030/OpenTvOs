package con.open.tvos.ui.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.IntEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.BounceInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.BaseActivity
import con.open.tvos.base.BaseLazyFragment
import con.open.tvos.bean.AbsSortXml
import con.open.tvos.bean.MovieSort
import con.open.tvos.bean.SourceBean
import con.open.tvos.event.RefreshEvent
import con.open.tvos.server.ControlManager
import con.open.tvos.ui.adapter.HomePageAdapter
import con.open.tvos.ui.adapter.SelectDialogAdapter
import con.open.tvos.ui.adapter.SortAdapter
import con.open.tvos.ui.dialog.SelectDialog
import con.open.tvos.ui.dialog.TipDialog
import con.open.tvos.ui.fragment.GridFragment
import con.open.tvos.ui.fragment.UserFragment
import con.open.tvos.ui.tv.widget.DefaultTransformer
import con.open.tvos.ui.tv.widget.FixedSpeedScroller
import con.open.tvos.ui.tv.widget.NoScrollViewPager
import con.open.tvos.ui.tv.widget.ViewObj
import con.open.tvos.util.AppManager
import con.open.tvos.util.DefaultConfig
import con.open.tvos.util.FastClickCheckUtil
import con.open.tvos.util.FileUtils
import con.open.tvos.util.HawkConfig
import con.open.tvos.util.LOG
import con.open.tvos.util.MD5
import con.open.tvos.viewmodel.SourceViewModel
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import me.jessyan.autosize.utils.AutoSizeUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Date

class HomeActivity : BaseActivity() {
    private lateinit var topLayout: LinearLayout
    private lateinit var contentLayout: LinearLayout
    private lateinit var tvDate: TextView
    private lateinit var tvName: TextView
    private lateinit var mGridView: TvRecyclerView
    private lateinit var mViewPager: NoScrollViewPager
    private lateinit var sourceViewModel: SourceViewModel
    private lateinit var sortAdapter: SortAdapter
    private lateinit var pageAdapter: HomePageAdapter
    
    private var currentView: View? = null
    private val fragments: MutableList<BaseLazyFragment> = ArrayList()
    private var isDownOrUp = false
    private var sortChange = false
    private var currentSelected = 0
    private var sortFocused = 0
    var sortFocusView: View? = null
    private val mHandler = Handler(Looper.getMainLooper())
    private var mExitTime: Long = 0
    
    private val mRunnable: Runnable = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            val date = Date()
            val timeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")
            tvDate.text = timeFormat.format(date)
            mHandler.postDelayed(this, 1000)
        }
    }

    override fun getLayoutResID(): Int = R.layout.activity_home

    private var useCacheConfig = false

    override fun init() {
        EventBus.getDefault().register(this)
        ControlManager.get().startServer()
        initView()
        initViewModel()
        useCacheConfig = false
        val intent = intent
        if (intent != null && intent.extras != null) {
            val bundle = intent.extras!!
            useCacheConfig = bundle.getBoolean("useCache", false)
        }
        initData()
    }

    private fun initView() {
        this.topLayout = findViewById(R.id.topLayout)
        this.tvDate = findViewById(R.id.tvDate)
        this.tvName = findViewById(R.id.tvName)
        this.contentLayout = findViewById(R.id.contentLayout)
        this.mGridView = findViewById(R.id.mGridView)
        this.mViewPager = findViewById(R.id.mViewPager)
        this.sortAdapter = SortAdapter()
        this.mGridView.layoutManager = V7LinearLayoutManager(this.mContext, 0, false)
        this.mGridView.setSpacingWithMargins(0, AutoSizeUtils.dp2px(this.mContext, 10.0f))
        this.mGridView.adapter = this.sortAdapter
        sortAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                mGridView.post {
                    val firstChild = mGridView.layoutManager?.findViewByPosition(0)
                    if (firstChild != null) {
                        mGridView.setSelectedPosition(0)
                        firstChild.requestFocus()
                    }
                }
            }
        })
        this.mGridView.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(tvRecyclerView: TvRecyclerView, view: View, position: Int) {
                if (view != null && !this@HomeActivity.isDownOrUp) {
                    mHandler.postDelayed({
                        val textView: TextView = view.findViewById(R.id.tvTitle)
                        textView.paint.isFakeBoldText = false
                        if (sortFocused == position) {
                            view.animate().scaleX(1.1f).scaleY(1.1f)
                                .setInterpolator(BounceInterpolator())
                                .setDuration(300).start()
                            textView.setTextColor(this@HomeActivity.resources.getColor(R.color.color_FFFFFF))
                        } else {
                            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start()
                            textView.setTextColor(this@HomeActivity.resources.getColor(R.color.color_BBFFFFFF))
                            view.findViewById<View>(R.id.tvFilter).visibility = View.GONE
                            view.findViewById<View>(R.id.tvFilterColor).visibility = View.GONE
                        }
                        textView.invalidate()
                    }, 10)
                }
            }

            override fun onItemSelected(tvRecyclerView: TvRecyclerView, view: View, position: Int) {
                if (view != null) {
                    this@HomeActivity.currentView = view
                    this@HomeActivity.isDownOrUp = false
                    this@HomeActivity.sortChange = true
                    view.animate().scaleX(1.1f).scaleY(1.1f)
                        .setInterpolator(BounceInterpolator())
                        .setDuration(300).start()
                    val textView: TextView = view.findViewById(R.id.tvTitle)
                    textView.paint.isFakeBoldText = true
                    textView.setTextColor(this@HomeActivity.resources.getColor(R.color.color_FFFFFF))
                    textView.invalidate()
                    val sortData = sortAdapter.getItem(position)
                    if (sortData.filters.isNotEmpty()) {
                        showFilterIcon(sortData.filterSelectCount())
                    }
                    this@HomeActivity.sortFocusView = view
                    this@HomeActivity.sortFocused = position
                    mHandler.removeCallbacks(mDataRunnable)
                    mHandler.postDelayed(mDataRunnable, 200)
                }
            }

            override fun onItemClick(parent: TvRecyclerView, itemView: View, position: Int) {
                if (itemView != null && currentSelected == position) {
                    val baseLazyFragment = fragments[currentSelected]
                    if (baseLazyFragment is GridFragment && sortAdapter.getItem(position).filters.isNotEmpty()) {
                        baseLazyFragment.showFilter()
                    } else if (baseLazyFragment is UserFragment) {
                        showSiteSwitch()
                    }
                }
            }
        })

        this.mGridView.setOnInBorderKeyEventListener { direction, view ->
            if (direction == View.FOCUS_UP) {
                val baseLazyFragment = fragments[sortFocused]
                if (baseLazyFragment is GridFragment) {
                    baseLazyFragment.forceRefresh()
                }
            }
            if (direction != View.FOCUS_DOWN) {
                return@setOnInBorderKeyEventListener false
            }
            val baseLazyFragment = fragments[sortFocused]
            if (baseLazyFragment !is GridFragment) {
                return@setOnInBorderKeyEventListener false
            }
            !baseLazyFragment.isLoad
        }

        tvName.setOnClickListener { v ->
            FastClickCheckUtil.check(v)
            if (dataInitOk && jarInitOk) {
                val cspCachePath = FileUtils.getFilePath() + "/csp/"
                val jar = ApiConfig.get().homeSourceBean?.jar ?: ""
                val jarUrl = if (jar.isNotEmpty()) jar else ApiConfig.get().spider
                val cspCacheDir = File(cspCachePath + MD5.string2MD5(jarUrl) + ".jar")
                Toast.makeText(mContext, "jar缓存已清除", Toast.LENGTH_LONG).show()
                if (!cspCacheDir.exists()) {
                    refreshHome()
                    return@setOnClickListener
                }
                Thread {
                    try {
                        FileUtils.deleteFile(cspCacheDir)
                        ApiConfig.get().clearJarLoader()
                        refreshHome()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            } else {
                jumpActivity(SettingActivity::class.java)
            }
        }
        
        tvName.setOnLongClickListener {
            jumpActivity(SettingActivity::class.java)
            true
        }
        
        setLoadSir(this.contentLayout)
    }

    private var skipNextUpdate = false

    private fun initViewModel() {
        sourceViewModel = ViewModelProvider(this)[SourceViewModel::class.java]
        sourceViewModel.sortResult.observe(this) { absXml ->
            if (skipNextUpdate) {
                skipNextUpdate = false
                return@observe
            }
            showSuccess()
            if (absXml != null && absXml.classes != null && absXml.classes!!.sortList != null) {
                sortAdapter.setNewData(
                    DefaultConfig.adjustSort(
                        ApiConfig.get().homeSourceBean?.key,
                        absXml.classes!!.sortList!!,
                        true
                    )
                )
            } else {
                sortAdapter.setNewData(
                    DefaultConfig.adjustSort(
                        ApiConfig.get().homeSourceBean?.key,
                        ArrayList(),
                        true
                    )
                )
            }
            initViewPager(absXml)
            val home = ApiConfig.get().homeSourceBean
            if (home != null && home.name != null && home.name!!.isNotEmpty()) {
                tvName.text = home.name
            }
            tvName.clearAnimation()
        }
    }

    private var dataInitOk = false
    private var jarInitOk = false

    private fun initData() {
        if (dataInitOk && jarInitOk) {
            sourceViewModel.getSort(ApiConfig.get().homeSourceBean?.key)
            if (hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                LOG.e("有")
            } else {
                LOG.e("无")
            }
            if (!useCacheConfig && Hawk.get(HawkConfig.DEFAULT_LOAD_LIVE, false)) {
                jumpActivity(LivePlayActivity::class.java)
            }
            return
        }
        tvNameAnimation()
        showLoading()
        if (dataInitOk && !jarInitOk) {
            if (ApiConfig.get().spider.isNotEmpty()) {
                ApiConfig.get().loadJar(useCacheConfig, ApiConfig.get().spider, object : ApiConfig.LoadConfigCallback {
                    override fun success() {
                        jarInitOk = true
                        mHandler.postDelayed({
                            initData()
                        }, 50)
                    }

                    override fun notice(msg: String) {
                        mHandler.post {
                            Toast.makeText(this@HomeActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun error(msg: String) {
                        jarInitOk = true
                        dataInitOk = true
                        mHandler.postDelayed({
                            Toast.makeText(this@HomeActivity, "$msg; 尝试加载最近一次的jar", Toast.LENGTH_SHORT).show()
                            initData()
                        }, 50)
                    }
                })
            }
            return
        }
        ApiConfig.get().loadConfig(useCacheConfig, object : ApiConfig.LoadConfigCallback {
            var dialog: TipDialog? = null

            override fun notice(msg: String) {
                mHandler.post {
                    Toast.makeText(this@HomeActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun success() {
                dataInitOk = true
                if (ApiConfig.get().spider.isEmpty()) {
                    jarInitOk = true
                }
                mHandler.postDelayed({
                    initData()
                }, 50)
            }

            override fun error(msg: String) {
                if (msg.equals("-1", ignoreCase = true)) {
                    mHandler.post {
                        dataInitOk = true
                        jarInitOk = true
                        initData()
                    }
                    return
                }
                mHandler.post {
                    if (dialog == null) {
                        dialog = TipDialog(
                            this@HomeActivity,
                            msg,
                            "重试",
                            "取消",
                            object : TipDialog.OnListener {
                                override fun left() {
                                    mHandler.post {
                                        initData()
                                        dialog?.hide()
                                    }
                                }

                                override fun right() {
                                    dataInitOk = true
                                    jarInitOk = true
                                    mHandler.post {
                                        initData()
                                        dialog?.hide()
                                    }
                                }

                                override fun cancel() {
                                    dataInitOk = true
                                    jarInitOk = true
                                    mHandler.post {
                                        initData()
                                        dialog?.hide()
                                    }
                                }
                            }
                        )
                    }
                    if (dialog?.isShowing != true) {
                        dialog?.show()
                    }
                }
            }
        }, this)
    }

    private fun initViewPager(absXml: AbsSortXml?) {
        if (sortAdapter.data.size > 0) {
            for (data in sortAdapter.data) {
                if (data.id == "my0") {
                    if (Hawk.get(HawkConfig.HOME_REC, 0) == 1 && absXml != null && absXml.videoList != null && absXml.videoList!!.size > 0) {
                        fragments.add(UserFragment.newInstance(absXml.videoList))
                    } else {
                        fragments.add(UserFragment.newInstance(null))
                    }
                } else {
                    fragments.add(GridFragment.newInstance(data))
                }
            }
            pageAdapter = HomePageAdapter(supportFragmentManager, fragments)
            try {
                val field: Field = ViewPager::class.java.getDeclaredField("mScroller")
                field.isAccessible = true
                val scroller = FixedSpeedScroller(mContext, AccelerateInterpolator())
                field.set(mViewPager, scroller)
                scroller.setmDuration(300)
            } catch (e: Exception) {
                // Ignore
            }
            mViewPager.setPageTransformer(true, DefaultTransformer())
            mViewPager.adapter = pageAdapter
            mViewPager.setCurrentItem(currentSelected, false)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBackPressed() {
        // 打断加载
        if (isLoading) {
            refreshEmpty()
            return
        }
        // 如果处于 VOD 删除模式，则退出该模式并刷新界面
        if (HawkConfig.hotVodDelete) {
            HawkConfig.hotVodDelete = false
            UserFragment.homeHotVodAdapter?.notifyDataSetChanged()
            return
        }

        // 检查 fragments 状态
        if (this.fragments.size <= 0 || this.sortFocused >= this.fragments.size || this.sortFocused < 0) {
            doExit()
            return
        }

        val baseLazyFragment = this.fragments[this.sortFocused]
        if (baseLazyFragment is GridFragment) {
            val grid = baseLazyFragment
            // 如果当前 Fragment 能恢复之前保存的 UI 状态，则直接返回
            if (grid.restoreView()) {
                return
            }
            // 如果 sortFocusView 存在且没有获取焦点，则请求焦点
            if (this.sortFocusView != null && !this.sortFocusView!!.isFocused) {
                this.sortFocusView!!.requestFocus()
            }
            // 如果当前不是第一个界面，则将列表设置到第一项
            else if (this.sortFocused != 0) {
                this.mGridView.setSelection(0)
            } else {
                doExit()
            }
        } else if (baseLazyFragment is UserFragment && UserFragment.tvHotList.canScrollVertically(-1)) {
            // 如果 UserFragment 列表可以向上滚动，则滚动到顶部
            UserFragment.tvHotList.scrollToPosition(0)
            this.mGridView.setSelection(0)
        } else {
            doExit()
        }
    }

    private fun doExit() {
        // 如果两次返回间隔小于 2000 毫秒，则退出应用
        if (System.currentTimeMillis() - mExitTime < 2000) {
            AppManager.getInstance().finishAllActivity()
            EventBus.getDefault().unregister(this)
            ControlManager.get().stopServer()
            finish()
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        } else {
            // 否则仅提示用户，再按一次退出应用
            mExitTime = System.currentTimeMillis()
            Toast.makeText(mContext, "再按一次返回键退出应用", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        mHandler.post(mRunnable)
    }

    override fun onPause() {
        super.onPause()
        mHandler.removeCallbacksAndMessages(null)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refresh(event: RefreshEvent) {
        if (event.type == RefreshEvent.TYPE_PUSH_URL) {
            if (ApiConfig.get().getSource("push_agent") != null) {
                val newIntent = Intent(mContext, DetailActivity::class.java)
                newIntent.putExtra("id", event.obj as String)
                newIntent.putExtra("sourceKey", "push_agent")
                newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                this@HomeActivity.startActivity(newIntent)
            }
        } else if (event.type == RefreshEvent.TYPE_FILTER_CHANGE) {
            if (currentView != null) {
                showFilterIcon(event.obj as Int)
            }
        }
    }

    private fun showFilterIcon(count: Int) {
        val visible = count > 0
        currentView?.findViewById<View>(R.id.tvFilterColor)?.visibility = if (visible) View.VISIBLE else View.GONE
        currentView?.findViewById<View>(R.id.tvFilter)?.visibility = if (visible) View.GONE else View.VISIBLE
    }

    private val mDataRunnable: Runnable = object : Runnable {
        override fun run() {
            if (sortChange) {
                sortChange = false
                if (sortFocused != currentSelected) {
                    currentSelected = sortFocused
                    mViewPager.setCurrentItem(sortFocused, false)
                    changeTop(sortFocused != 0)
                }
            }
        }
    }

    private var menuKeyDownTime: Long = 0
    private val longPressThreshold: Long = 2000 // 设置长按的阈值，单位是毫秒

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (topHide < 0) return false
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                menuKeyDownTime = System.currentTimeMillis()
            } else if (event.action == KeyEvent.ACTION_UP) {
                val pressDuration = System.currentTimeMillis() - menuKeyDownTime
                if (pressDuration >= longPressThreshold) {
                    jumpActivity(SettingActivity::class.java)
                } else {
                    showSiteSwitch()
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private var topHide: Byte = 0

    private fun changeTop(hide: Boolean) {
        val viewObj = ViewObj(topLayout, topLayout.layoutParams as ViewGroup.MarginLayoutParams)
        val animatorSet = AnimatorSet()
        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                topHide = if (hide) 1 else 0
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        if (hide && topHide.toInt() == 0) {
            animatorSet.playTogether(
                ObjectAnimator.ofObject(
                    viewObj, "marginTop", IntEvaluator(),
                    AutoSizeUtils.mm2px(this.mContext, 10.0f),
                    AutoSizeUtils.mm2px(this.mContext, 0.0f)
                ),
                ObjectAnimator.ofObject(
                    viewObj, "height", IntEvaluator(),
                    AutoSizeUtils.mm2px(this.mContext, 50.0f),
                    AutoSizeUtils.mm2px(this.mContext, 1.0f)
                ),
                ObjectAnimator.ofFloat(this.topLayout, "alpha", 1.0f, 0.0f)
            )
            animatorSet.duration = 200
            animatorSet.start()
            return
        }
        if (!hide && topHide.toInt() == 1) {
            animatorSet.playTogether(
                ObjectAnimator.ofObject(
                    viewObj, "marginTop", IntEvaluator(),
                    AutoSizeUtils.mm2px(this.mContext, 0.0f),
                    AutoSizeUtils.mm2px(this.mContext, 10.0f)
                ),
                ObjectAnimator.ofObject(
                    viewObj, "height", IntEvaluator(),
                    AutoSizeUtils.mm2px(this.mContext, 1.0f),
                    AutoSizeUtils.mm2px(this.mContext, 50.0f)
                ),
                ObjectAnimator.ofFloat(this.topLayout, "alpha", 0.0f, 1.0f)
            )
            animatorSet.duration = 200
            animatorSet.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        AppManager.getInstance().appExit(0)
        ControlManager.get().stopServer()
    }

    private var mSiteSwitchDialog: SelectDialog<SourceBean>? = null

    private fun showSiteSwitch() {
        val sites = ApiConfig.get().switchSourceBeanList
        if (sites.isEmpty()) return
        var select = sites.indexOf(ApiConfig.get().homeSourceBean)
        if (select < 0 || select >= sites.size) select = 0
        if (mSiteSwitchDialog == null) {
            mSiteSwitchDialog = SelectDialog(this)
            val tvRecyclerView: TvRecyclerView = mSiteSwitchDialog!!.findViewById(R.id.list)
            // 根据 sites 数量动态计算列数
            var spanCount = Math.floor(sites.size / 20.0).toInt()
            spanCount = Math.min(spanCount, 2)
            tvRecyclerView.layoutManager = V7GridLayoutManager(mSiteSwitchDialog!!.context, spanCount + 1)
            // 设置对话框宽度
            val clRoot: ConstraintLayout = mSiteSwitchDialog!!.findViewById(R.id.cl_root)
            val clp = clRoot.layoutParams
            clp.width = AutoSizeUtils.mm2px(mSiteSwitchDialog!!.context, (380 + 200 * spanCount).toFloat())
            mSiteSwitchDialog!!.setTip("请选择首页数据源")
        }
        mSiteSwitchDialog!!.setAdapter(
            object : SelectDialogAdapter.SelectDialogInterface<SourceBean> {
                override fun click(value: SourceBean, pos: Int) {
                    ApiConfig.get().setSourceBean(value)
                    refreshHome()
                }

                override fun getDisplay(`val`: SourceBean): String {
                    return `val`.name ?: ""
                }
            },
            object : DiffUtil.ItemCallback<SourceBean>() {
                override fun areItemsTheSame(oldItem: SourceBean, newItem: SourceBean): Boolean {
                    return oldItem === newItem
                }

                override fun areContentsTheSame(oldItem: SourceBean, newItem: SourceBean): Boolean {
                    return oldItem.key == newItem.key
                }
            },
            sites,
            select
        )
        mSiteSwitchDialog!!.show()
    }

    private fun refreshHome() {
        val intent = Intent(applicationContext, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        val bundle = Bundle()
        bundle.putBoolean("useCache", true)
        intent.putExtras(bundle)
        this@HomeActivity.startActivity(intent)
    }

    private fun refreshEmpty() {
        skipNextUpdate = true
        showSuccess()
        sortAdapter.setNewData(
            DefaultConfig.adjustSort(
                ApiConfig.get().homeSourceBean?.key,
                ArrayList(),
                true
            )
        )
        initViewPager(null)
        tvName.clearAnimation()
    }

    private fun tvNameAnimation() {
        val blinkAnimation = AlphaAnimation(0.0f, 1.0f)
        blinkAnimation.duration = 500
        blinkAnimation.startOffset = 20
        blinkAnimation.repeatMode = Animation.REVERSE
        blinkAnimation.repeatCount = Animation.INFINITE
        tvName.startAnimation(blinkAnimation)
    }
}
