package con.open.tvos.util

import android.app.Activity
import android.content.Context
import com.orhanobut.hawk.Hawk
import org.json.JSONException
import tv.danmaku.ijk.media.player.IjkLibLoader
import xyz.doikki.videoplayer.player.AndroidMediaPlayerFactory
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.render.TextureRenderViewFactory
import con.open.tvos.api.ApiConfig
import con.open.tvos.bean.IJKCode
import con.open.tvos.player.ExoMediaPlayerFactory
import con.open.tvos.player.IjkMediaPlayer
import con.open.tvos.player.render.SurfaceRenderViewFactory
import con.open.tvos.player.thirdparty.*
import java.text.DecimalFormat

object PlayerHelper {

    private var mPlayersInfo: HashMap<Int, String>? = null
    private var mPlayersExistInfo: HashMap<Int, Boolean>? = null

    @JvmStatic
    fun updateCfg(videoView: VideoView?, playerCfg: org.json.JSONObject) {
        updateCfg(videoView, playerCfg, -1)
    }

    @JvmStatic
    fun updateCfg(videoView: VideoView?, playerCfg: org.json.JSONObject, forcePlayerType: Int) {
        var playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0)
        var renderType = Hawk.get(HawkConfig.PLAY_RENDER, 0)
        var ijkCode = Hawk.get(HawkConfig.IJK_CODEC, "硬解码")
        var scale = Hawk.get(HawkConfig.PLAY_SCALE, 0)
        try {
            playerType = playerCfg.getInt("pl")
            renderType = playerCfg.getInt("pr")
            ijkCode = playerCfg.getString("ijk")
            scale = playerCfg.getInt("sc")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        if (forcePlayerType >= 0) playerType = forcePlayerType
        val codec = ApiConfig.get().getIJKCodec(ijkCode)
        val playerFactory = when (playerType) {
            1 -> {
                initIjkPlayer()
                object : xyz.doikki.videoplayer.player.PlayerFactory<IjkMediaPlayer>() {
                    override fun createPlayer(context: Context): IjkMediaPlayer {
                        return IjkMediaPlayer(context, codec)
                    }
                }
            }
            2 -> ExoMediaPlayerFactory.create()
            else -> AndroidMediaPlayerFactory.create()
        }
        val renderViewFactory = when (renderType) {
            1 -> SurfaceRenderViewFactory.create()
            else -> TextureRenderViewFactory.create()
        }
        videoView?.apply {
            setPlayerFactory(playerFactory)
            setRenderViewFactory(renderViewFactory)
            setScreenScaleType(scale)
        }
    }

    @JvmStatic
    fun updateCfg(videoView: VideoView) {
        val playType = Hawk.get(HawkConfig.PLAY_TYPE, 0)
        val playerFactory = when (playType) {
            1 -> {
                initIjkPlayer()
                object : xyz.doikki.videoplayer.player.PlayerFactory<IjkMediaPlayer>() {
                    override fun createPlayer(context: Context): IjkMediaPlayer {
                        return IjkMediaPlayer(context, null)
                    }
                }
            }
            2 -> ExoMediaPlayerFactory.create()
            else -> AndroidMediaPlayerFactory.create()
        }
        val renderType = Hawk.get(HawkConfig.PLAY_RENDER, 0)
        val renderViewFactory = when (renderType) {
            1 -> SurfaceRenderViewFactory.create()
            else -> TextureRenderViewFactory.create()
        }
        videoView.setPlayerFactory(playerFactory)
        videoView.setRenderViewFactory(renderViewFactory)
    }

    @JvmStatic
    fun init() {
        initIjkPlayer()
    }

