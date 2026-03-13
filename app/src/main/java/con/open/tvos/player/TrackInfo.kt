package con.open.tvos.player

class TrackInfo {
    private val audio: MutableList<TrackInfoBean> = mutableListOf()
    private val subtitle: MutableList<TrackInfoBean> = mutableListOf()

    fun getAudio(): List<TrackInfoBean> = audio

    fun getAudioSelected(track: Boolean): Int = getSelected(audio, track)

    fun getSubtitleSelected(track: Boolean): Int = getSelected(subtitle, track)

    private fun getSelected(list: List<TrackInfoBean>, track: Boolean): Int {
        list.forEachIndexed { index, trackInfoBean ->
            if (trackInfoBean.selected) {
                return if (track) trackInfoBean.index else index
            }
        }
        return 99999
    }

    fun addAudio(audio: TrackInfoBean) {
        this.audio.add(audio)
    }

    fun getSubtitle(): List<TrackInfoBean> = subtitle

    fun addSubtitle(subtitle: TrackInfoBean) {
        this.subtitle.add(subtitle)
    }
}
