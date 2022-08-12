package com.robot.tsplayer_kotlin.controller

import android.content.Context
import android.media.AudioManager
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.robot.tsplayer_kotlin.player.BaseVideoView
import com.robot.tsplayer_kotlin.utils.PlayerUtils
import kotlin.math.abs

abstract class GestureVideoController : BaseVideoController, GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener, View.OnTouchListener {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var mGestureDetector: GestureDetector? = null
    private var mAudioManager: AudioManager? = null
    private var mIsGestureEnabled = true
    private var mStreamVolume = 0
    private var mBrightness = 0f
    private var mSeekPosition = -1
    private var mFirstTouch = false
    private var mChangePosition = false
    private var mChangeBrightness = false
    private var mChangeVolume = false

    private var mCanChangePosition = true

    private var mEnableInNormal = true

    private var mCanSlide = false

    private var mCurPlayState = 0

    private var mIsDoubleTapTogglePlayEnabled = true

    override fun initView() {
        super.initView()
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mGestureDetector = GestureDetector(context, this)
        setOnTouchListener(this)
    }

    /**
     * 设置是否可以滑动调节进度，默认可以
     */
    open fun setCanChangePosition(canChangePosition: Boolean) {
        mCanChangePosition = canChangePosition
    }

    /**
     * 是否在竖屏模式下开始手势控制，默认开启
     */
    open fun setEnableInNormal(enableInNormal: Boolean) {
        mEnableInNormal = enableInNormal
    }

    /**
     * 是否开启手势控制，默认开启，关闭之后，手势调节进度，音量，亮度功能将关闭
     */
    open fun setGestureEnabled(gestureEnabled: Boolean) {
        mIsGestureEnabled = gestureEnabled
    }

    /**
     * 是否开启双击播放/暂停，默认开启
     */
    open fun setDoubleTapTogglePlayEnabled(enabled: Boolean) {
        mIsDoubleTapTogglePlayEnabled = enabled
    }

    override fun setPlayerState(playerState: Int) {
        super.setPlayerState(playerState)
        if (playerState == BaseVideoView.PLAYER_NORMAL) {
            mCanSlide = mEnableInNormal
        } else if (playerState == BaseVideoView.PLAYER_FULL_SCREEN) {
            mCanSlide = true
        }
    }

    override fun setPlayState(playState: Int) {
        super.setPlayState(playState)
        mCurPlayState = playState
    }

    open fun isInPlaybackState(): Boolean {
        return mControlWrapper != null
                && mCurPlayState != BaseVideoView.STATE_ERROR
                && mCurPlayState != BaseVideoView.STATE_IDLE
                && mCurPlayState != BaseVideoView.STATE_PREPARING
                && mCurPlayState != BaseVideoView.STATE_PREPARED
                && mCurPlayState != BaseVideoView.STATE_START_ABORT
                && mCurPlayState != BaseVideoView.STATE_PLAYBACK_COMPLETED
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return mGestureDetector?.onTouchEvent(event) ?: false
    }

    /**
     * 手指按下瞬间
     */
    override fun onDown(e: MotionEvent): Boolean {
        if (!isInPlaybackState() ||//没在播放周期
            !mIsGestureEnabled || //没有打开手势开关
            PlayerUtils.isEdge(context, e) //屏幕边缘
        ) return true

        //声音
        mStreamVolume = mAudioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        //亮度
        val mActivity = PlayerUtils.scanForActivity(context)
        mBrightness = mActivity?.window?.attributes?.screenBrightness ?: 0f
        mFirstTouch = true
        mChangePosition = false
        mChangeBrightness = false
        mChangeVolume = false
        return true
    }

