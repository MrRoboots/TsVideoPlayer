package com.robot.tsplayer_ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.LayoutInflaterCompat
import androidx.core.view.setMargins
import com.robot.tsplayer_kotlin.controller.ControlWrapper
import com.robot.tsplayer_kotlin.controller.IControlComponent
import com.robot.tsplayer_kotlin.player.BaseVideoView
import com.robot.tsplayer_kotlin.utils.PlayerUtils

/**
 * 播放完成界面显示
 */
class CompleteView : FrameLayout, IControlComponent {
    private var mControlWrapper: ControlWrapper? = null
    private lateinit var mBack: ImageView

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        visibility = View.GONE
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_complete_view, this, true)
        findViewById<ImageView>(R.id.stop_fullscreen).setOnClickListener() {
            mControlWrapper?.replay(true)
        }
        mBack = findViewById<ImageView>(R.id.stop_fullscreen)
        mBack.setOnClickListener {
            if (mControlWrapper?.isFullScreen() == true) {
                val activity = PlayerUtils.scanForActivity(context)
                if (activity != null && !activity.isFinishing) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    mControlWrapper?.stopFullScreen()
                }
            }
        }
        isClickable = true
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
        if (playState == BaseVideoView.STATE_PLAYBACK_COMPLETED) {
            visibility = VISIBLE
            mBack.visibility = if (mControlWrapper?.isFullScreen() == true) {
                VISIBLE
            } else {
                GONE
            }
            //移动到最上面 然后重新绘制
            bringToFront()
        } else {
            visibility = GONE
        }
    }

    /**
     * 全屏/非全屏回调
     */
    @SuppressLint("SwitchIntDef")
    override fun onPlayerStateChange(playerState: Int) {
        if (playerState == BaseVideoView.PLAYER_FULL_SCREEN) {
            mBack.visibility = VISIBLE
        } else if (playerState == BaseVideoView.PLAYER_NORMAL) {
            mBack.visibility = GONE
        }

        val activity = PlayerUtils.scanForActivity(context)
        if (activity != null && !activity.isFinishing) {
            val orientation = activity.requestedOrientation
            val layoutParams = mBack.layoutParams as LayoutParams
            val cutoutHeight = mControlWrapper?.getCutoutHeight() ?: 0

            when (orientation) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> {
                    layoutParams.setMargins(0, 0, 0, 0)
                }
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> {
                    layoutParams.setMargins(cutoutHeight, 0, 0, 0)
                }
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> {
                    layoutParams.setMargins(0, 0, 0, 0)
                }
            }
        }
    }
}