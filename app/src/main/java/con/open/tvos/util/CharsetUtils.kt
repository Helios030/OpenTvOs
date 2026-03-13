package con.open.tvos.util

import java.nio.charset.Charset
import org.mozilla.universalchardet.UniversalDetector

/**
 * 字符集工具类，提供了检测字符集的工具方法
 * 首先当然是使用mozilla的开源工具包universalchardet进行字符集检测，对于检测失败的，使用中文常用字进行再次检测
 */
object CharsetUtils {

    /**
     * 中文常用字符集
     */
    val AVAILABLE_CHINESE_CHARSET_NAMES = arrayOf("GBK", "gb2312", "GB18030", "UTF-8", "Big5")

    /**
     * 中文常用字
     */
    private val CHINESE_COMMON_CHARACTER_PATTERN = Regex("的|一|是|了|我|不|人|在|他|有|这|个|上|们|来|到|时|大|地|为|子|中|你|说|生|国|年|着|就|那|和|要")

    @JvmStatic
    fun detect(content: ByteArray): Charset {
        var charset = universalDetect(content)
        if (!charset.isNullOrEmpty()) {
            return Charset.forName(charset)
        }

        var longestMatch = 0
        for (cs in AVAILABLE_CHINESE_CHARSET_NAMES) {
            val temp = String(content, Charset.forName(cs))
            val count = CHINESE_COMMON_CHARACTER_PATTERN.findAll(temp).count()
            if (count > longestMatch) {
                longestMatch = count
                charset = cs
            }
        }
        return Charset.forName(charset ?: "GB18030")
    }

    /**
     * 使用mozilla的开源工具包universalchardet进行字符集检测，不一定能完全检测中文字符集
     */
    @JvmStatic
    fun universalDetect(content: ByteArray): String? {
        val detector = UniversalDetector(null)
        detector.handleData(content, 0, content.size)
        detector.dataEnd()
        return detector.detectedCharset
    }
}
