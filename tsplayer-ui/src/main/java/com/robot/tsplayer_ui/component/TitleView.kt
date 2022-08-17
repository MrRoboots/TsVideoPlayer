package com.robot.tsplayer_ui.component

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.robot.tsplayer_kotlin.controller.ControlWrapper
import com.robot.tsplayer_kotlin.controller.IControlComponent
import com.robot.tsplayer_kotlin.player.BaseVideoView
import com.robot.tsplayer_kotlin.utils.PlayerUtils
import com.robot.tsplayer_ui.R

/**
 * 标题界面
 */
class TitleView : FrameLayout, IControlComponent {
    private var mControlWrapper: ControlWrapper? = null

    private var mTitleContainer: LinearLayout? = null
    private var mTitle: TextView? = null
    private var mSysTime: TextView? = null //系统当前时间

    private var mBatteryReceiver: BatteryReceiver? = null
    private var mIsRegister = false //是否注册BatteryReceiver

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        visibility = GONE
        LayoutInflater.from(context).inflate(R.layout.dkplayer_layout_title_view, this, true)
        mTitleContainer = findViewById<LinearLayout>(R.id.title_container)
        val back = findViewById<ImageView>(R.id.back)
        back.setOnClickListener {
            val activity: Activity? = PlayerUtils.scanForActivity(context)
            if (activity != null && mControlWrapper?.isFullScreen() == true) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                mControlWrapper?.stopFullScreen()
            }
        }
        mTitle = findViewById<TextView>(R.id.title)
        mSysTime = findViewById<TextView>(R.id.sys_time)
        val batteryLevel = findViewById<ImageView>(R.id.iv_battery)
        mBatteryReceiver = BatteryReceiver(batteryLevel)
    }

    fun setTitle(title: String?) {
        mTitle?.text = title
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mIsRegister) {
            context.unregisterReceiver(mBatteryReceiver)
            mIsRegister = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!mIsRegister) {
            context.registerReceiver(mBatteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            mIsRegister = true
        }
    }

    override fun onLockStateChanged(locked: Boolean) {
        if (locked) {
            visibility = GONE
        } else {
            visibility = VISIBLE
            mSysTime?.text = PlayerUtils.getCurrentSystemTime()
        }
    }

    override fun onVisibilityChanged(mShowing: Boolean, anim: Animation?) {
        //只在全屏时才有效
        if (!mControlWrapper?.isFullScreen()!!) return
        if (mShowing) {
            if (visibility == GONE) {
                mSysTime?.text = PlayerUtils.getCurrentSystemTime()
                visibility = VISIBLE
                anim?.let { startAnimation(it) }
            }
        } else {
            if (visibility == VISIBLE) {
                visibility = GONE
                anim?.let { startAnimation(it) }
            }
        }
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
            BaseVideoView.STATE_IDLE,
            BaseVideoView.STATE_START_ABORT,
            BaseVideoView.STATE_PREPARING,
            BaseVideoView.STATE_PREPARED,
            BaseVideoView.STATE_ERROR,
            BaseVideoView.STATE_PLAYBACK_COMPLETED -> visibility = GONE
        }
    }

    override fun onPlayerStateChange(playerState: Int) {
        if (playerState == BaseVideoView.PLAYER_FULL_SCREEN) {
            if (mControlWrapper?.isShowing() == true && !mControlWrapper!!.isLocked()) {
                visibility = VISIBLE
                mSysTime?.text = PlayerUtils.getCurrentSystemTime()
            }
            mTitle?.isSelected = true
        } else {
            visibility = GONE
            mTitle?.isSelected = false
        }

        val activity = PlayerUtils.scanForActivity(context)
        if (activity != null && mControlWrapper!!.hasCutout()) {
            val orientation = activity.requestedOrientation
            val cutoutHeight = mControlWrapper?.getCutoutHeight() ?: 0
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                mTitleContainer?.setPadding(0, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                mTitleContainer?.setPadding(cutoutHeight, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                mTitleContainer?.setPadding(0, 0, cutoutHeight, 0)
            }
        }
    }

    private class BatteryReceiver(private val pow: ImageView) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val extras = intent.extras ?: return
            val current = extras.getInt("level") // 获得当前电量
            val total = extras.getInt("scale") // 获得总电量
            val percent = current * 100 / total
            pow.drawable.level = percent
        }
    }

}