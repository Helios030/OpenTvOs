package con.open.tvos.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.StyleRes
import con.open.tvos.R
import xyz.doikki.videoplayer.util.CutoutUtil

class BaseDialog @JvmOverloads constructor(
    context: Context,
    @StyleRes themeResId: Int = R.style.CustomDialogStyle
) : Dialog(context, themeResId) {

    override fun onCreate(savedInstanceState: Bundle?) {
        CutoutUtil.adaptCutoutAboveAndroidP(this, true)
        super.onCreate(savedInstanceState)
    }

    override fun show() {
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        super.show()
        hideSysBar()
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    private fun hideSysBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window?.decorView?.let { decorView ->
                var uiOptions = decorView.systemUiVisibility
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_FULLSCREEN
                uiOptions = uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                decorView.systemUiVisibility = uiOptions
            }
        }
    }
}
