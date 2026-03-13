package con.open.tvos.player

import android.content.Context
import xyz.doikki.videoplayer.player.PlayerFactory

class ExoMediaPlayerFactory private constructor() : PlayerFactory<ExoPlayer>() {

    companion object {
        fun create(): ExoMediaPlayerFactory = ExoMediaPlayerFactory()
    }

    override fun createPlayer(context: Context): ExoPlayer = ExoPlayer(context)
}
