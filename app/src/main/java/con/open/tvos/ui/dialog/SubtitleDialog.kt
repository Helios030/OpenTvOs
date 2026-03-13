package con.open.tvos.ui.dialog

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import con.open.tvos.R
import con.open.tvos.util.FastClickCheckUtil
import con.open.tvos.util.SubtitleHelper

class SubtitleDialog(@NonNull context: Context) : BaseDialog(context) {

    var selectInternal: TextView? = null
        private set
    private var selectLocal: TextView? = null
    private var selectRemote: TextView? = null
    private var subtitleSizeMinus: TextView? = null
    private var subtitleSizeText: TextView? = null
    private var subtitleSizePlus: TextView? = null
    private var subtitleTimeMinus: TextView? = null
    private var subtitleTimeText: TextView? = null
    private var subtitleTimePlus: TextView? = null
    private var subtitleStyleOne: TextView? = null
    private var subtitleStyleTwo: TextView? = null

    private var mSearchSubtitleListener: SearchSubtitleListener? = null
    private var mLocalFileChooserListener: LocalFileChooserListener? = null
    private var mSubtitleViewListener: SubtitleViewListener? = null

    init {
        if (context is Activity) {
            setOwnerActivity(context)
        }
        setContentView(R.layout.dialog_subtitle)
        initView(context)
    }

    private fun initView(context: Context) {
        selectInternal = findViewById(R.id.selectInternal)
        selectLocal = findViewById(R.id.selectLocal)
        selectRemote = findViewById(R.id.selectRemote)
        subtitleSizeMinus = findViewById(R.id.subtitleSizeMinus)
        subtitleSizeText = findViewById(R.id.subtitleSizeText)
        subtitleSizePlus = findViewById(R.id.subtitleSizePlus)
        subtitleTimeMinus = findViewById(R.id.subtitleTimeMinus)
        subtitleTimeText = findViewById(R.id.subtitleTimeText)
        subtitleTimePlus = findViewById(R.id.subtitleTimePlus)
        subtitleStyleOne = findViewById(R.id.subtitleStyleOne)
        subtitleStyleTwo = findViewById(R.id.subtitleStyleTwo)

        selectLocal?.setOnClickListener { view ->
            FastClickCheckUtil.check(view)
            dismiss()
            mLocalFileChooserListener?.openLocalFileChooserDialog()
        }

        selectRemote?.setOnClickListener { view ->
            FastClickCheckUtil.check(view)
            dismiss()
            mSearchSubtitleListener?.openSearchSubtitleDialog()
        }

        val size = SubtitleHelper.getTextSize(ownerActivity)
        subtitleSizeText?.text = size.toString()

        subtitleSizeMinus?.setOnClickListener {
            val sizeStr = subtitleSizeText?.text.toString()
            var curSize = sizeStr.toInt()
            curSize -= 2
            if (curSize <= 12) {
                curSize = 12
            }
            subtitleSizeText?.text = curSize.toString()
            SubtitleHelper.setTextSize(curSize)
            mSubtitleViewListener?.setTextSize(curSize)
        }

        subtitleSizePlus?.setOnClickListener {
            val sizeStr = subtitleSizeText?.text.toString()
            var curSize = sizeStr.toInt()
            curSize += 2
            if (curSize >= 60) {
                curSize = 60
            }
            subtitleSizeText?.text = curSize.toString()
            SubtitleHelper.setTextSize(curSize)
            mSubtitleViewListener?.setTextSize(curSize)
        }

        val timeDelay = SubtitleHelper.getTimeDelay()
        val timeStr = if (timeDelay != 0) {
            val dbTimeDelay = timeDelay / 1000.0
            dbTimeDelay.toString()
        } else {
            "0"
        }
        subtitleTimeText?.text = timeStr

        subtitleTimeMinus?.setOnClickListener { view ->
            FastClickCheckUtil.check(view)
            val currentTimeStr = subtitleTimeText?.text.toString()
            var time = currentTimeStr.toFloat().toDouble()
            val oneceDelay = -0.5
            time += oneceDelay
            val displayTimeStr = if (time == 0.0) "0" else time.toString()
            subtitleTimeText?.text = displayTimeStr
            val mseconds = (oneceDelay * 1000).toInt()
            SubtitleHelper.setTimeDelay((time * 1000).toInt())
            mSubtitleViewListener?.setSubtitleDelay(mseconds)
        }

        subtitleTimePlus?.setOnClickListener { view ->
            FastClickCheckUtil.check(view)
            val currentTimeStr = subtitleTimeText?.text.toString()
            var time = currentTimeStr.toFloat().toDouble()
            val oneceDelay = 0.5
            time += oneceDelay
            val displayTimeStr = if (time == 0.0) "0" else time.toString()
            subtitleTimeText?.text = displayTimeStr
            val mseconds = (oneceDelay * 1000).toInt()
            SubtitleHelper.setTimeDelay((time * 1000).toInt())
            mSubtitleViewListener?.setSubtitleDelay(mseconds)
        }

        selectInternal?.setOnClickListener { view ->
            FastClickCheckUtil.check(view)
            dismiss()
            mSubtitleViewListener?.selectInternalSubtitle()
        }

        subtitleStyleOne?.setOnClickListener {
            val style = 0
            dismiss()
            mSubtitleViewListener?.setTextStyle(style)
            Toast.makeText(getContext(), "设置样式成功", Toast.LENGTH_SHORT).show()
        }

        subtitleStyleTwo?.setOnClickListener {
            val style = 1
            dismiss()
            mSubtitleViewListener?.setTextStyle(style)
            Toast.makeText(getContext(), "设置样式成功", Toast.LENGTH_SHORT).show()
        }
    }

    fun setLocalFileChooserListener(localFileChooserListener: LocalFileChooserListener) {
        mLocalFileChooserListener = localFileChooserListener
    }

    interface LocalFileChooserListener {
        fun openLocalFileChooserDialog()
    }

    fun setSearchSubtitleListener(searchSubtitleListener: SearchSubtitleListener) {
        mSearchSubtitleListener = searchSubtitleListener
    }

    interface SearchSubtitleListener {
        fun openSearchSubtitleDialog()
    }

    fun setSubtitleViewListener(subtitleViewListener: SubtitleViewListener) {
        mSubtitleViewListener = subtitleViewListener
    }

    interface SubtitleViewListener {
        fun setTextSize(size: Int)
        fun setSubtitleDelay(milliseconds: Int)
        fun selectInternalSubtitle()
        fun setTextStyle(style: Int)
    }
}
