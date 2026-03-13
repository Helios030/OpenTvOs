package con.open.tvos.player.controller

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import xyz.doikki.videoplayer.controller.BaseVideoController
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.controller.IGestureComponent
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils

abstract class BaseController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseVideoController(context, attrs, defStyleAttr),
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    View.OnTouchListener {

    private var mGestureDetector: GestureDetector? = null
    private var mAudioManager: AudioManager? = null
    private var mIsGestureEnabled = true
    private var mStreamVolume = 0
    private var mBrightness = 0f
    private var mSeekPosition = 0
    private var mFirstTouch = false
    private var mChangePosition = false
    private var mChangeBrightness = false
    private var mChangeVolume = false
    private var mCanChangePosition = true
    private var mEnableInNormal = false
    private var mCanSlide = false
    private var mCurPlayState = 0

    protected var mHandler: Handler? = null

    protected var mHandlerCallback: HandlerCallback? = null

    protected interface HandlerCallback {
        fun callback(msg: Message)
    }

    private var mIsDoubleTapTogglePlayEnabled = true

    private var mSlideInfo: TextView? = null
    private var mLoading: ProgressBar? = null
    private var mPauseRoot: ViewGroup? = null
    private var mPauseTime: TextView? = null

    init {
        mHandler = Handler { msg ->
            when (msg.what) {
                MSG_SLIDE_INFO_SHOW -> {
                    mSlideInfo?.visibility = VISIBLE
                    mSlideInfo?.text = msg.obj?.toString()
                }
                MSG_SLIDE_INFO_HIDE -> {
                    mSlideInfo?.visibility = GONE
                }
                else -> {
                    mHandlerCallback?.callback(msg)
                }
            }
            false
        }
    }

    override fun initView() {
        super.initView()
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        mGestureDetector = GestureDetector(context, this)
        setOnTouchListener(this)
        mSlideInfo = findViewWithTag("vod_control_slide_info")
        mLoading = findViewWithTag("vod_control_loading")
        mPauseRoot = findViewWithTag("vod_control_pause")
        mPauseTime = findViewWithTag("vod_control_pause_t")
    }

    override fun setProgress(duration: Int, position: Int) {
        super.setProgress(duration, position)
        mPauseTime?.text = "${PlayerUtils.stringForTime(position)} / ${PlayerUtils.stringForTime(duration)}"
    }

    override fun onPlayStateChanged(playState: Int) {
        super.onPlayStateChanged(playState)
        when (playState) {
            VideoView.STATE_IDLE -> mLoading?.visibility = GONE
            VideoView.STATE_PLAYING -> {
                mPauseRoot?.visibility = GONE
                mLoading?.visibility = GONE
            }
            VideoView.STATE_PAUSED -> {
                mPauseRoot?.visibility = VISIBLE
                mLoading?.visibility = GONE
            }
            VideoView.STATE_PREPARED, VideoView.STATE_ERROR, VideoView.STATE_BUFFERED -> {
                mLoading?.visibility = GONE
            }
            VideoView.STATE_PREPARING, VideoView.STATE_BUFFERING -> {
                mLoading?.visibility = VISIBLE
            }
            VideoView.STATE_PLAYBACK_COMPLETED -> {
                mLoading?.visibility = GONE
                mPauseRoot?.visibility = GONE
            }
        }
    }

    /**
     * 设置是否可以滑动调节进度，默认可以
     */
    fun setCanChangePosition(canChangePosition: Boolean) {
        mCanChangePosition = canChangePosition
    }

    /**
     * 是否在竖屏模式下开始手势控制，默认关闭
     */
    fun setEnableInNormal(enableInNormal: Boolean) {
        mEnableInNormal = enableInNormal
    }

    /**
     * 是否开启手势控制，默认开启，关闭之后，手势调节进度，音量，亮度功能将关闭
     */
    fun setGestureEnabled(gestureEnabled: Boolean) {
        mIsGestureEnabled = gestureEnabled
    }

    /**
     * 是否开启双击播放/暂停，默认开启
     */
    fun setDoubleTapTogglePlayEnabled(enabled: Boolean) {
        mIsDoubleTapTogglePlayEnabled = enabled
    }

    override fun setPlayerState(playerState: Int) {
        super.setPlayerState(playerState)
        mCanSlide = when (playerState) {
            VideoView.PLAYER_NORMAL -> mEnableInNormal
            VideoView.PLAYER_FULL_SCREEN -> true
            else -> mCanSlide
        }
    }

    override fun setPlayState(playState: Int) {
        super.setPlayState(playState)
        mCurPlayState = playState
    }

    protected fun isInPlaybackState(): Boolean {
        return mControlWrapper != null
            && mCurPlayState != VideoView.STATE_ERROR
            && mCurPlayState != VideoView.STATE_IDLE
            && mCurPlayState != VideoView.STATE_PREPARING
            && mCurPlayState != VideoView.STATE_PREPARED
            && mCurPlayState != VideoView.STATE_START_ABORT
            && mCurPlayState != VideoView.STATE_PLAYBACK_COMPLETED
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return mGestureDetector?.onTouchEvent(event) ?: false
    }

    /**
     * 手指按下的瞬间
     */
    override fun onDown(e: MotionEvent): Boolean {
        if (!isInPlaybackState()
            || !mIsGestureEnabled
            || PlayerUtils.isEdge(context, e)
        ) {
            return true
        }
        mStreamVolume = mAudioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val activity = PlayerUtils.scanForActivity(context)
        mBrightness = activity?.window?.attributes?.screenBrightness ?: 0f
        mFirstTouch = true
        mChangePosition = false
        mChangeBrightness = false
        mChangeVolume = false
        return true
    }

    /**
     * 单击
     */
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (isInPlaybackState()) {
            mControlWrapper?.toggleShowState()
        }
        return true
    }

    /**
     * 双击
     */
    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (mIsDoubleTapTogglePlayEnabled && !isLocked && isInPlaybackState()) {
            togglePlay()
        }
        return true
    }

    /**
     * 在屏幕上滑动
     */
    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (!isInPlaybackState()
            || !mIsGestureEnabled
            || !mCanSlide
            || isLocked
            || PlayerUtils.isEdge(context, e1)
        ) {
            return true
        }

        val deltaX = e1.x - e2.x
        val deltaY = e1.y - e2.y

        if (mFirstTouch) {
            mChangePosition = kotlin.math.abs(distanceX) >= kotlin.math.abs(distanceY)
            if (!mChangePosition) {
                val halfScreen = PlayerUtils.getScreenWidth(context, true) / 2
                if (e2.x > halfScreen) {
                    mChangeVolume = true
                } else {
                    mChangeBrightness = true
                }
            }

            if (mChangePosition) {
                mChangePosition = mCanChangePosition
            }

            if (mChangePosition || mChangeBrightness || mChangeVolume) {
                mControlComponents.entries.forEach { (component, _) ->
                    if (component is IGestureComponent) {
                        component.onStartSlide()
                    }
                }
            }
            mFirstTouch = false
        }

        when {
            mChangePosition -> slideToChangePosition(deltaX)
            mChangeBrightness -> slideToChangeBrightness(deltaY)
            mChangeVolume -> slideToChangeVolume(deltaY)
        }
        return true
    }

    protected open fun slideToChangePosition(deltaX: Float) {
        var adjustedDeltaX = -deltaX
        val width = measuredWidth
        val duration = mControlWrapper?.duration?.toInt() ?: 0
        val currentPosition = mControlWrapper?.currentPosition?.toInt() ?: 0
        var position = (adjustedDeltaX / width * 120000 + currentPosition).toInt()
        position = position.coerceIn(0, duration)

        mControlComponents.entries.forEach { (component, _) ->
            if (component is IGestureComponent) {
                component.onPositionChange(position, currentPosition, duration)
            }
        }
        updateSeekUI(currentPosition, position, duration)
        mSeekPosition = position
    }

    protected open fun updateSeekUI(curr: Int, seekTo: Int, duration: Int) {
        // Override in subclasses if needed
    }

    protected open fun slideToChangeBrightness(deltaY: Float) {
        val activity = PlayerUtils.scanForActivity(context) ?: return
        val window = activity.window
        val attributes = window.attributes
        val height = measuredHeight
        var brightness = if (mBrightness == -1.0f) 0.5f else mBrightness
        brightness = deltaY * 2 / height * 1.0f + brightness
        brightness = brightness.coerceIn(0f, 1.0f)
        val percent = (brightness * 100).toInt()
        attributes.screenBrightness = brightness
        window.attributes = attributes

        mControlComponents.entries.forEach { (component, _) ->
            if (component is IGestureComponent) {
                component.onBrightnessChange(percent)
            }
        }

        mHandler?.apply {
            val msg = Message.obtain()
            msg.what = MSG_SLIDE_INFO_SHOW
            msg.obj = "亮度${percent}%"
            sendMessage(msg)
            removeMessages(MSG_SLIDE_INFO_HIDE)
            sendEmptyMessageDelayed(MSG_SLIDE_INFO_HIDE, 1000)
        }
    }

    protected open fun slideToChangeVolume(deltaY: Float) {
        val audioManager = mAudioManager ?: return
        val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val height = measuredHeight
        val deltaV = deltaY * 2 / height * streamMaxVolume
        var index = mStreamVolume + deltaV
        index = index.coerceIn(0f, streamMaxVolume.toFloat())
        val percent = (index / streamMaxVolume * 100).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index.toInt(), 0)

        mControlComponents.entries.forEach { (component, _) ->
            if (component is IGestureComponent) {
                component.onVolumeChange(percent)
            }
        }

        mHandler?.apply {
            val msg = Message.obtain()
            msg.what = MSG_SLIDE_INFO_SHOW
            msg.obj = "音量${percent}%"
            sendMessage(msg)
            removeMessages(MSG_SLIDE_INFO_HIDE)
            sendEmptyMessageDelayed(MSG_SLIDE_INFO_HIDE, 1000)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mGestureDetector?.onTouchEvent(event) == false) {
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    stopSlide()
                    if (mSeekPosition > 0) {
                        mControlWrapper?.seekTo(mSeekPosition.toLong())
                        mSeekPosition = 0
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    stopSlide()
                    mSeekPosition = 0
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun stopSlide() {
        mControlComponents.entries.forEach { (component, _) ->
            if (component is IGestureComponent) {
                component.onStopSlide()
            }
        }
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false

    override fun onLongPress(e: MotionEvent) {
        // Override in subclasses if needed
    }

    override fun onShowPress(e: MotionEvent) {
        // No-op
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean = false

    override fun onSingleTapUp(e: MotionEvent): Boolean = false

    open fun onKeyEvent(event: KeyEvent): Boolean = false

    companion object {
        private const val MSG_SLIDE_INFO_SHOW = 100
        private const val MSG_SLIDE_INFO_HIDE = 101
    }
}
