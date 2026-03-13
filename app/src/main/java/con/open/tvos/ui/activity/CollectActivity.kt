package con.open.tvos.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import con.open.tvos.R
import con.open.tvos.api.ApiConfig
import con.open.tvos.base.BaseActivity
import con.open.tvos.cache.RoomDataManger
import con.open.tvos.cache.VodCollect
import con.open.tvos.event.RefreshEvent
import con.open.tvos.ui.adapter.CollectAdapter
import con.open.tvos.ui.dialog.ConfirmClearDialog
import con.open.tvos.util.FastClickCheckUtil
import con.open.tvos.util.HawkConfig
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class CollectActivity : BaseActivity() {
    
    private lateinit var tvDelete: ImageView
    private lateinit var tvClear: ImageView
    private lateinit var tvDelTip: TextView
    private lateinit var mGridView: TvRecyclerView
    
    var delMode = false
        private set
    
    override fun getLayoutResID(): Int = R.layout.activity_collect

    override fun init() {
        initView()
        initData()
    }

    private fun toggleDelMode() {
        HawkConfig.hotVodDelete = !HawkConfig.hotVodDelete
        collectAdapter.notifyDataSetChanged()
        delMode = !delMode
        tvDelTip.visibility = if (delMode) View.VISIBLE else View.GONE
    }

    private fun initView() {
        EventBus.getDefault().register(this)
        
        tvDelete = findViewById(R.id.tvDelete)
        tvClear = findViewById(R.id.tvClear)
        tvDelTip = findViewById(R.id.tvDelTip)
        mGridView = findViewById(R.id.mGridView)
        
        mGridView.setHasFixedSize(true)
        mGridView.setLayoutManager(V7GridLayoutManager(mContext, if (isBaseOnWidth) 5 else 6))
        
        collectAdapter = CollectAdapter()
        mGridView.adapter = collectAdapter
        
        tvDelete.setOnClickListener {
            toggleDelMode()
        }
        
        tvClear.setOnClickListener {
            val dialog = ConfirmClearDialog(mContext, "Collect")
            dialog.show()
        }
        
        mGridView.setOnInBorderKeyEventListener { direction, _ ->
            if (direction == View.FOCUS_UP) {
                tvDelete.isFocusable = true
                tvClear.isFocusable = true
                tvDelete.requestFocus()
            }
            false
        }
        
        mGridView.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView, itemView: View, position: Int) {
                itemView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(300)
                    .setInterpolator(BounceInterpolator())
                    .start()
            }

            override fun onItemSelected(parent: TvRecyclerView, itemView: View, position: Int) {
                itemView.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(300)
                    .setInterpolator(BounceInterpolator())
                    .start()
            }

            override fun onItemClick(parent: TvRecyclerView, itemView: View, position: Int) {
                // No action on item click in TvRecyclerView
            }
        })
        
        collectAdapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            val vodInfo = collectAdapter.data[position]
            vodInfo?.let {
                if (delMode) {
                    collectAdapter.remove(position)
                    RoomDataManger.deleteVodCollect(it.id)
                } else {
                    if (ApiConfig.get().getSource(it.sourceKey) != null) {
                        val bundle = Bundle().apply {
                            putString("id", it.vodId)
                            putString("sourceKey", it.sourceKey)
                            putString("picture", it.pic)
                        }
                        jumpActivity(DetailActivity::class.java, bundle)
                    } else {
                        val newIntent = Intent(mContext, SearchActivity::class.java).apply {
                            putExtra("title", it.name)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(newIntent)
                    }
                }
            }
        }
        
        collectAdapter.onItemLongClickListener = BaseQuickAdapter.OnItemLongClickListener { _, _, _ ->
            tvDelete.isFocusable = true
            toggleDelMode()
            true
        }
    }

    private fun initData() {
        val allVodRecord = RoomDataManger.getAllVodCollect()
        val vodInfoList = ArrayList<VodCollect>()
        for (vodInfo in allVodRecord) {
            vodInfoList.add(vodInfo)
        }
        collectAdapter.setNewData(vodInfoList)
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
        lateinit var collectAdapter: CollectAdapter
    }
}
