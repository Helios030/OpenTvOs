package con.open.tvos.bean

class LiveChannelGroup {
    /**
     * groupIndex : 分组索引号
     * groupName : 分组名称
     * password : 分组密码
     */
    var groupIndex: Int = 0
    var groupName: String? = null
    var groupPassword: String? = null
    var liveChannelItems: ArrayList<LiveChannelItem>? = null

    fun getLiveChannels(): ArrayList<LiveChannelItem>? = liveChannelItems

    fun setLiveChannels(liveChannelItems: ArrayList<LiveChannelItem>?) {
        this.liveChannelItems = liveChannelItems
    }
}
