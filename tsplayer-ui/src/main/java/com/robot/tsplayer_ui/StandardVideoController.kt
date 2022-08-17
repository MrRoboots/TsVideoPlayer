package com.robot.tsplayer_ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.AttrRes
import com.robot.tsplayer_kotlin.controller.GestureVideoController
import com.robot.tsplayer_kotlin.player.BaseVideoView
import com.robot.tsplayer_kotlin.utils.PlayerUtils.Companion.dp2px
import com.robot.tsplayer_ui.component.*

/**
 * 直播/点播控制器
 * 注意：此控制器仅做一个参考，如果想定制ui，你可以直接继承GestureVideoController或者BaseVideoController实现
 * 你自己的控制器
 */
open class StandardVideoController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : GestureVideoController(context, attrs, defStyleAttr), View.OnClickListener {


    private var mLockButton: ImageView? = null
    private var mLoadingProgress: ProgressBar? = null
    private var isBuffering = false

    override fun getLayoutId(): Int {
        return R.layout.dkplayer_layout_standard_controller
    }

    override fun initView() {
        super.initView()
        mLockButton = findViewById(R.id.lock)
        mLockButton?.setOnClickListener(this)
        mLoadingProgress = findViewById(R.id.loading)
    }

    /**
     * 快速添加各个组件
     * @param title  标题
     * @param isLive 是否为直播
     */
    fun addDefaultControlComponent(title: String?, isLive: Boolean) {
        val completeView = CompleteView(context)
        val errorView = ErrorView(context)
        val prepareView = PrepareView(context)
        prepareView.setClickStart()
        val titleView = TitleView(context)
        titleView.setTitle(title)
        addControlComponent(completeView, errorView, prepareView, titleView)
        if (isLive) {
            addControlComponent(LiveControlView(context))
        } else {
            addControlComponent(VodControlView(context))
        }
        addControlComponent(GestureView(context))
        setCanChangePosition(!isLive)
    }

    override fun onClick(v: View) {
        val i = v.id
        if (i == R.id.lock) {
            mControlWrapper?.toggleLockState()
        }
    }

    override fun onLockStateChanged(isLocked: Boolean) {
        if (isLocked) {
            mLockButton!!.isSelected = true
            Toast.makeText(context, R.string.dkplayer_locked, Toast.LENGTH_SHORT).show()
        } else {
            mLockButton!!.isSelected = false
            Toast.makeText(context, R.string.dkplayer_unlocked, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onVisibilityChanged(mShowing: Boolean, anim: Animation?) {
        if (mControlWrapper!!.isFullScreen()) {
            if (mShowing) {
                if (mLockButton!!.visibility == GONE) {
                    mLockButton!!.visibility = VISIBLE
                    if (anim != null) {
                        mLockButton!!.startAnimation(anim)
                    }
                }
            } else {
                mLockButton?.visibility = GONE
                if (anim != null) {
                    mLockButton?.startAnimation(anim)
                }
            }
        }
    }

    override fun onPlayerStateChange(playerState: Int) {
        super.onPlayerStateChange(playerState)
        when (playerState) {
            BaseVideoView.PLAYER_NORMAL -> {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                mLockButton?.visibility = GONE
            }
            BaseVideoView.PLAYER_FULL_SCREEN -> if (isShowing()) {
                mLockButton?.visibility = VISIBLE
            } else {
                mLockButton?.visibility = GONE
            }
        }
        if (mActivity != null && hasCutout()) {
            val orientation = mActivity?.requestedOrientation
            val dp24 = dp2px(context, 24f)
            val cutoutHeight = getCutoutHeight()
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                val lblp = mLockButton?.layoutParams as LayoutParams
                lblp.setMargins(dp24, 0, dp24, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                val layoutParams = mLockButton?.layoutParams as LayoutParams
                layoutParams.setMargins(dp24 + cutoutHeight, 0, dp24 + cutoutHeight, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                val layoutParams = mLockButton?.layoutParams as LayoutParams
                layoutParams.setMargins(dp24, 0, dp24, 0)
            }
        }
    }

    override fun onPlayStateChanged(playState: Int) {
        super.onPlayStateChanged(playState)
        when (playState) {
            BaseVideoView.STATE_IDLE -> {
                mLockButton?.isSelected = false
                mLoadingProgress?.visibility = GONE
            }
            BaseVideoView.STATE_PLAYING,
            BaseVideoView.STATE_PAUSED,
            BaseVideoView.STATE_PREPARED,
            BaseVideoView.STATE_ERROR,
            BaseVideoView.STATE_BUFFERED -> {
                if (playState == BaseVideoView.STATE_BUFFERED) {
                    isBuffering = false
                }
                if (!isBuffering) {
                    mLoadingProgress?.visibility = GONE
                }
            }
            BaseVideoView.STATE_PREPARING, BaseVideoView.STATE_BUFFERING -> {
                mLoadingProgress!!.visibility = VISIBLE
                if (playState == BaseVideoView.STATE_BUFFERING) {
                    isBuffering = true
                }
            }
            BaseVideoView.STATE_PLAYBACK_COMPLETED -> {
                mLoadingProgress?.visibility = GONE
                mLockButton?.visibility = GONE
                mLockButton?.isSelected = false
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (isLocked()) {
            show()
            Toast.makeText(context, R.string.dkplayer_lock_tip, Toast.LENGTH_SHORT).show()
            return true
        }
        return if (mControlWrapper?.isFullScreen() == true) {
            stopFullScreen()
        } else super.onBackPressed()
    }
}