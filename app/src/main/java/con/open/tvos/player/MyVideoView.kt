package con.open.tvos.player

import android.content.Context
import android.util.AttributeSet
import xyz.doikki.videoplayer.player.AbstractPlayer
import xyz.doikki.videoplayer.player.VideoView

class MyVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

    val mediaPlayer: AbstractPlayer?
        get() = mMediaPlayer
}
