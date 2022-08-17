package com.robot.tsplayer_ui.component

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.robot.tsplayer_kotlin.controller.ControlWrapper
import com.robot.tsplayer_kotlin.controller.IControlComponent
import com.robot.tsplayer_kotlin.player.BaseVideoView
import com.robot.tsplayer_kotlin.utils.PlayerUtils
import com.robot.tsplayer_ui.R

/**
 * 直播底部控制栏
 */
class LiveControlView : FrameLayout, IControlComponent, View.OnClickListener {
    private var mControlWrapper: ControlWrapper? = null

    private var mFullScreen: ImageView? = null
    private var mBottomContainer: LinearLayout? = null
    private var mPlayButton: ImageView? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        visibility = GONE
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_live_control_view, this, true)
        mFullScreen = findViewById<ImageView>(R.id.fullscreen)
        mFullScreen?.setOnClickListener(this)
        mBottomContainer = findViewById<LinearLayout>(R.id.bottom_container)
        mPlayButton = findViewById<ImageView>(R.id.iv_play)
        mPlayButton?.setOnClickListener(this)
        val refresh = findViewById<ImageView>(R.id.iv_refresh)
        refresh.setOnClickListener(this)
    }

    /**
     * 锁定状态改变 不响应手势状态 ui隐藏等
     */
    override fun onLockStateChanged(locked: Boolean) {
        onVisibilityChanged(!locked, null)
    }

    /**
     * 回调控制器显示和隐藏状态
     */
    override fun onVisibilityChanged(mShowing: Boolean, anim: Animation?) {
        if (mShowing) {
            if (visibility == GONE) {
                visibility = VISIBLE
                if (anim != null) {
                    startAnimation(anim)
                }
            }
        } else {
            if (visibility == VISIBLE) {
                visibility = GONE
                if (anim != null) {
                    startAnimation(anim)
                }
            }
        }
    }

    /**
     * 绑定ControlComponent和Controller
     */
    override fun attach(mControlWrapper: ControlWrapper) {
        this.mControlWrapper = mControlWrapper
    }

    /**
     * 进度回调
     */
    override fun setProgress(currentPosition: Int, duration: Int) {
    }

    /**
     * 获取控制ui view
     */
    override fun getView(): View {
        return this
    }

    /**
     * 播放器生命周期回调
     */
    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            BaseVideoView.STATE_IDLE,
            BaseVideoView.STATE_START_ABORT,
            BaseVideoView.STATE_PREPARING,
            BaseVideoView.STATE_PREPARED,
            BaseVideoView.STATE_ERROR,
            BaseVideoView.STATE_PLAYBACK_COMPLETED ->
                visibility = GONE
            BaseVideoView.STATE_PLAYING -> mPlayButton?.isSelected = true
            BaseVideoView.STATE_PAUSED -> mPlayButton?.isSelected = false
            BaseVideoView.STATE_BUFFERING, BaseVideoView.STATE_BUFFERED ->
                mPlayButton?.isSelected = mControlWrapper?.isPlaying() == true
        }
    }

    /**
     * 全屏/非全屏回调
     */
    override fun onPlayerStateChange(playerState: Int) {
        when (playerState) {
            BaseVideoView.PLAYER_NORMAL -> mFullScreen?.isSelected = false
            BaseVideoView.PLAYER_FULL_SCREEN -> mFullScreen?.isSelected = true
        }

        val activity: Activity? = PlayerUtils.scanForActivity(context)
        if (activity != null && mControlWrapper!!.hasCutout()) {
            val orientation = activity.requestedOrientation
            val cutoutHeight = mControlWrapper!!.getCutoutHeight()
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                mBottomContainer?.setPadding(0, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                mBottomContainer?.setPadding(cutoutHeight, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                mBottomContainer?.setPadding(0, 0, cutoutHeight, 0)
            }
        }
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.fullscreen) {
            toggleFullScreen()
        } else if (id == R.id.iv_play) {
            mControlWrapper?.togglePlay()
        } else if (id == R.id.iv_refresh) {
            mControlWrapper?.replay(true)
        }
    }


    private fun toggleFullScreen() {
        val activity = PlayerUtils.scanForActivity(context)
        mControlWrapper?.toggleFullScreen(activity)
    }
}