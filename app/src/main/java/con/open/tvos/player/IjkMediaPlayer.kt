package con.open.tvos.player

import android.content.Context
import android.text.TextUtils
import con.open.tvos.api.ApiConfig
import con.open.tvos.bean.IJKCode
import con.open.tvos.server.ControlManager
import con.open.tvos.util.AudioTrackMemory
import con.open.tvos.util.FileUtils
import con.open.tvos.util.HawkConfig
import con.open.tvos.util.LOG
import con.open.tvos.util.MD5
import com.orhanobut.hawk.Hawk
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.misc.ITrackInfo
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo
import xyz.doikki.videoplayer.ijk.IjkPlayer
import java.io.File
import java.net.URI
import java.net.URLEncoder

class IjkMediaPlayer(
    context: Context,
    private var codec: IJKCode?
) : IjkPlayer(context) {

    companion object {
        private const val ITV_TARGET_DOMAIN = "gslbserv.itv.cmvideo.cn"
        private const val RTSP_UDP_RTP = 1
        private const val CACHE_VIDEO = 2
        private const val M3U8 = 3
        private const val OTHER = 0
        
        private var memory: AudioTrackMemory? = null
    }

    protected var currentPlayPath: String? = null

    init {
        memory = AudioTrackMemory.getInstance(context)
    }

    override fun setOptions() {
        super.setOptions()
        val codecTmp = codec ?: ApiConfig.get().currentIJKCode
        val options = codecTmp.option
        options?.forEach { (key, value) ->
            val opt = key.split("|")
            val category = opt[0].trim().toInt()
            val name = opt[1].trim()
            try {
                val valLong = value?.toLong() ?: 0L
                mMediaPlayer.setOption(category, name, valLong)
            } catch (e: Exception) {
                mMediaPlayer.setOption(category, name, value ?: "")
            }
        }
        
        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-fps", 30)
        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_PLAYER, "subtitle", 1)
        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1)
        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_timeout", -1)
        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "safe", 0)
        
        if (Hawk.get(HawkConfig.PLAYER_IS_LIVE, false)) {
            LOG.i("echo-type-直播")
            mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 300)
            mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1)
            mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 1)
            mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_CODEC, "threads", "1")
        } else {
            LOG.i("echo-type-点播")
            mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 3000)
            mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "infbuf", 0)
            mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_CODEC, "threads", "2")
        }
    }

    override fun setDataSource(path: String, headers: Map<String, String>?) {
        var modifiedPath = path
        try {
            when (getStreamType(path)) {
                RTSP_UDP_RTP -> {
                    mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "infbuf", 1)
                    mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp")
                    mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_flags", "prefer_tcp")
                    mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 512 * 1000)
                    mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 2 * 1000 * 1000)
                }
                CACHE_VIDEO -> {
                    if (Hawk.get(HawkConfig.IJK_CACHE_PLAY, false)) {
                        val cachePath = FileUtils.getCachePath() + "/ijkcaches/"
                        val cacheFile = File(cachePath)
                        if (!cacheFile.exists()) cacheFile.mkdirs()
                        val tmpMd5 = MD5.string2MD5(path)
                        val cacheFilePath = cachePath + tmpMd5 + ".file"
                        val cacheMapPath = cachePath + tmpMd5 + ".map"
                        
                        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_file_path", cacheFilePath)
                        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_map_path", cacheMapPath)
                        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "parse_cache_map", 1)
                        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "auto_save_map", 1)
                        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_max_capacity", 60 * 1024 * 1024)
                        modifiedPath = "ijkio:cache:ffio:$path"
                    }
                }
                M3U8 -> {
                    if (Hawk.get(HawkConfig.PLAYER_IS_LIVE, false)) {
                        val uri = URI(path)
                        val host = uri.host
                        if (ITV_TARGET_DOMAIN.equals(host, ignoreCase = true)) {
                            modifiedPath = ControlManager.get().getAddress(true) + "proxy?go=live&type=m3u8&url=" + URLEncoder.encode(path, "UTF-8")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        setDataSourceHeader(headers)
        mMediaPlayer.setOption(
            tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT,
            "protocol_whitelist",
            "ijkio,ffio,async,cache,crypto,file,dash,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtp,tcp,tls,udp,ijkurlhook,data"
        )
        currentPlayPath = modifiedPath
        super.setDataSource(modifiedPath, null)
    }

    private fun getStreamType(path: String): Int {
        if (TextUtils.isEmpty(path)) return OTHER
        
        val lowerPath = path.lowercase()
        if (lowerPath.startsWith("rtsp://") || lowerPath.startsWith("udp://") || lowerPath.startsWith("rtp://")) {
            return RTSP_UDP_RTP
        }
        
        val cleanUrl = path.split("?")[0]
        return when {
            cleanUrl.endsWith(".m3u8") -> M3U8
            cleanUrl.endsWith(".mp4") || cleanUrl.endsWith(".mkv") || cleanUrl.endsWith(".avi") -> CACHE_VIDEO
            else -> OTHER
        }
    }

    private fun setDataSourceHeader(headers: Map<String, String>?) {
        if (headers.isNullOrEmpty()) return
        
        val userAgent = headers["User-Agent"]
        if (!TextUtils.isEmpty(userAgent)) {
            mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", userAgent)
            headers.toMutableMap().remove("User-Agent")
        }
        
        if (headers.isNotEmpty()) {
            val sb = StringBuilder()
            headers.forEach { (key, value) ->
                if (!TextUtils.isEmpty(value)) {
                    sb.append(key)
                    sb.append(": ")
                    sb.append(value)
                    sb.append("\r\n")
                }
            }
            mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_FORMAT, "headers", sb.toString())
        }
    }

    fun getTrackInfo(): TrackInfo? {
        val trackInfo = mMediaPlayer.trackInfo ?: return null
        val data = TrackInfo()
        val subtitleSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
        val audioSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
        
        trackInfo.forEachIndexed { index, info ->
            when (info.trackType) {
                ITrackInfo.MEDIA_TRACK_TYPE_AUDIO -> {
                    val a = TrackInfoBean().apply {
                        val name = processAudioName(info.infoInline)
                        language = info.language
                        if (name.startsWith("aac")) language = "中文"
                        this.name = name
                        this.index = index
                        selected = index == audioSelected
                    }
                    data.addAudio(a)
                }
                ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT -> {
                    val t = TrackInfoBean().apply {
                        name = info.infoInline
                        language = info.language
                        index = index
                        selected = index == subtitleSelected
                    }
                    data.addSubtitle(t)
                }
            }
        }
        return data
    }

    private fun processAudioName(rawName: String): String {
        return rawName
            .replace("AUDIO,", "")
            .replace("N/A,", "")
            .replace(" ", "")
    }

    fun setTrack(trackIndex: Int) {
        val audioSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
        val subtitleSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
        if (trackIndex != audioSelected && trackIndex != subtitleSelected) {
            mMediaPlayer.selectTrack(trackIndex)
        }
    }

    fun setTrack(trackIndex: Int, playKey: String) {
        val audioSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
        if (trackIndex != audioSelected) {
            if (playKey.isNotEmpty()) {
                memory?.save(playKey, trackIndex)
            }
            mMediaPlayer.selectTrack(trackIndex)
        }
    }

    fun setOnTimedTextListener(listener: IMediaPlayer.OnTimedTextListener) {
        mMediaPlayer.setOnTimedTextListener(listener)
    }

    fun loadDefaultTrack(trackInfo: TrackInfo?, playKey: String) {
        if (trackInfo != null && trackInfo.getAudio().size > 1) {
            val trackIndex = memory?.ijkLoad(playKey) ?: -1
            if (trackIndex == -1) {
                val firstIndex = trackInfo.getAudio()[0].index
                setTrack(firstIndex)
                return
            }
            setTrack(trackIndex)
        }
    }
}
