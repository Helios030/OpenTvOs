package con.open.tvos.ui.tv.widget

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * @author acer
 * @date 2018/7/24 15:
 */
class NoScrollViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {
    
    /**
     * 禁止viewpager里面内容导致页面切换
     */
    override fun executeKeyEvent(event: KeyEvent): Boolean = false
    
    override fun onTouchEvent(ev: MotionEvent): Boolean = false
    
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = false
}
