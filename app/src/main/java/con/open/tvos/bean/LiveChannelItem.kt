package con.open.tvos.bean

/**
 * @author pj567
 * @date :2021/1/12
 * @description:
 */
class LiveChannelItem {
    /**
     * channelIndex : 频道索引号
     * channelNum : 频道名称
     * channelSourceNames : 频道源名称
     * channelUrls : 频道源地址
     * sourceIndex : 频道源索引
     * sourceNum : 频道源总数
     */
    private var channelIndex: Int = 0
    private var channelNum: Int = 0
    private var channelName: String? = null
    private var channelSourceNames: ArrayList<String>? = null
    private var channelUrls: ArrayList<String>? = null
    
    var sourceIndex: Int = 0
    var sourceNum: Int = 0
    var include_back: Boolean = false

    fun setinclude_back(include_back: Boolean) {
        this.include_back = include_back
    }

    fun getinclude_back(): Boolean = include_back

    fun setChannelIndex(channelIndex: Int) {
        this.channelIndex = channelIndex
    }

    fun getChannelIndex(): Int = channelIndex

    fun setChannelNum(channelNum: Int) {
        this.channelNum = channelNum
    }

    fun getChannelNum(): Int = channelNum

    fun setChannelName(channelName: String?) {
        this.channelName = channelName
    }

    fun getChannelName(): String? = channelName

    fun getChannelUrls(): ArrayList<String>? = channelUrls

    fun setChannelUrls(channelUrls: ArrayList<String>?) {
        this.channelUrls = channelUrls
        sourceNum = channelUrls?.size ?: 0
    }

    fun preSource() {
        sourceIndex--
        if (sourceIndex < 0) sourceIndex = sourceNum - 1
    }

    fun nextSource() {
        sourceIndex++
        if (sourceIndex == sourceNum) sourceIndex = 0
    }

    fun setSourceIndex(sourceIndex: Int) {
        this.sourceIndex = sourceIndex
    }

    fun getSourceIndex(): Int = sourceIndex

    fun getUrl(): String? = channelUrls?.get(sourceIndex)

    fun getSourceNum(): Int = sourceNum

    fun getChannelSourceNames(): ArrayList<String>? = channelSourceNames

    fun setChannelSourceNames(channelSourceNames: ArrayList<String>?) {
        this.channelSourceNames = channelSourceNames
    }

    fun getSourceName(): String? = channelSourceNames?.get(sourceIndex)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as LiveChannelItem
        return channelName == that.channelName && channelUrls?.get(sourceIndex) == that.getUrl()
    }

    override fun hashCode(): Int {
        return listOf(channelName, channelUrls?.get(sourceIndex)).hashCode()
    }
}
