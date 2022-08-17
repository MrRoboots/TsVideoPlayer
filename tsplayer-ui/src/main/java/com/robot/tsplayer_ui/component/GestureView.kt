package com.robot.tsplayer_ui.component

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.*
import com.robot.tsplayer_kotlin.controller.ControlWrapper
import com.robot.tsplayer_kotlin.controller.IGestureComponent
import com.robot.tsplayer_kotlin.player.BaseVideoView
import com.robot.tsplayer_kotlin.utils.PlayerUtils.Companion.stringForTime
import com.robot.tsplayer_ui.R

class GestureView : FrameLayout, IGestureComponent {
    private var mControlWrapper: ControlWrapper? = null

    private var mIcon: ImageView? = null
    private var mProgressPercent: ProgressBar? = null
    private var mTextPercent: TextView? = null

    private var mCenterContainer: LinearLayout

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        visibility = GONE
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_gesture_control_view, this, true)
        mIcon = findViewById(R.id.iv_icon)
        mProgressPercent = findViewById(R.id.pro_percent)
        mTextPercent = findViewById(R.id.tv_percent)
        mCenterContainer = findViewById(R.id.center_container)
    }

    /**
     * 开始滑动
     */
    override fun onStartSlide() {
        mControlWrapper?.hide()
        mCenterContainer.visibility = VISIBLE
        mCenterContainer.alpha = 1f
    }

    /**
     * 结束滑动
     */
    override fun onStopSlide() {
        mCenterContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mCenterContainer.visibility = GONE
                }
            }).start()

        //exo播放器会在缓冲完后调用STATE_BUFFERED从而触发 startProgress刷新进度
        //androidMediaPlayer 没有这个回调
        mControlWrapper?.startProgress()
    }

    /**
     * pos滑动进度
     * currentPosition当前进度
     * duration总进度
     */
    override fun onPositionChange(pos: Int, currentPosition: Int, duration: Int) {
        mProgressPercent?.visibility = GONE

        if (pos > currentPosition) {
            mIcon?.setImageResource(R.drawable.tsplayer_ic_action_fast_forward)
        } else {
            mIcon?.setImageResource(R.drawable.tsplayer_ic_action_fast_rewind)
        }
        mTextPercent?.text = String.format("%s/%s", stringForTime(pos), stringForTime(duration))
    }

    /**
     * 调节亮度
     * percent亮度百分比
     */
    @SuppressLint("SetTextI18n")
    override fun onBrightnessChange(percent: Int) {
        mProgressPercent?.visibility = VISIBLE
        mIcon?.setImageResource(R.drawable.tsplayer_ic_action_brightness)
        mTextPercent?.text = "$percent%"
        mProgressPercent?.progress = percent
    }

    /**
     * 调节声音
     * percent声音百分比
     */
    @SuppressLint("SetTextI18n")
    override fun onVolumeChange(percent: Int) {
        mProgressPercent?.visibility = VISIBLE
        if (percent <= 0) {
            mIcon?.setImageResource(R.drawable.tsplayer_ic_action_volume_off)
        } else {
            mIcon?.setImageResource(R.drawable.tsplayer_ic_action_volume_up)
        }
        mTextPercent?.text = "$percent%"
        mProgressPercent?.progress = percent
    }

    /**
     * 锁定状态改变 不响应手势状态 ui隐藏等
     */
    override fun onLockStateChanged(locked: Boolean) {
    }

    /**
     * 回调控制器显示和隐藏状态
     */
    override fun onVisibilityChanged(mShowing: Boolean, anim: Animation?) {
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
        visibility =
            if (playState == BaseVideoView.STATE_IDLE
                || playState == BaseVideoView.STATE_START_ABORT
                || playState == BaseVideoView.STATE_PREPARING
                || playState == BaseVideoView.STATE_PREPARED
                || playState == BaseVideoView.STATE_ERROR
                || playState == BaseVideoView.STATE_PLAYBACK_COMPLETED
            ) {
                GONE
            } else {
                VISIBLE
            }
    }

    /**
     * 全屏/非全屏回调
     */
    override fun onPlayerStateChange(playerState: Int) {
    }

}