    private fun initIjkPlayer() {
        try {
            tv.danmaku.ijk.media.player.IjkMediaPlayer.loadLibrariesOnce(object : IjkLibLoader() {
                override fun loadLibrary(s: String) {
                    try {
                        System.loadLibrary(s)
                    } catch (th: Throwable) {
                        th.printStackTrace()
                    }
                }
            })
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    @JvmStatic
    fun getPlayerName(playType: Int): String {
        val playersInfo = getPlayersInfo()
        return playersInfo[playType] ?: "系统播放器"
    }

    @JvmStatic
    fun getPlayersInfo(): HashMap<Int, String> {
        if (mPlayersInfo == null) {
            val playersInfo = HashMap<Int, String>()
            playersInfo[0] = "系统播放器"
            playersInfo[1] = "IJK播放器"
            playersInfo[2] = "Exo播放器"
            playersInfo[10] = "MX播放器"
            playersInfo[11] = "Reex播放器"
            playersInfo[12] = "Kodi播放器"
            playersInfo[13] = "附近TVBox"
            playersInfo[14] = "VLC播放器"
            mPlayersInfo = playersInfo
        }
        return mPlayersInfo!!
    }

    @JvmStatic
    fun getPlayersExistInfo(): HashMap<Int, Boolean> {
        if (mPlayersExistInfo == null) {
            val playersExist = HashMap<Int, Boolean>()
            playersExist[0] = true
            playersExist[1] = true
            playersExist[2] = true
            playersExist[10] = MXPlayer.getPackageInfo() != null
            playersExist[11] = ReexPlayer.getPackageInfo() != null
            playersExist[12] = Kodi.getPackageInfo() != null
            playersExist[13] = RemoteTVBox.getAvalible() != null
            playersExist[14] = VlcPlayer.getPackageInfo() != null
            mPlayersExistInfo = playersExist
        }
        return mPlayersExistInfo!!
    }

    @JvmStatic
    fun getPlayerExist(playType: Int): Boolean {
        val playersExistInfo = getPlayersExistInfo()
        return playersExistInfo[playType] ?: false
    }

    @JvmStatic
    fun getExistPlayerTypes(): ArrayList<Int> {
        val playersExistInfo = getPlayersExistInfo()
        val existPlayers = ArrayList<Int>()
        for (playerType in playersExistInfo.keys) {
            if (playersExistInfo[playerType] == true) {
                existPlayers.add(playerType)
            }
        }
        return existPlayers
    }

    @JvmStatic
    fun runExternalPlayer(
        playerType: Int,
        activity: Activity,
        url: String,
        title: String,
        subtitle: String,
        headers: HashMap<String, String>
    ): Boolean {
        return runExternalPlayer(playerType, activity, url, title, subtitle, headers, 0)
    }

    @JvmStatic
    fun runExternalPlayer(
        playerType: Int,
        activity: Activity,
        url: String,
        title: String,
        subtitle: String,
        headers: HashMap<String, String>?,
        progress: Long
    ): Boolean {
        return when (playerType) {
            10 -> MXPlayer.run(activity, url, title, subtitle, headers)
            11 -> ReexPlayer.run(activity, url, title, subtitle, headers)
            12 -> Kodi.run(activity, url, title, subtitle, headers)
            13 -> RemoteTVBox.run(activity, url, title, subtitle, headers)
            14 -> VlcPlayer.run(activity, url, title, subtitle, progress)
            else -> false
        }
    }

    @JvmStatic
    fun getRenderName(renderType: Int): String {
        return if (renderType == 1) "SurfaceView" else "TextureView"
    }

    @JvmStatic
    fun getScaleName(screenScaleType: Int): String {
        return when (screenScaleType) {
            VideoView.SCREEN_SCALE_DEFAULT -> "默认"
            VideoView.SCREEN_SCALE_16_9 -> "16:9"
            VideoView.SCREEN_SCALE_4_3 -> "4:3"
            VideoView.SCREEN_SCALE_MATCH_PARENT -> "填充"
            VideoView.SCREEN_SCALE_ORIGINAL -> "原始"
            VideoView.SCREEN_SCALE_CENTER_CROP -> "裁剪"
            else -> "默认"
        }
    }

    @JvmStatic
    fun getDisplaySpeed(speed: Long, show: Boolean): String {
        return when {
            speed > 1048576 -> DecimalFormat("#.00").format(speed / 1048576.0) + "Mb/s"
            speed > 1024 -> "${speed / 1024}Kb/s"
            speed > 0 -> "${speed}B/s"
            show -> "0B/s"
            else -> ""
        }
    }

    @JvmStatic
    fun getDisplaySpeedBps(speed: Long, show: Boolean): String {
        val bitSpeed = speed * 8
        return when {
            bitSpeed >= 1_000_000_000 -> DecimalFormat("0.00").format(bitSpeed / 1_000_000_000.0) + "Gbps"
            bitSpeed >= 1_000 -> {
                val mbps = bitSpeed / 1_000_000.0
                val df = if (mbps < 0.1) DecimalFormat("0.00") else DecimalFormat("0.0")
                df.format(mbps) + "Mbps"
            }
            show -> "0bps"
            else -> ""
        }
    }
}
