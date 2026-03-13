package con.open.tvos.ui.tv.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.util.Random

class AudioWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint: Paint = Paint().apply {
        color = Color.RED // 字节跳动颜色
        style = Paint.Style.FILL
    }
    
    private val rectF1 = RectF()
    private val rectF2 = RectF()
    private val rectF3 = RectF()
    private val rectF4 = RectF()
    private val rectF5 = RectF()
    
    private var viewWidth: Int = 0
    private var viewHeight: Int = 0
    
    /** 每个条的宽度 */
    private var rectWidth: Int = 0
    
    /** 条数 */
    private val columnCount = 7
    
    /** 条间距 */
    private val space = 8
    
    /** 条随机高度 */
    private var randomHeight: Int = 0
    
    private val random = Random()
    
    private val handler = Handler(Looper.getMainLooper()) {
        invalidate()
        true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        
        rectWidth = (viewWidth - space * (columnCount - 1)) / columnCount
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val left = rectWidth + space
        
        // 画每个条之前高度都重新随机生成
        randomHeight = random.nextInt(viewHeight)
        rectF1.set(left * 0f, randomHeight.toFloat(), left * 0 + rectWidth.toFloat(), viewHeight.toFloat())
        
        randomHeight = random.nextInt(viewHeight)
        rectF2.set(left * 1f, randomHeight.toFloat(), left * 1 + rectWidth.toFloat(), viewHeight.toFloat())
        
        randomHeight = random.nextInt(viewHeight)
        rectF3.set(left * 2f, randomHeight.toFloat(), left * 2 + rectWidth.toFloat(), viewHeight.toFloat())
        
        randomHeight = random.nextInt(viewHeight)
        rectF4.set(left * 3f, randomHeight.toFloat(), left * 3 + rectWidth.toFloat(), viewHeight.toFloat())
        
        randomHeight = random.nextInt(viewHeight)
        rectF5.set(left * 4f, randomHeight.toFloat(), left * 4 + rectWidth.toFloat(), viewHeight.toFloat())
        
        canvas.drawRect(rectF1, paint)
        canvas.drawRect(rectF2, paint)
        canvas.drawRect(rectF3, paint)
        canvas.drawRect(rectF4, paint)
        canvas.drawRect(rectF5, paint)
        
        // 每间隔200毫秒发送消息刷新
        handler.sendEmptyMessageDelayed(0, 200)
    }
}
