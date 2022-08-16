package com.robot.tsplayer_ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.TextView
import com.robot.tsplayer_kotlin.controller.ControlWrapper
import com.robot.tsplayer_kotlin.controller.IControlComponent
import com.robot.tsplayer_kotlin.player.BaseVideoView
import kotlin.math.abs

/**
 * 播放错误页面
 */
class ErrorView : FrameLayout, IControlComponent {

    lateinit var mControlWrapper: ControlWrapper
    private var mDownX: Float = 0f
    private var mDownY: Float = 0f

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        visibility = GONE
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_error_view, this, true)
        findViewById<TextView>(R.id.status_btn).setOnClickListener {
            visibility = GONE
            mControlWrapper.replay(false)
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
        if (playState == BaseVideoView.STATE_ERROR) {
            bringToFront()
            visibility = VISIBLE
        } else if (playState == BaseVideoView.STATE_IDLE) {
            visibility = GONE
        }
    }

    /**
     * 全屏/非全屏回调
     */
    override fun onPlayerStateChange(playerState: Int) {

    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = ev.x
                mDownY = ev.y
                //父类不拦截此事件
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = abs(ev.x - mDownX)
                val deltaY = abs(ev.y - mDownY)
                if (deltaX > ViewConfiguration.get(context).scaledTouchSlop
                    || deltaY > ViewConfiguration.get(context).scaledTouchSlop
                ) {
                    //父类拦截事件
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            MotionEvent.ACTION_UP -> {
            }
        }
        return super.dispatchTouchEvent(ev)
    }

}