package con.open.tvos.ui.activity

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import con.open.tvos.R
import con.open.tvos.base.BaseActivity
import con.open.tvos.server.ControlManager
import con.open.tvos.ui.tv.QRCodeGen
import me.jessyan.autosize.utils.AutoSizeUtils

class PushActivity : BaseActivity() {
    
    private lateinit var ivQRCode: ImageView
    private lateinit var tvAddress: TextView

    override fun getLayoutResID(): Int = R.layout.activity_push

    override fun init() {
        initView()
        initData()
    }

    private fun initView() {
        ivQRCode = findViewById(R.id.ivQRCode)
        tvAddress = findViewById(R.id.tvAddress)
        refreshQRCode()
        
        findViewById<View>(R.id.pushLocal).setOnClickListener {
            try {
                val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (manager != null) {
                    if (manager.hasPrimaryClip && manager.primaryClip != null && manager.primaryClip!!.itemCount > 0) {
                        val addedText = manager.primaryClip!!.getItemAt(0)
                        val clipText = addedText.text.toString().trim()
                        val newIntent = Intent(mContext, DetailActivity::class.java)
                        newIntent.putExtra("id", clipText)
                        newIntent.putExtra("sourceKey", "push_agent")
                        newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(newIntent)
                    }
                }
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
    }

    private fun refreshQRCode() {
        val address = ControlManager.get().getAddress(false)
        tvAddress.text = String.format("手机/电脑扫描上方二维码或者直接浏览器访问地址\n%s", address)
        ivQRCode.setImageBitmap(
            QRCodeGen.generateBitmap(
                "$addresspush.html",
                AutoSizeUtils.mm2px(this, 300f),
                AutoSizeUtils.mm2px(this, 300f),
                4
            )
        )
    }

    private fun initData() {
        // No data initialization needed
    }
}
