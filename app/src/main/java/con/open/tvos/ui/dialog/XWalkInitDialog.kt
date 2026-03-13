package con.open.tvos.ui.dialog

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Toast
import androidx.annotation.NonNull
import con.open.tvos.R
import con.open.tvos.util.FastClickCheckUtil
import con.open.tvos.util.XWalkUtils
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.FileCallback
import com.lzy.okgo.model.Progress
import com.lzy.okgo.model.Response
import java.io.File

class XWalkInitDialog(@NonNull context: Context) : BaseDialog(context) {

    private var listener: OnListener? = null

    init {
        setCanceledOnTouchOutside(false)
        setCancelable(true)
        setContentView(R.layout.dialog_xwalk)
        setOnDismissListener {
            OkGo.getInstance().cancelTag("down_xwalk")
        }

        val downText = findViewById<TextView>(R.id.downXWalk)
        val downTip = findViewById<TextView>(R.id.downXWalkArch)

        downTip.text = "下载XWalkView运行组件\nArch:${XWalkUtils.getRuntimeAbi()}"

        if (XWalkUtils.xWalkLibExist(context)) {
            downText.text = "重新下载"
        }

        downText.setOnClickListener { v ->
            FastClickCheckUtil.check(v)

            fun setTextEnable(enable: Boolean) {
                downText.isEnabled = enable
                downText.setTextColor(if (enable) Color.BLACK else Color.GRAY)
            }

            setTextEnable(false)

            OkGo.get<File>(XWalkUtils.downUrl())
                .tag("down_xwalk")
                .execute(object : FileCallback(
                    context.cacheDir.absolutePath,
                    XWalkUtils.saveZipFile()
                ) {
                    override fun onSuccess(response: Response<File>) {
                        try {
                            XWalkUtils.unzipXWalkZip(context, response.body().absolutePath)
                            XWalkUtils.extractXWalkLib(context)
                            downText.text = "重新下载"
                            listener?.onchange()
                            dismiss()
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            setTextEnable(true)
                        }
                    }

                    override fun onError(response: Response<File>) {
                        super.onError(response)
                        Toast.makeText(context, response.exception?.message, Toast.LENGTH_LONG).show()
                        setTextEnable(true)
                    }

                    override fun downloadProgress(progress: Progress) {
                        super.downloadProgress(progress)
                        downText.text = String.format("%.2f%%", progress.fraction * 100)
                    }
                })
        }
    }

    fun setOnListener(listener: OnListener?): XWalkInitDialog {
        this.listener = listener
        return this
    }

    interface OnListener {
        fun onchange()
    }
}
