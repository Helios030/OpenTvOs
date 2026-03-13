package con.open.tvos.ui.dialog

import android.app.UiModeManager
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import com.chad.library.adapter.base.BaseQuickAdapter
import con.open.tvos.R
import con.open.tvos.bean.MovieSort
import con.open.tvos.ui.adapter.GridFilterKVAdapter
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager

class GridFilterDialog(@NonNull context: Context) : BaseDialog(context) {

    var filterRoot: LinearLayout? = null
        private set

    private var selectChange = false

    init {
        setCanceledOnTouchOutside(false)
        setCancelable(true)
        setContentView(R.layout.dialog_grid_filter)
        filterRoot = findViewById(R.id.filterRoot)

        if (!isTvOrBox(context)) {
            findViewById<View>(R.id.root)?.setOnClickListener {
                dismiss()
            }
        }
    }

    interface Callback {
        fun change()
    }

    fun setOnDismiss(callback: Callback) {
        setOnDismissListener { _: DialogInterface ->
            if (selectChange) {
                callback.change()
            }
        }
    }

    fun setData(sortData: MovieSort.SortData) {
        val filters = sortData.filters
        for (filter in filters) {
            val line = LayoutInflater.from(context).inflate(R.layout.item_grid_filter, null)
            line.findViewById<TextView>(R.id.filterName)?.text = filter.name
            val gridView = line.findViewById<TvRecyclerView>(R.id.mFilterKv)
            gridView?.setHasFixedSize(true)
            gridView?.setLayoutManager(V7LinearLayoutManager(context, 0, false))
            val filterKVAdapter = GridFilterKVAdapter()
            gridView?.adapter = filterKVAdapter
            val key = filter.key
            val values = ArrayList(filter.values.values)
            val keys = ArrayList(filter.values.keys)

            var pre: View? = null
            filterKVAdapter.onItemClickListener = object : BaseQuickAdapter.OnItemClickListener {
                override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
                    selectChange = true
                    val filterSelect = sortData.filterSelect[key]
                    if (filterSelect == null || filterSelect != keys[position]) {
                        sortData.filterSelect[key] = keys[position]
                        pre?.findViewById<TextView>(R.id.filterValue)?.let { val ->
                            val.paint = val.paint
                            paint.isFakeBoldText = false
                            val.setTextColor(context.resources.getColor(R.color.color_FFFFFF))
                        }
                        view.findViewById<TextView>(R.id.filterValue)?.let { val ->
                            val.paint = val.paint
                            paint.isFakeBoldText = true
                            val.setTextColor(context.resources.getColor(R.color.color_02F8E1))
                        }
                        pre = view
                    } else {
                        sortData.filterSelect.remove(key)
                        pre?.findViewById<TextView>(R.id.filterValue)?.let { val ->
                            val paint = val.paint
                            paint.isFakeBoldText = false
                            val.setTextColor(context.resources.getColor(R.color.color_FFFFFF))
                        }
                        pre = null
                    }
                }
            }
            filterKVAdapter.setNewData(values)
            filterRoot?.addView(line)
        }
    }

    override fun show() {
        selectChange = false
        super.show()
        window?.attributes?.apply {
            gravity = Gravity.BOTTOM
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
            dimAmount = 0f
        }
        window?.decorView?.setPadding(0, 0, 0, 0)
    }

    companion object {
        fun isTvOrBox(context: Context): Boolean {
            // SDK > Android 11 直接认为不是 TV / 机顶盒
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                return false
            }
            // SDK <= Android 9 直接认为是 TV / 机顶盒
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                return true
            }
            val pm = context.packageManager
            val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

            // 1. UiMode 判断
            if (uiMode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
                return true
            }

            // 2. Android TV / Leanback 特性判断
            if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                || pm.hasSystemFeature("android.software.leanback_only") // Strict leanback
                || pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                // Amazon Fire TV
                || pm.hasSystemFeature("amazon.hardware.fire_tv")
                // Google TV (part of Android TV 家族)
                || pm.hasSystemFeature("com.google.android.tv")
            ) {
                return true
            }

            // 3. 没有触摸屏：大多数机顶盒、电视不带触摸
            if (!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
                return true
            }

            // 4. 物理遥控器 / D‑pad 键存在判断 兼容一些既有触摸也支持遥控的设备
            if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_UP)
                && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_DOWN)
                && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_LEFT)
                && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_RIGHT)
                && KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_CENTER)
            ) {
                return true
            }

            // 5. 输入设备源中有 DPAD
            val deviceIds = InputDevice.getDeviceIds()
            for (id in deviceIds) {
                val dev = InputDevice.getDevice(id) ?: continue
                val sources = dev.sources
                if (sources and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD) {
                    return true
                }
            }

            return false
        }
    }
}
