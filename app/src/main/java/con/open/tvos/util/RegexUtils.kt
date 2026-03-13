package con.open.tvos.util

import java.util.regex.Pattern

object RegexUtils {

    private val patternCache = HashMap<String, Pattern>()

    @JvmStatic
    fun getPattern(regex: String): Pattern {
        return patternCache.getOrPut(regex) { Pattern.compile(regex) }
    }

    @JvmStatic
    fun getPattern(regex: String, flag: Int): Pattern {
        return patternCache.getOrPut(regex) { Pattern.compile(regex, flag) }
    }
}
