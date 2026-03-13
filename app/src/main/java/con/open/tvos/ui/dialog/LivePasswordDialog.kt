package con.open.tvos.ui.dialog

import android.app.Activity
import android.content.Context
import android.widget.EditText
import androidx.annotation.NonNull
import con.open.tvos.R

/**
 * 描述
 *
 * @author pj567
 * @since 2020/12/27
 */
class LivePasswordDialog(@NonNull context: Context) : BaseDialog(context) {

    private val inputPassword: EditText

    private var listener: OnListener? = null

    init {
        setOwnerActivity(context as Activity)
        setContentView(R.layout.dialog_live_password)
        inputPassword = findViewById(R.id.input)
        findViewById<View>(R.id.inputSubmit).setOnClickListener {
            val password = inputPassword.text.toString().trim()
            if (password.isNotEmpty()) {
                listener?.onChange(password)
                dismiss()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        listener?.onCancel()
        dismiss()
    }

    fun setOnListener(listener: OnListener) {
        this.listener = listener
    }

    interface OnListener {
        fun onChange(password: String)
        fun onCancel()
    }
}
