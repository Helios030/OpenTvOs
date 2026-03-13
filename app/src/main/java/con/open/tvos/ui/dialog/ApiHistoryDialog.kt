package con.open.tvos.ui.dialog

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.NonNull
import con.open.tvos.R
import con.open.tvos.ui.adapter.ApiHistoryDialogAdapter
import com.owen.tvrecyclerview.widget.TvRecyclerView

class ApiHistoryDialog(@NonNull context: Context) : BaseDialog(context, R.style.CustomDialogStyleDim) {

    init {
        setContentView(R.layout.dialog_api_history)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun setTip(tip: String) {
        findViewById<TextView>(R.id.title)?.text = tip
    }

    fun setAdapter(
        sourceBeanSelectDialogInterface: ApiHistoryDialogAdapter.SelectDialogInterface,
        data: List<String>,
        select: Int
    ) {
        val adapter = ApiHistoryDialogAdapter(sourceBeanSelectDialogInterface)
        adapter.setData(data, select)
        val tvRecyclerView = findViewById<TvRecyclerView>(R.id.list)
        tvRecyclerView?.apply {
            setAdapter(adapter)
            setSelectedPosition(select)
            post {
                scrollToPosition(select)
            }
        }
    }
}
