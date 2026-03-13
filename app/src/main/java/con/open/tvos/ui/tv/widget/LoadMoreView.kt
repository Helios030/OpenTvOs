package con.open.tvos.ui.tv.widget

import con.open.tvos.R

class LoadMoreView : com.chad.library.adapter.base.loadmore.LoadMoreView() {
    
    override fun getLayoutId(): Int = R.layout.item_view_load_more
    
    override fun getLoadingViewId(): Int = R.id.load_more_loading_view
    
    override fun getLoadFailViewId(): Int = R.id.load_more_load_fail_view
    
    override fun getLoadEndViewId(): Int = R.id.load_more_load_end_view
}
