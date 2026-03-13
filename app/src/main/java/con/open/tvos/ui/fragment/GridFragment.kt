package con.open.tvos.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import con.open.tvos.R
import con.open.tvos.bean.AbsXml
import con.open.tvos.bean.Movie
import con.open.tvos.bean.MovieSort
import con.open.tvos.event.RefreshEvent
import con.open.tvos.ui.activity.DetailActivity
import con.open.tvos.ui.activity.FastSearchActivity
import con.open.tvos.ui.activity.SearchActivity
import con.open.tvos.ui.adapter.GridAdapter
import con.open.tvos.ui.adapter.GridFilterKVAdapter
import con.open.tvos.ui.dialog.GridFilterDialog
import con.open.tvos.ui.tv.widget.LoadMoreView
import con.open.tvos.util.FastClickCheckUtil
import con.open.tvos.util.HawkConfig
import con.open.tvos.util.ImgUtil
import con.open.tvos.viewmodel.SourceViewModel
import org.greenrobot.eventbus.EventBus

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
class GridFragment : BaseLazyFragment() {

    private var sortData: MovieSort.SortData? = null
    private var mGridView: TvRecyclerView? = null
    private var sourceViewModel: SourceViewModel? = null
    private var gridFilterDialog: GridFilterDialog? = null
    private var gridAdapter: GridAdapter? = null
    private var page = 1
    private var maxPage = 1
    private var isLoad = false
    private var isTop = true
    private var focusedView: View? = null
    private var style: ImgUtil.Style? = null

    private class GridInfo {
        var sortID: String = ""
        var mGridView: TvRecyclerView? = null
        var gridAdapter: GridAdapter? = null
        var page = 1
        var maxPage = 1
        var isLoad = false
        var focusedView: View? = null
    }

    private val mGrids: Stack<GridInfo> = Stack() // ui栈

    companion object {
        fun newInstance(sortData: MovieSort.SortData): GridFragment {
            return GridFragment().apply {
                this.sortData = sortData
            }
        }
    }

    override fun getLayoutResID(): Int = R.layout.fragment_grid

    override fun init() {
        initView()
        initViewModel()
        initData()
    }

    private fun changeView(id: String, isFolder: Boolean) {
        sortData?.let { data ->
            data.flag = if (isFolder) {
                if (style == null) "1" else "2"
            } else {
                "2"
            }
        }
        initView()
        sortData?.id = id
        initViewModel()
        initData()
    }

    fun isFolederMode(): Boolean = getUITag() == '1'

    // 获取当前页面UI的显示模式 '0' 正常模式 '1' 文件夹模式 '2' 显示缩略图的文件夹模式
    fun getUITag(): Char {
        return sortData?.let { data ->
            if (data.flag.isNullOrEmpty() || style != null) '0' else data.flag[0]
        } ?: '0'
    }

    // 是否允许聚合搜索 sortData.flag的第二个字符为'1'时允许聚搜
    fun enableFastSearch(): Boolean {
        return sortData?.flag?.let { flag ->
            flag.length < 2 || flag[1] == '1'
        } ?: true
    }

    // 保存当前页面
    private fun saveCurrentView() {
        mGridView ?: return
        GridInfo().apply {
            sortData?.id?.let { sortID = it }
            mGridView = this@GridFragment.mGridView
            gridAdapter = this@GridFragment.gridAdapter
            page = this@GridFragment.page
            maxPage = this@GridFragment.maxPage
            isLoad = this@GridFragment.isLoad
            focusedView = this@GridFragment.focusedView
        }.also { mGrids.push(it) }
    }

    // 丢弃当前页面，将页面还原成上一个保存的页面
    fun restoreView(): Boolean {
        if (mGrids.empty()) return false
        showSuccess()
        mGridView?.parent?.let { parent ->
            (parent as? ViewGroup)?.removeView(mGridView)
        }
        val info = mGrids.pop()
        sortData?.id = info.sortID
        mGridView = info.mGridView
        gridAdapter = info.gridAdapter
        page = info.page
        maxPage = info.maxPage
        isLoad = info.isLoad
        focusedView = info.focusedView
        mGridView?.visibility = View.VISIBLE
        mGridView?.requestFocus()
        return true
    }

