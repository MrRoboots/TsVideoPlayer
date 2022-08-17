package com.robot.tsplayer_ui.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import com.robot.tsplayer_kotlin.controller.ControlWrapper
import com.robot.tsplayer_kotlin.controller.IControlComponent
import com.robot.tsplayer_kotlin.player.BaseVideoView
import com.robot.tsplayer_kotlin.player.VideoViewManager
import com.robot.tsplayer_ui.R

/**
 * 准备界面
 */
class PrepareView : FrameLayout, IControlComponent {
    private var mControlWrapper: ControlWrapper? = null

    private var mThumb: ImageView? = null
    private var mStartPlay: ImageView? = null
    private var mLoading: ProgressBar? = null
    private var mNetWarning: FrameLayout? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_prepare_view, this, true)
        mThumb = findViewById<ImageView>(R.id.thumb)
        mStartPlay = findViewById<ImageView>(R.id.start_play)
        mLoading = findViewById<ProgressBar>(R.id.loading)
        mNetWarning = findViewById<FrameLayout>(R.id.net_warning_layout)
        findViewById<View>(R.id.status_btn).setOnClickListener {
            mNetWarning?.visibility = GONE
            VideoViewManager.getInstance()?.setPlayOnMobileNetwork(true)
            mControlWrapper?.start()
        }
    }

    /**
     * 设置点击此界面开始播放
     */
    fun setClickStart() {
        setOnClickListener { mControlWrapper?.start() }
    }

    override fun onLockStateChanged(locked: Boolean) {
    }

    override fun onVisibilityChanged(mShowing: Boolean, anim: Animation?) {
    }

    override fun attach(mControlWrapper: ControlWrapper) {
        this.mControlWrapper = mControlWrapper
    }

    override fun setProgress(currentPosition: Int, duration: Int) {
    }

    override fun getView(): View {
        return this
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            BaseVideoView.STATE_PREPARING -> {
                bringToFront()
                visibility = VISIBLE
                mStartPlay?.visibility = GONE
                mNetWarning?.visibility = GONE
                mLoading?.visibility = VISIBLE
            }
            BaseVideoView.STATE_PLAYING,
            BaseVideoView.STATE_PAUSED,
            BaseVideoView.STATE_ERROR,
            BaseVideoView.STATE_BUFFERING,
            BaseVideoView.STATE_BUFFERED,
            BaseVideoView.STATE_PLAYBACK_COMPLETED -> visibility = GONE
            BaseVideoView.STATE_IDLE -> {
                visibility = VISIBLE
                bringToFront()
                mLoading?.visibility = GONE
                mNetWarning?.visibility = GONE
                mStartPlay?.visibility = VISIBLE
                mThumb?.visibility = VISIBLE
            }
            BaseVideoView.STATE_START_ABORT -> {
                visibility = VISIBLE
                mNetWarning?.visibility = VISIBLE
                mNetWarning?.bringToFront()
            }
        }
    }

    override fun onPlayerStateChange(playerState: Int) {
    }
}