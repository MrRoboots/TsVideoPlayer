package com.robot.tsplayer_kotlin.controller

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import com.robot.tsplayer_kotlin.player.BaseVideoView
import com.robot.tsplayer_kotlin.player.VideoViewManager
import com.robot.tsplayer_kotlin.utils.CutoutUtil
import com.robot.tsplayer_kotlin.utils.PlayerUtils

abstract class BaseVideoController : FrameLayout, IVideoController, OrientationHelper.OrientationChangeListener {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
    }

    /**
     * 播放器包装类，集合了MediaPlayerControl的api和IVideoController的api
     */
    var mControlWrapper: ControlWrapper? = null

    /**
     * 重力感应帮助类
     */
    private var mOrientationHelper: OrientationHelper? = null

    /**
     * 是否自动旋转
     */
    private var mEnableOrientation: Boolean = false

    /**
     * 是否适配刘海屏
     */
    private var mAdaptCutout: Boolean = false

    /**
     * 是否有刘海
     */
    private var mHasCutout: Boolean = false

    /**
     * 刘海的高度
     */
    private var mCutoutHeight = 0

    private lateinit var mShowAnim: Animation
    private lateinit var mHideAnim: Animation

    var mActivity: Activity? = null

    /**
     * 播放控件显示时间
     */
    private var mDefaultTimeout: Long = 4000

    /**
     * 视图是否显示
     */
    private var mShowing: Boolean = false

    /**
     * 是否是锁定状态
     */
    private var mLocked: Boolean = false

    /**
     * 是否开始刷新进度
     */
    private var mIsStartProgress = false


    /**
     * 屏幕旋转角度
     */
    private var mOrientation: Int = 0

    /**
     * 保存了所有的控制组件
     */
    var mControlComponents: LinkedHashMap<IControlComponent, Boolean> = LinkedHashMap()


    @CallSuper
    open fun initView() {
        if (getLayoutId() != 0) {
            LayoutInflater.from(context).inflate(getLayoutId(), this, true)
        }
        mOrientationHelper = OrientationHelper(context.applicationContext)
        mEnableOrientation = VideoViewManager.getConfig()?.mEnableOrientation ?: false
        mAdaptCutout = VideoViewManager.getConfig()?.mAdaptCutout ?: false

        mShowAnim = AlphaAnimation(0f, 1f)
        mShowAnim.duration = 300
        mHideAnim = AlphaAnimation(1f, 0f)
        mHideAnim.duration = 300

        mActivity = PlayerUtils.scanForActivity(context)
    }

    /**
     * 布局
     */
    abstract fun getLayoutId(): Int

    /**
     * 是否使用流量播放视频
     */
    fun showNetWarning(): Boolean {
        return PlayerUtils.getNetworkType(context) == PlayerUtils.NETWORK_MOBILE && !VideoViewManager.getInstance()
            ?.playOnMobileNetwork()!!
    }

    /**
     * 播放器生命周期
     */
    @CallSuper
    //加上 子类就必须调用 super.xxx
    open fun setPlayState(playState: Int) {
        handlePlayStateChanged(playState)
    }


    /**
     * 播放器生命周期
     */
    @CallSuper
    //加上 子类就必须调用 super.xxx
    open fun setPlayerState(playerState: Int) {
        handlePlayerStateChange(playerState)
    }


    /**
     * 改变返回键逻辑，用于activity
     */
    open fun onBackPressed(): Boolean {
        return false
    }


    /**
     * 重要：此方法用于将{@link VideoView} 和控制器绑定
     */
    fun setMediaPlayer(mediaPlayerControl: MediaPlayerControl) {
        mControlWrapper = ControlWrapper(mediaPlayerControl, this)
        //绑定ControlComponent和Controller
        mControlWrapper?.let {
            for (next in mControlComponents) {
                val component = next.key
                component.attach(it)
            }
        }
        //开始监听设备方向
        mOrientationHelper?.setOrientationChangeListener(this)
    }

    /**
     * 添加控制组件，最后面添加的在最下面，合理组织添加顺序，可让ControlComponent位于不同的层级
     */
    fun addControlComponent(vararg component: IControlComponent?) {
        for (item in component) {
            item?.let {
                addControlComponent(it, false)
            }
        }
    }

    /**
     * 添加控制组件，最后面添加的在最下面，合理组织添加顺序，可让ControlComponent位于不同的层级
     *
     * @param isDissociate 是否为游离的控制组件，
     *                     如果为 true ControlComponent 不会添加到控制器中，ControlComponent 将独立于控制器而存在，
     *                     如果为 false ControlComponent 将会被添加到控制器中，并显示出来。
     *                     为什么要让 ControlComponent 将独立于控制器而存在，假设有如下几种情况：
     *                     情况一：
     *                     如果在一个列表中控制器是复用的，但是控制器的某些部分是不能复用的，比如封面图，
     *                     此时你就可以将封面图拆分成一个游离的 ControlComponent，并把这个 ControlComponent
     *                     放在 item 的布局中，就可以实现每个item的封面图都是不一样，并且封面图可以随着播放器的状态显示和隐藏。
     *                     demo中演示的就是这种情况。
     *                     情况二：
     *                     假设有这样一种需求，播放器控制区域在显示区域的下面，此时你就可以通过自定义 ControlComponent
     *                     并将 isDissociate 设置为 true 来实现这种效果。
     */
    open fun addControlComponent(controlComponent: IControlComponent, isDissociate: Boolean) {
        mControlComponents[controlComponent] = isDissociate
        val view: View? = controlComponent.getView()
        if (view != null && !isDissociate) {
            addView(view, 0)
        }
    }

    /**
     * 移除某一个组件
     */
    fun removeControlComponent(component: IControlComponent) {
        removeView(component.getView())
        mControlComponents.remove(component)
    }

    /**
     * 移除所有控制组件
     */
    open fun removeAllControlComponent() {
        for (next: Map.Entry<IControlComponent, Boolean?> in mControlComponents) {
            removeView(next.key.getView())
        }
        mControlComponents.clear()
    }


    /**
     * 移除游离组建
     */
    open fun removeAllDissociateComponents() {
        val iterator = mControlComponents.entries.iterator()
        while (iterator.hasNext()) {
            val (_, value) = iterator.next()
            if (value)
                iterator.remove()
        }
    }

    /**
     * 自动旋转监听
     */
    @CallSuper
    override fun onOrientationChangeListener(orientation: Int) {
        if (mActivity == null || mActivity?.isFinishing == true)
            return
        //记录上一次的位置
        var lastOrientation = mOrientation

        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            //检测不到角度
            mOrientation = -1
            return
        }

        if (orientation > 350 || orientation < 10) {
            val o = mActivity?.requestedOrientation
            if (o == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && lastOrientation == 0) return
            if (mOrientation == 0) return
            //竖屏
            mOrientation = 0
            onOrientationPortrait(mActivity)
        } else if (orientation in 81..99) {
            val o = mActivity?.requestedOrientation
            if (o == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && lastOrientation == 90) return
            if (mOrientation == 90) return
            //右侧横屏
            mOrientation = 90
            onOrientationReverseLandscape(mActivity)
        } else if (orientation in 261..279) {
            val o = mActivity?.requestedOrientation
            if (o == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && lastOrientation == 270) return
            if (mOrientation == 270) return
            //左侧横屏
            mOrientation = 270
            onOrientationLandscape(mActivity)
        }

    }

    /**
     * 左侧横屏
     */
    private fun onOrientationLandscape(activity: Activity?) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        if (mControlWrapper?.isFullScreen() == true) {
            handlePlayerStateChange(BaseVideoView.PLAYER_FULL_SCREEN)
        } else {
            mControlWrapper?.startFullScreen()
        }
    }

    /**
     * 右侧横屏
     */
    private fun onOrientationReverseLandscape(activity: Activity?) {
        //反向横屏
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE

        //当前是全屏状态
        if (mControlWrapper?.isFullScreen() == true) {
            handlePlayerStateChange(BaseVideoView.PLAYER_FULL_SCREEN)
        } else {
            mControlWrapper?.startFullScreen()
        }
    }

    /**
     * 设置竖屏
     */
    private fun onOrientationPortrait(activity: Activity?) {
        //锁屏状态
        if (mLocked) return
        //没有开启设备方向监听
        if (!mEnableOrientation) return
        //切换到竖屏显示
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        //从装饰类中移除布局
        mControlWrapper?.stopFullScreen()
    }

    /**
     * 设置超时时间
     */
    fun setDefaultTimeout(defaultTimeOut: Long) {
        mDefaultTimeout = defaultTimeOut
    }

    /**
     * 当前视图是否显示
     */
    override fun isShowing(): Boolean {
        return mShowing
    }

    /**
     * 是否是锁定
     */
    override fun isLocked(): Boolean {
        return mLocked
    }

    /**
     * 设置锁定状态
     */
    override fun setLocked(locked: Boolean) {
        this.mLocked = locked
        //需要往下传递界面更新ui
        handleLockStateChanged(locked)
    }


    /**
     * 隐藏视图
     */
    override fun hide() {
        if (mShowing) {
            stopFadeOut()
            handleVisibilityChanged(false, mHideAnim)
            mShowing = false
        }
    }

    /**
     * 显示视图
     */
    override fun show() {
        if (!mShowing) {
            startFadeOut()
            handleVisibilityChanged(true, mShowAnim)
            mShowing = true
        }
    }


    /**
     * 控制界面显示倒计时
     */
    private val runnable = Runnable { hide() }


    /**
     * 进度更新
     */
    private val mShowProgress: Runnable = object : Runnable {
        override fun run() {
            val pos = setProgress()
            if (mControlWrapper?.isPlaying() == true) {
                postDelayed(this, ((1000 - pos % 1000) / mControlWrapper!!.getSpeed()).toLong())
            } else {
                mIsStartProgress = false
            }
        }
    }

    /**
     * 更新进度
     */
    private fun setProgress(): Int {
        val currentPosition = mControlWrapper?.getCurrentPosition()?.toInt() ?: 0
        val duration = mControlWrapper?.getDuration()?.toInt() ?: 0
        handleSetProgress(currentPosition, duration)
        return currentPosition
    }


    /**
     * 开始计时
     */
    override fun startFadeOut() {
        stopFadeOut()
        postDelayed(runnable, mDefaultTimeout)
    }

    /**
     * 停止计时
     */
    override fun stopFadeOut() {
        removeCallbacks(runnable)
    }


    /**
     * 开始刷新进度
     */
    override fun startProgress() {
        if (mIsStartProgress) return
        post(mShowProgress)
        mIsStartProgress = true
    }

    /**
     * 停止刷新进度
     */
    override fun stopProgress() {
        if (!mIsStartProgress) return
        removeCallbacks(mShowProgress)
        mIsStartProgress = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkCutout()
    }

    /**
     * 检查适配刘海屏
     */
    private fun checkCutout() {
        if (!mAdaptCutout) return
        if (mActivity != null && mHasCutout) {
            mHasCutout = CutoutUtil.allowDisplayToCutout(mActivity!!)
            if (mHasCutout) {
                //竖屏下的状态栏高度可认为是刘海的高度
                mCutoutHeight = PlayerUtils.getStatusBarHeightPortrait(mActivity!!).toInt()
            }
        }
    }

    /**
     * 刘海屏幕高度
     */
    override fun getCutoutHeight(): Int {
        return mCutoutHeight
    }

    /**
     * 是否有屏幕高度
     */
    override fun hasCutout(): Boolean {
        return mHasCutout
    }

    /**
     * 设置是否适配刘海屏
     */
    fun setAdaptCutout(adaptCutout: Boolean) {
        mAdaptCutout = adaptCutout
    }


    /**
     * 子类中请使用此方法来退出全屏
     *
     * @return 是否成功退出全屏
     */
    protected open fun stopFullScreen(): Boolean {
        if (mActivity == null || mActivity!!.isFinishing) return false
        mActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        mControlWrapper?.stopFullScreen()
        return true
    }


    /**
     * 是否自动旋转， 默认不自动旋转
     */
    open fun setEnableOrientation(enableOrientation: Boolean) {
        mEnableOrientation = enableOrientation
    }


    //------------------------ start handle event change ------------------------//


    /**
     * 播放器生命周期传递
     */
    private fun handlePlayStateChanged(playState: Int) {
        for (entry in mControlComponents) {
            val component = entry.key
            component.onPlayStateChanged(playState)
        }
        onPlayStateChanged(playState)
    }

    @CallSuper
    open fun onPlayStateChanged(playState: Int) {
        when (playState) {
            BaseVideoView.STATE_IDLE -> {
                mOrientationHelper?.disable()
                mOrientation = 0
                mLocked = false
                mShowing = false
                //由于游离组件是独立于控制器存在的，
                //所以在播放器release的时候需要移除
                removeAllDissociateComponents()
            }

            BaseVideoView.STATE_PLAYBACK_COMPLETED -> {
                mLocked = false
                mShowing = false
            }

            BaseVideoView.STATE_ERROR -> {
                mShowing = false
            }
        }
    }

    /**
     * 更新控制器在不同播放器状态下的ui
     */
    private fun handlePlayerStateChange(playerState: Int) {
        for (entry in mControlComponents) {
            val component = entry.key
            component.onPlayerStateChange(playerState)
        }
        onPlayerStateChange(playerState)
    }

    /**
     * 子类重写更新控制器在不同播放器状态下的ui
     */
    @CallSuper
    open fun onPlayerStateChange(playerState: Int) {
        when (playerState) {
            BaseVideoView.PLAYER_NORMAL -> {
                if (mEnableOrientation) {
                    mOrientationHelper?.enable()
                } else {
                    mOrientationHelper?.disable()
                }
                if (hasCutout()) {
                    CutoutUtil.adaptCutoutAboveAndroidP(mActivity, false)
                }
            }
            BaseVideoView.PLAYER_FULL_SCREEN -> {
                mOrientationHelper?.enable()
                if (hasCutout()) {
                    CutoutUtil.adaptCutoutAboveAndroidP(mActivity, true)
                }
            }
            BaseVideoView.PLAYER_TINY_SCREEN -> {
                mOrientationHelper?.disable()
            }
        }
    }

    /**
     * 锁定状态改变通知页面更新ui
     */
    private fun handleLockStateChanged(isLocked: Boolean) {
        for (entry: Map.Entry<IControlComponent, Boolean> in mControlComponents) {
            val component = entry.key
            component.onLockStateChanged(isLocked)
        }
        onLockStateChanged(isLocked)
    }

    /**
     * 子类重新更新锁ui
     */
    open fun onLockStateChanged(isLocked: Boolean) {

    }


    /**
     * 视图显示状态回调
     */
    private fun handleVisibilityChanged(mShowing: Boolean, anim: Animation?) {
        if (!mLocked) {
            for (entry: Map.Entry<IControlComponent, Boolean> in mControlComponents) {
                val component = entry.key
                component.onVisibilityChanged(mShowing, anim)
            }
        }
        if (anim != null) {
            onVisibilityChanged(mShowing, anim)
        }
    }

    /**
     * 视图显示状态回调
     */
    open fun onVisibilityChanged(mShowing: Boolean, anim: Animation?) {

    }

    /**
     * 进度更新回调
     */
    private fun handleSetProgress(currentPosition: Int, duration: Int) {
        for (entry: Map.Entry<IControlComponent, Boolean> in mControlComponents) {
            val component = entry.key
            component.setProgress(currentPosition, duration)
        }
        setProgress(currentPosition, duration)
    }

    /**
     * 进度更新回调
     */
    open fun setProgress(currentPosition: Int, duration: Int) {

    }

    //------------------------ end handle event change ------------------------//

}