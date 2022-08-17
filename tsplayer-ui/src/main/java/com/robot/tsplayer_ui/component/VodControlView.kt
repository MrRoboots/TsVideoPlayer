package com.robot.tsplayer_ui.component

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import com.robot.tsplayer_kotlin.controller.ControlWrapper
import com.robot.tsplayer_kotlin.controller.IControlComponent
import com.robot.tsplayer_kotlin.player.BaseVideoView
import com.robot.tsplayer_kotlin.utils.PlayerUtils
import com.robot.tsplayer_kotlin.utils.PlayerUtils.Companion.stringForTime
import com.robot.tsplayer_ui.R

/**
 * 底部控制界面
 */
class VodControlView : FrameLayout, IControlComponent, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    lateinit var mControlWrapper: ControlWrapper
    private var mTotalTime: TextView? = null
    private var mCurrTime: TextView? = null
    private var mFullScreen: ImageView? = null
    private var mBottomContainer: LinearLayout? = null
    private var mVideoProgress: SeekBar? = null
    private var mBottomProgress: ProgressBar? = null
    private var mPlayButton: ImageView? = null

    private var mIsDragging = false

    private var mIsShowBottomProgress = true

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        visibility = GONE
        LayoutInflater.from(context).inflate(getLayoutId(), this, true)
        mFullScreen = findViewById<ImageView>(R.id.fullscreen)
        mFullScreen?.setOnClickListener(this)
        mBottomContainer = findViewById<LinearLayout>(R.id.bottom_container)
        mVideoProgress = findViewById<SeekBar>(R.id.seekBar)
        mVideoProgress?.setOnSeekBarChangeListener(this)
        mTotalTime = findViewById<TextView>(R.id.total_time)
        mCurrTime = findViewById<TextView>(R.id.curr_time)
        mPlayButton = findViewById<ImageView>(R.id.iv_play)
        mPlayButton?.setOnClickListener(this)
        mBottomProgress = findViewById<ProgressBar>(R.id.bottom_progress)

        //5.1以下系统SeekBar高度需要设置成WRAP_CONTENT
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mVideoProgress?.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    private fun getLayoutId(): Int {
        return R.layout.dkplayer_layout_vod_control_view
    }


    override fun onLockStateChanged(locked: Boolean) {
        onVisibilityChanged(!locked, null)
    }

    /**
     * 是否显示底部进度条，默认显示
     */
    fun showBottomProgress(isShow: Boolean) {
        mIsShowBottomProgress = isShow
    }

    override fun onVisibilityChanged(mShowing: Boolean, anim: Animation?) {
        if (mShowing) {
            mBottomContainer?.visibility = VISIBLE
            if (anim != null) {
                mBottomContainer?.startAnimation(anim)
            }
            if (mIsShowBottomProgress) {
                mBottomProgress?.visibility = GONE
            }
        } else {
            mBottomContainer?.visibility = GONE
            if (anim != null) {
                mBottomContainer?.startAnimation(anim)
            }
            if (mIsShowBottomProgress) {
                mBottomProgress?.visibility = VISIBLE
                val animation = AlphaAnimation(0f, 1f)
                animation.duration = 300
                mBottomProgress?.startAnimation(animation)
            }
        }
    }

    override fun attach(mControlWrapper: ControlWrapper) {
        this.mControlWrapper = mControlWrapper
    }

    override fun setProgress(currentPosition: Int, duration: Int) {
        if (mIsDragging) {
            return
        }

        if (duration > 0) {
            mVideoProgress?.isEnabled = true
            val pos: Int = (currentPosition * 1.0 / duration * mVideoProgress?.max!!).toInt()
            mVideoProgress?.progress = pos
            mBottomProgress?.progress = pos
        } else {
            mVideoProgress?.isEnabled = false
        }
        val percent = mControlWrapper.getBufferedPercentage()
        if (percent >= 95) { //解决缓冲进度不能100%问题
            mVideoProgress?.secondaryProgress = mVideoProgress?.max!!
            mBottomProgress?.secondaryProgress = mBottomProgress?.max!!
        } else {
            mVideoProgress?.secondaryProgress = percent * 10
            mBottomProgress?.secondaryProgress = percent * 10
        }

        mTotalTime?.let {
            it.text = stringForTime(duration)
        }

        mCurrTime?.let {
            it.text = stringForTime(currentPosition)
        }
    }

    override fun getView(): View {
        return this
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            BaseVideoView.STATE_IDLE,
            BaseVideoView.STATE_PLAYBACK_COMPLETED -> {
                visibility = GONE
                mBottomProgress?.progress = 0
                mBottomProgress?.secondaryProgress = 0
                mVideoProgress?.progress = 0
                mVideoProgress?.secondaryProgress = 0
            }
            BaseVideoView.STATE_START_ABORT,
            BaseVideoView.STATE_PREPARING,
            BaseVideoView.STATE_PREPARED,
            BaseVideoView.STATE_ERROR -> visibility = GONE
            BaseVideoView.STATE_PLAYING -> {
                mPlayButton?.isSelected = true
                if (mIsShowBottomProgress) {
                    if (mControlWrapper.isShowing()) {
                        mBottomProgress?.visibility = GONE
                        mBottomContainer?.visibility = VISIBLE
                    } else {
                        mBottomContainer?.visibility = GONE
                        mBottomProgress?.visibility = VISIBLE
                    }
                } else {
                    mBottomContainer?.visibility = GONE
                }
                visibility = VISIBLE
                //开始刷新进度
                mControlWrapper.startProgress()
            }
            BaseVideoView.STATE_PAUSED -> mPlayButton?.isSelected = false
            BaseVideoView.STATE_BUFFERING -> {
                mPlayButton?.isSelected = mControlWrapper.isPlaying()
                // 停止刷新进度
                mControlWrapper.stopProgress()
            }
            BaseVideoView.STATE_BUFFERED -> {
                mPlayButton?.isSelected = mControlWrapper.isPlaying()
                //开始刷新进度
                mControlWrapper.startProgress()
            }
        }
    }

    override fun onPlayerStateChange(playerState: Int) {
        when (playerState) {
            BaseVideoView.PLAYER_NORMAL -> mFullScreen?.isSelected = false
            BaseVideoView.PLAYER_FULL_SCREEN -> mFullScreen?.isSelected = true
        }

        val activity = PlayerUtils.scanForActivity(context)
        if (activity != null && mControlWrapper.hasCutout()) {
            val orientation = activity.requestedOrientation
            val cutoutHeight = mControlWrapper.getCutoutHeight()
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                mBottomContainer?.setPadding(0, 0, 0, 0)
                mBottomProgress?.setPadding(0, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                mBottomContainer?.setPadding(cutoutHeight, 0, 0, 0)
                mBottomProgress?.setPadding(cutoutHeight, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                mBottomContainer?.setPadding(0, 0, cutoutHeight, 0)
                mBottomProgress?.setPadding(0, 0, cutoutHeight, 0)
            }
        }
    }

    override fun onClick(v: View?) {
        val id = v!!.id
        if (id == R.id.fullscreen) {
            toggleFullScreen()
        } else if (id == R.id.iv_play) {
            mControlWrapper.togglePlay()
        }
    }

    private fun toggleFullScreen() {
        val activity: Activity? = PlayerUtils.scanForActivity(context)
        mControlWrapper.toggleFullScreen(activity)
        // 下面方法会根据适配宽高决定是否旋转屏幕
//        mControlWrapper.toggleFullScreenByVideoSize(activity);
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        mIsDragging = true
        mControlWrapper.stopProgress()
        mControlWrapper.stopFadeOut()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        val duration = mControlWrapper.getDuration()
        val newPosition = duration * seekBar.progress / mVideoProgress?.max!!
        mControlWrapper.seekTo(newPosition.toInt().toLong())
        mIsDragging = false
        mControlWrapper.startProgress()
        mControlWrapper.startFadeOut()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) {
            return
        }
        val duration = mControlWrapper.getDuration()
        val newPosition = duration * progress / mVideoProgress?.max!!
        if (mCurrTime != null) mCurrTime?.text = stringForTime(newPosition.toInt())
    }
}