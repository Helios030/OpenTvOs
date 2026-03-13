package con.open.tvos.bean

import android.util.Base64
import con.open.tvos.util.DefaultConfig

/**
 * @author pj567
 * @date :2021/3/8
 * @description:
 */
class ParseBean {
    var name: String? = null
    var url: String? = null
        get() = DefaultConfig.checkReplaceProxy(field)
        set(value) {
            field = value
        }
    var ext: String? = null
    var type: Int = 0 // 0 普通嗅探 1 json 2 Json扩展 3 聚合

    var isDefault: Boolean = false

    fun mixUrl(): String {
        val url = this.url ?: return ""
        val ext = this.ext ?: ""
        if (ext.isNotEmpty()) {
            val idx = url.indexOf("?")
            if (idx > 0) {
                return url.substring(0, idx + 1) + 
                    "cat_ext=" + Base64.encodeToString(
                        ext.toByteArray(),
                        Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP
                    ) + "&" + url.substring(idx + 1)
            }
        }
        return url
    }
}
