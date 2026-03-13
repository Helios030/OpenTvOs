package con.open.tvos.ui.activity

import android.os.Bundle
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.BaseActivity
import con.open.tvos.bean.VodInfo
import con.open.tvos.cache.RoomDataManger
import con.open.tvos.event.RefreshEvent
import con.open.tvos.ui.adapter.HistoryAdapter
import con.open.tvos.ui.dialog.ConfirmClearDialog
import con.open.tvos.util.FastClickCheckUtil
import con.open.tvos.util.HawkConfig
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * @author pj567
 * @date :2021/1/7
 * @description:
 */
class HistoryActivity : BaseActivity() {
    private lateinit var tvDelete: ImageView
    private lateinit var tvClear: ImageView
    private lateinit var tvDelTip: TextView
    private lateinit var mGridView: TvRecyclerView
    
    private var delMode = false

    override fun getLayoutResID(): Int = R.layout.activity_history

    override fun init() {
        initView()
        initData()
    }

    private fun toggleDelMode() {
        HawkConfig.hotVodDelete = !HawkConfig.hotVodDelete
        historyAdapter.notifyDataSetChanged()
        delMode = !delMode
        tvDelTip.visibility = if (delMode) View.VISIBLE else View.GONE
    }

    private fun initView() {
        EventBus.getDefault().register(this)
        
        tvDelete = findViewById(R.id.tvDelete)
        tvClear = findViewById(R.id.tvClear)
        tvDelTip = findViewById(R.id.tvDelTip)
        mGridView = findViewById<TvRecyclerView>(R.id.mGridView).apply {
            setHasFixedSize(true)
            layoutManager = V7GridLayoutManager(this@HistoryActivity.mContext, if (isBaseOnWidth) 5 else 6)
            adapter = historyAdapter
            
            setOnInBorderKeyEventListener { direction, _ ->
                if (direction == View.FOCUS_UP) {
                    tvDelete.isFocusable = true
                    tvClear.isFocusable = true
                    tvDelete.requestFocus()
                }
                false
            }
            
            setOnItemListener(object : TvRecyclerView.OnItemListener {
                override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View, position: Int) {
                    itemView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(300)
                        .setInterpolator(BounceInterpolator())
                        .start()
                }

                override fun onItemSelected(parent: TvRecyclerView?, itemView: View, position: Int) {
                    itemView.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(300)
                        .setInterpolator(BounceInterpolator())
                        .start()
                }

                override fun onItemClick(parent: TvRecyclerView?, itemView: View, position: Int) {
                    // No action on item click
                }
            })
        }
        
        tvDelete.setOnClickListener {
            toggleDelMode()
        }
        
        tvClear.setOnClickListener {
            ConfirmClearDialog(mContext, "History").show()
        }
        
        historyAdapter.setOnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            if (position == -1) return@setOnItemClickListener
            
            val vodInfo = historyAdapter.getData().getOrNull(position) ?: return@setOnItemClickListener
            
            if (delMode) {
                historyAdapter.remove(position)
                RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo)
            } else {
                val bundle = Bundle().apply {
                    putString("id", vodInfo.id)
                    putString("sourceKey", vodInfo.sourceKey)
                }
                
                val sourceBean = ApiConfig.get().getSource(vodInfo.sourceKey)
                if (sourceBean != null) {
                    bundle.putString("picture", vodInfo.pic)
                    jumpActivity(DetailActivity::class.java, bundle)
                } else {
                    bundle.putString("title", vodInfo.name)
                    if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                        jumpActivity(FastSearchActivity::class.java, bundle)
                    } else {
                        jumpActivity(SearchActivity::class.java, bundle)
                    }
                }
            }
        }
        
        historyAdapter.setOnItemLongClickListener { _, _, _ ->
            tvDelete.isFocusable = true
            toggleDelMode()
            true
        }
    }

    private fun initData() {
        val allVodRecord = RoomDataManger.getAllVodRecord(100)
        val vodInfoList = allVodRecord.map { vodInfo ->
            vodInfo.apply {
                if (!playNote.isNullOrEmpty()) {
                    note = "上次看到$playNote"
                }
            }
        }
        historyAdapter.setNewData(vodInfoList)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refresh(event: RefreshEvent) {
        if (event.type == RefreshEvent.TYPE_HISTORY_REFRESH) {
            initData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onBackPressed() {
        if (delMode) {
            toggleDelMode()
            return
        }
        super.onBackPressed()
    }

    companion object {
        val historyAdapter: HistoryAdapter by lazy { HistoryAdapter() }
    }
}
