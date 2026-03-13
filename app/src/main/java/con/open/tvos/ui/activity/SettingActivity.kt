package con.open.tvos.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.chad.library.adapter.base.BaseQuickAdapter
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.BaseActivity
import con.open.tvos.base.BaseLazyFragment
import con.open.tvos.ui.adapter.SettingMenuAdapter
import con.open.tvos.ui.adapter.SettingPageAdapter
import con.open.tvos.ui.fragment.ModelSettingFragment
import con.open.tvos.util.AppManager
import con.open.tvos.util.HawkConfig
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
class SettingActivity : BaseActivity() {
    private lateinit var mGridView: TvRecyclerView
    private lateinit var mViewPager: ViewPager
    private lateinit var sortAdapter: SettingMenuAdapter
    private lateinit var pageAdapter: SettingPageAdapter
    private val fragments: MutableList<BaseLazyFragment> = ArrayList()
    private var sortChange = false
    private var defaultSelected = 0
    private var sortFocused = 0
    private val mHandler = Handler(Looper.getMainLooper())
    private var homeSourceKey: String? = null
    private var currentApi: String? = null
    private var homeRec = 0
    private var dnsOpt = 0
    private var currentLiveApi: String? = null

    private val mDataRunnable = Runnable {
        if (sortChange) {
            sortChange = false
            if (sortFocused != defaultSelected) {
                defaultSelected = sortFocused
                mViewPager.setCurrentItem(sortFocused, false)
            }
        }
    }

    private val mDevModeRun = Runnable {
        devMode = ""
    }

    interface DevModeCallback {
        fun onChange()
    }

    var devMode = ""

    override fun getLayoutResID(): Int = R.layout.activity_setting

    override fun init() {
        initView()
        initData()
    }

    private fun initView() {
        mGridView = findViewById(R.id.mGridView)
        mViewPager = findViewById(R.id.mViewPager)
        sortAdapter = SettingMenuAdapter()
        mGridView.adapter = sortAdapter
        mGridView.layoutManager = V7LinearLayoutManager(mContext, 1, false)
        
        sortAdapter.onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.tvName) {
                view.parent?.let { parent ->
                    (parent as ViewGroup).requestFocus()
                    sortFocused = position
                    if (sortFocused != defaultSelected) {
                        defaultSelected = sortFocused
                        mViewPager.setCurrentItem(sortFocused, false)
                    }
                }
            }
        }
        
        mGridView.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView, itemView: View?, position: Int) {
                itemView?.let {
                    val tvName = it.findViewById<TextView>(R.id.tvName)
                    tvName.setTextColor(resources.getColor(R.color.color_CCFFFFFF))
                }
            }

            override fun onItemSelected(parent: TvRecyclerView, itemView: View?, position: Int) {
                itemView?.let {
                    sortChange = true
                    sortFocused = position
                    val tvName = it.findViewById<TextView>(R.id.tvName)
                    tvName.setTextColor(Color.WHITE)
                }
            }

            override fun onItemClick(parent: TvRecyclerView, itemView: View?, position: Int) {
                // No action needed
            }
        })
    }

    private fun initData() {
        currentApi = Hawk.get(HawkConfig.API_URL, "")
        homeSourceKey = ApiConfig.get().homeSourceBean?.key
        homeRec = Hawk.get(HawkConfig.HOME_REC, 0)
        dnsOpt = Hawk.get(HawkConfig.DOH_URL, 0)
        currentLiveApi = Hawk.get(HawkConfig.LIVE_API_URL, "")
        val sortList = ArrayList<String>()
        sortList.add("设置其他")
        sortAdapter.setNewData(sortList)
        initViewPager()
    }

    private fun initViewPager() {
        fragments.add(ModelSettingFragment.newInstance())
        pageAdapter = SettingPageAdapter(supportFragmentManager, fragments)
        mViewPager.adapter = pageAdapter
        mViewPager.setCurrentItem(0)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            mHandler.removeCallbacks(mDataRunnable)
            val keyCode = event.keyCode
            when (keyCode) {
                KeyEvent.KEYCODE_0 -> {
                    mHandler.removeCallbacks(mDevModeRun)
                    devMode += "0"
                    mHandler.postDelayed(mDevModeRun, 200)
                    if (devMode.length >= 4) {
                        callback?.onChange()
                    }
                }
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            mHandler.postDelayed(mDataRunnable, 200)
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        val currentApiNow: String? = Hawk.get(HawkConfig.API_URL, "")
        val dnsOptNow: Int = Hawk.get(HawkConfig.DOH_URL, 0)
        val homeRecNow: Int = Hawk.get(HawkConfig.HOME_REC, 0)
        val homeApiNow: String? = Hawk.get(HawkConfig.HOME_API, "")
        val currentLiveApiNow: String? = Hawk.get(HawkConfig.LIVE_API_URL, "")
        
        if (currentApi == currentApiNow) {
            if (dnsOpt != dnsOptNow) {
                AppManager.getInstance().finishAllActivity()
                jumpActivity(HomeActivity::class.java)
            } else if ((homeSourceKey != null && homeSourceKey != homeApiNow) || homeRec != homeRecNow) {
                jumpActivity(HomeActivity::class.java, createBundle())
            } else if (currentLiveApi != currentLiveApiNow) {
                jumpActivity(HomeActivity::class.java)
            }
        } else {
            AppManager.getInstance().finishAllActivity()
            jumpActivity(HomeActivity::class.java)
        }
        super.onBackPressed()
    }

    private fun createBundle(): Bundle {
        return Bundle().apply {
            putBoolean("useCache", true)
        }
    }

    companion object {
        var callback: DevModeCallback? = null
    }
}