    // 更改当前页面
    private fun createView() {
        saveCurrentView()
        if (mGridView == null) {
            mGridView = findViewById(R.id.mGridView)
        } else {
            TvRecyclerView(mContext).apply {
                setSpacingWithMargins(10, 10)
                layoutParams = mGridView?.layoutParams
                mGridView?.let { v ->
                    setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, v.paddingBottom)
                    clipToPadding = v.clipToPadding
                }
                (mGridView?.parent as? ViewGroup)?.addView(this)
                mGridView?.visibility = View.GONE
                mGridView = this
                visibility = View.VISIBLE
            }
        }
        mGridView?.setHasFixedSize(true)
        style = ImgUtil.initStyle()
        gridAdapter = GridAdapter(isFolederMode(), style)
        page = 1
        maxPage = 1
        isLoad = false
    }

    private fun initView() {
        createView()
        mGridView?.adapter = gridAdapter
        mGridView?.let { gridView ->
            if (isFolederMode()) {
                gridView.layoutManager = V7LinearLayoutManager(mContext, 1, false)
            } else {
                var spanCount = if (isBaseOnWidth) 5 else 6
                style?.let { spanCount = ImgUtil.spanCountByStyle(it, spanCount) }
                gridView.layoutManager = if (spanCount == 1) {
                    V7LinearLayoutManager(mContext, spanCount, false)
                } else {
                    V7GridLayoutManager(mContext, spanCount)
                }
            }

            gridAdapter?.setOnLoadMoreListener({
                gridAdapter?.setEnableLoadMore(true)
                sortData?.let { sourceViewModel?.getList(it, page) }
            }, gridView)

            gridView.setOnItemListener(object : TvRecyclerView.OnItemListener {
                override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                    itemView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(300)
                        ?.setInterpolator(BounceInterpolator())?.start()
                }

                override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
                    itemView?.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.setDuration(300)
                        ?.setInterpolator(BounceInterpolator())?.start()
                }

                override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {}
            })

            gridView.setOnInBorderKeyEventListener { direction, _ ->
                // if (direction == View.FOCUS_UP) { }
                false
            }
        }

        gridAdapter?.onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            gridAdapter?.getData()?.get(position)?.let { video ->
                val bundle = Bundle().apply {
                    putString("id", video.id)
                    putString("sourceKey", video.sourceKey)
                    putString("title", video.name)
                }
                if (video.tag == "folder" || video.tag == "cover") {
                    focusedView = view
                    if ("12".indexOf(getUITag()) != -1) {
                        changeView(video.id, video.tag == "folder")
                    } else {
                        changeView(video.id, false)
                    }
                } else {
                    when {
                        video.id.isNullOrEmpty() || video.id.startsWith("msearch:") -> {
                            if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false) && enableFastSearch()) {
                                jumpActivity(FastSearchActivity::class.java, bundle)
                            } else {
                                jumpActivity(SearchActivity::class.java, bundle)
                            }
                        }
                        else -> {
                            bundle.putString("picture", video.pic)
                            jumpActivity(DetailActivity::class.java, bundle)
                        }
                    }
                }
            }
        }

        gridAdapter?.onItemLongClickListener = BaseQuickAdapter.OnItemLongClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            gridAdapter?.getData()?.get(position)?.let { video ->
                val bundle = Bundle().apply {
                    putString("id", video.id)
                    putString("sourceKey", video.sourceKey)
                    putString("title", video.name)
                }
                jumpActivity(FastSearchActivity::class.java, bundle)
            }
            true
        }

        gridAdapter?.setLoadMoreView(LoadMoreView())
        mGridView?.let { setLoadSir2(it) }
    }

    private fun initViewModel() {
        if (sourceViewModel != null) return
        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)
        sourceViewModel?.listResult?.observe(this) { absXml ->
            if (absXml?.movie?.videoList?.isNotEmpty() == true) {
                if (page == 1) {
                    showSuccess()
                    isLoad = true
                    gridAdapter?.setNewData(absXml.movie.videoList)
                } else {
                    gridAdapter?.addData(absXml.movie.videoList)
                }
                page++
                maxPage = absXml.movie.pagecount
                if (maxPage > 0 && page > maxPage) {
                    gridAdapter?.loadMoreEnd()
                    gridAdapter?.setEnableLoadMore(false)
                    if (page > 2) {
                        Toast.makeText(context, "没有更多了", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    gridAdapter?.loadMoreComplete()
                    gridAdapter?.setEnableLoadMore(true)
                }
            } else {
                if (page == 1) {
                    showEmpty()
                } else if (page > 2) {
                    Toast.makeText(context, "没有更多了", Toast.LENGTH_SHORT).show()
                }
                gridAdapter?.loadMoreEnd()
                gridAdapter?.setEnableLoadMore(false)
            }
        }
    }

    fun isLoad(): Boolean = isLoad || mGrids.isNotEmpty()

    private fun initData() {
        showLoading()
        isLoad = false
        scrollTop()
        toggleFilterColor()
        sortData?.let { sourceViewModel?.getList(it, page) }
    }

    private fun toggleFilterColor() {
        sortData?.filters?.takeIf { it.isNotEmpty() }?.let {
            val count = sortData?.filterSelectCount() ?: 0
            EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_FILTER_CHANGE, count))
        }
    }

    fun isTop(): Boolean = isTop

    fun scrollTop() {
        isTop = true
        mGridView?.scrollToPosition(0)
    }

    fun showFilter() {
        if (sortData?.filters?.isNotEmpty() == true && gridFilterDialog == null) {
            gridFilterDialog = GridFilterDialog(mContext)
            setFilterDialogData()
        }
        gridFilterDialog?.show()
    }

    fun setFilterDialogData() {
        val context = context ?: return
        val inflater = LayoutInflater.from(context)
        val defaultColor = ContextCompat.getColor(context, R.color.color_FFFFFF)
        val selectedColor = ContextCompat.getColor(context, R.color.color_02F8E1)

        sortData?.filters?.forEach { filter ->
            val line = inflater.inflate(R.layout.item_grid_filter, gridFilterDialog?.filterRoot, false)
            val filterNameTv: TextView = line.findViewById(R.id.filterName)
            filterNameTv.text = filter.name
            val gridView: TvRecyclerView = line.findViewById(R.id.mFilterKv)
            gridView.setHasFixedSize(true)
            gridView.layoutManager = V7LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            val adapter = GridFilterKVAdapter()
            gridView.adapter = adapter

            val key = filter.key
            val values = ArrayList(filter.values.keys)
            val keys = ArrayList(filter.values.values)

            var previousSelectedView: View? = null

            adapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, view, position ->
                val currentSelection = sortData?.filterSelect?.get(key)
                val newSelection = keys[position]
                
                if (currentSelection == null || currentSelection != newSelection) {
                    sortData?.filterSelect?.put(key, newSelection)
                    updateViewStyle(view, selectedColor, true)
                    previousSelectedView?.let { updateViewStyle(it, defaultColor, false) }
                    previousSelectedView = view
                } else {
                    sortData?.filterSelect?.remove(key)
                    previousSelectedView?.let { updateViewStyle(it, defaultColor, false) }
                    previousSelectedView = null
                }
                forceRefresh()
            }

            fun updateViewStyle(view: View, color: Int, isBold: Boolean) {
                val valueTv: TextView = view.findViewById(R.id.filterValue)
                valueTv.paint.isFakeBoldText = isBold
                valueTv.setTextColor(color)
            }

            adapter.setNewData(values)
            gridFilterDialog?.filterRoot?.addView(line)
        }
    }

    fun forceRefresh() {
        page = 1
        initData()
    }
}
