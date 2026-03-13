package con.open.tvos.ui.tv.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import con.open.tvos.R

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
class SearchKeyboard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val mRecyclerView: RecyclerView
    private val keys = listOf(
        "远程搜索", "删除", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"
    )
    private val keyboardList: MutableList<Keyboard> = ArrayList()
    private var searchKeyListener: OnSearchKeyListener? = null
    
    private val focusChangeListener = View.OnFocusChangeListener { itemView, hasFocus ->
        if (itemView != null && itemView !== mRecyclerView) {
            itemView.isSelected = hasFocus
        }
    }
    
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_keyborad, this)
        mRecyclerView = view.findViewById(R.id.mRecyclerView)
        val manager = GridLayoutManager(context, 6)
        mRecyclerView.layoutManager = manager
        
        mRecyclerView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(child: View) {
                if (child.isFocusable && child.onFocusChangeListener == null) {
                    child.onFocusChangeListener = focusChangeListener
                }
            }
            
            override fun onChildViewDetachedFromWindow(view: View) {
                // No-op
            }
        })
        
        for (key in keys) {
            keyboardList.add(Keyboard(1, key))
        }
        
        val adapter = KeyboardAdapter(keyboardList)
        mRecyclerView.adapter = adapter
        
        adapter.spanSizeLookup = object : BaseQuickAdapter.SpanSizeLookup() {
            override fun getSpanSize(gridLayoutManager: GridLayoutManager, position: Int): Int {
                return when (position) {
                    0 -> 3
                    1 -> 3
                    else -> 1
                }
            }
        }
        
        adapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, _, position ->
            val keyboard = adapter.getItem(position) as Keyboard
            searchKeyListener?.onSearchKey(position, keyboard.key)
        }
    }
    
    fun setOnSearchKeyListener(listener: OnSearchKeyListener) {
        searchKeyListener = listener
    }
    
    internal class Keyboard(
        private val itemType: Int,
        var key: String
    ) : MultiItemEntity {
        override fun getItemType(): Int = itemType
    }
    
    private class KeyboardAdapter(data: List<Keyboard>) : 
        BaseMultiItemQuickAdapter<Keyboard, BaseViewHolder>(data) {
        
        init {
            addItemType(1, R.layout.item_keyboard)
        }
        
        override fun convert(helper: BaseViewHolder, item: Keyboard) {
            when (helper.itemViewType) {
                1 -> helper.setText(R.id.keyName, item.key)
            }
        }
    }
    
    interface OnSearchKeyListener {
        fun onSearchKey(pos: Int, key: String)
    }
}