    /**
     * 单击
     */
    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        if (isInPlaybackState()) {
            mControlWrapper?.toggleShowState()
        }
        return true
    }

    /**
     * 双击
     */
    override fun onDoubleTap(e: MotionEvent?): Boolean {
        if (mIsDoubleTapTogglePlayEnabled && !isLocked() && isInPlaybackState()) mControlWrapper?.togglePlay()
        return true
    }


    /**
     * 滑动事件
     */
    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (!isInPlaybackState() //不处于播放状态
            || !mIsGestureEnabled //关闭了手势
            || !mCanSlide //关闭了滑动手势
            || isLocked() //锁住了屏幕
            || PlayerUtils.isEdge(context, e1)//处于屏幕边沿
        )
            return true


        val deltaX = e1.x - e2.x
        val deltaY = e1.y - e2.y

        if (mFirstTouch) {
            //判断方向
            mChangePosition = abs(distanceX) >= abs(distanceY)

            //x > y 横 true
            //x < y 竖 false
            //竖屏滑动
            if (!mChangeBrightness) {
                //获取屏幕一半
                val hasMid = PlayerUtils.getScreenWidth(context, true) / 2
                if (e2.x > hasMid) {
                    //右边
                    mChangeVolume = true
                } else {
                    //左边
                    mChangeBrightness = true
                }
            }

            if (mChangePosition) {
                //根据用户设置是否可以滑动调节进度来决定最终是否可以滑动调节进度
                mChangePosition = mCanChangePosition
            }

            if (mChangePosition || mChangeBrightness || mChangeVolume) {
                for (component: Map.Entry<IControlComponent, Boolean?> in mControlComponents.entries) {
                    if (component is IGestureComponent) {
                        (component as IGestureComponent).onStartSlide()
                    }
                }
            }

            mFirstTouch = false
        }

        if (mChangePosition) {
            slideToChangePosition(deltaX)
        } else if (mChangeBrightness) {
            slideToChangeBrightness(deltaY)
        } else if (mChangeVolume) {
            slideToChangeVolume(deltaY)
        }
        return true
    }

    /**
     * 调节声音
     */
    private fun slideToChangeVolume(deltaY: Float) {
        val streamMaxVolume = mAudioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val height = measuredHeight
        val deltaV = deltaY * 2 / height * streamMaxVolume
        var index = mStreamVolume + deltaV
        if (index > streamMaxVolume) index = streamMaxVolume.toFloat()
        if (index < 0) index = 0f
        val percent = (index / streamMaxVolume * 100).toInt()
        mAudioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, index.toInt(), 0)
        for (component: Map.Entry<IControlComponent, Boolean> in mControlComponents.entries) {
            if (component is IGestureComponent) {
                (component as IGestureComponent).onVolumeChange(percent)
            }
        }
    }

    /**
     * 调节亮度
     */
    private fun slideToChangeBrightness(deltaY: Float) {
        val activity = PlayerUtils.scanForActivity(context) ?: return
        val window = activity.window
        val attributes = window.attributes
        if (mBrightness == -1.0f) mBrightness = 0.5f
        var brightness = deltaY * 2 / measuredHeight + mBrightness
        if (brightness < 0) {
            brightness = 0f
        }
        if (brightness > 1.0f) brightness = 1.0f
        val percent = (brightness * 100).toInt()
        attributes.screenBrightness = brightness
        window.attributes = attributes
        for (component: Map.Entry<IControlComponent, Boolean> in mControlComponents.entries) {
            if (component is IGestureComponent) {
                (component as IGestureComponent).onBrightnessChange(percent)
            }
        }
    }


    /**
     * 拖动进度
     */
    private fun slideToChangePosition(deltaX: Float) {
        val mDeltaX = -deltaX
        val duration = mControlWrapper?.getDuration()?.toInt() ?: 0
        val currentPosition = mControlWrapper?.getCurrentPosition()?.toInt() ?: 0
        var pos = (mDeltaX / measuredWidth * 120000 + currentPosition).toInt()
        if (pos > duration) pos = duration
        if (pos < 0) pos = 0
        for (component: Map.Entry<IControlComponent, Boolean> in mControlComponents.entries) {
            if (component is IGestureComponent) {
                (component as IGestureComponent).onPositionChange(pos, currentPosition, duration)
            }
        }
        mSeekPosition = pos
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //滑动结束时事件处理
        if (!mGestureDetector?.onTouchEvent(event)!!) {
            when (event?.action) {
                MotionEvent.ACTION_UP -> {
                    stopSlide()
                    if (mSeekPosition >= 0) {
                        mControlWrapper?.seekTo(mSeekPosition.toLong())
                        mSeekPosition = -1
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    stopSlide()
                    mSeekPosition = -1
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 停止滑动事件
     */
    private fun stopSlide() {
        for (component: Map.Entry<IControlComponent, Boolean> in mControlComponents.entries) {
            if (component is IGestureComponent) {
                (component as IGestureComponent).onStopSlide()
            }
        }
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {}

    override fun onShowPress(e: MotionEvent?) {}

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        return false
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }
}