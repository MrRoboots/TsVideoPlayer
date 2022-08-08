package com.robot.tsplayer_kotlin.player

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import com.robot.tsplayer_kotlin.R
import com.robot.tsplayer_kotlin.controller.BaseVideoController
import com.robot.tsplayer_kotlin.controller.MediaPlayerControl
import com.robot.tsplayer_kotlin.render.RenderViewFactory
import com.robot.tsplayer_kotlin.utils.PlayerUtils

/**
 * 带泛型的播放器
 */

open class BaseVideoView<P : AbstractPlayer> : FrameLayout, MediaPlayerControl,
    AbstractPlayer.PlayerEventListener {

    /**
     * 播放器
     */
    private var mMediaPlayer: P? = null

    /**
     * 工厂类，用于实例化播放核心
     */
    private var mPlayerFactory: PlayerFactory<P>? = null

    /**
     * 监听系统中音频焦点改变
     */
    private var mEnableAudioFocus: Boolean = false

    /**
     * 进度管理器，设置之后播放器会记录播放进度，以便下次播放恢复进度
     */
    private var mProgressManager: ProgressManager? = null

    /**
     * 当前视频比例类型
     */
    private var mCurrentScreenScaleType = 0

    /**
     * 渲染View
     */
    private var mRenderViewFactory: RenderViewFactory? = null

    /**
     * 是否循环播放
     */
    private var mIsLopping: Boolean = false

    /**
     * 播放器背景颜色
     */
    private var mPlayerBackgroundColor: Int = Color.BLACK

    /**
     * 承载播放器布局
     */
    private lateinit var mPlayerContainer: FrameLayout

    //--------- data sources ---------//
    private var mUrl: String? = null //当前播放视频的地址
    protected var mHeaders: Map<String, String>? = null //当前视频地址的请求头
    private var mAssetFileDescriptor: AssetFileDescriptor? = null //assets文件

    protected var mCurrentPosition: Long = 0 //当前正在播放视频的位置

    /**
     * 播放器控制类
     */
    private var mVideoControl: BaseVideoController? = null

    /**
     * OnStateChangeListener集合，保存了所有开发者设置的监听器
     */
    private var mOnStateChangeListeners: ArrayList<OnStateChangeListener>? = null

    /**
     * 音频焦点
     */
    private lateinit var mAudioFocusHelper: AudioFocusHelper


    companion object {
        /**
         * 视频比例
         */
        const val SCREEN_SCALE_DEFAULT = 0
        const val SCREEN_SCALE_16_9 = 1
        const val SCREEN_SCALE_4_3 = 2
        const val SCREEN_SCALE_MATCH_PARENT = 3
        const val SCREEN_SCALE_ORIGINAL = 4
        const val SCREEN_SCALE_CENTER_CROP = 5

        /**
         * 视频播放状态
         */
        const val STATE_ERROR: Int = -1 //播放错误
        const val STATE_IDLE = 0 //空闲状态
        const val STATE_PREPARING = 1 //准备中
        const val STATE_PREPARED = 2 //准备完成
        const val STATE_PLAYING = 3 //播放中
        const val STATE_PAUSED = 4 //暂停
        const val STATE_PLAYBACK_COMPLETED = 5 //播放完成
        const val STATE_BUFFERING = 6 //缓冲中
        const val STATE_BUFFERED = 7 //缓冲完成
        const val STATE_START_ABORT = 8 //开始播放中止
    }

    private var mCurrentPlayState = STATE_IDLE //当前播放器的状态


    constructor(context: Context) : super(context, null)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        /**
         * 获取播放器全局配置
         */
        val config: VideoViewConfig? = VideoViewManager.getConfig()

        config?.let {
            mEnableAudioFocus = it.mEnableAudioFocus
            mProgressManager = it.mProgressManager
            mPlayerFactory = it.mPlayerFactory as PlayerFactory<P>?
            mCurrentScreenScaleType = it.mScreenScaleType
            mRenderViewFactory = it.mRenderViewFactory
        }

        /**
         * 设置自定义属性
         */
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.BaseVideoView)
        mIsLopping = typedArray.getBoolean(R.styleable.BaseVideoView_looping, false)
        mEnableAudioFocus =
            typedArray.getBoolean(R.styleable.BaseVideoView_enableAudioFocus, mEnableAudioFocus)
        mCurrentScreenScaleType =
            typedArray.getInt(R.styleable.BaseVideoView_screenScaleType, mCurrentScreenScaleType)
        mPlayerBackgroundColor =
            typedArray.getInt(R.styleable.BaseVideoView_playerBackgroundColor, Color.BLACK)
        typedArray.recycle()

        initView()
    }

    /**
     * 初始化界面
     */
    private fun initView() {
        mPlayerContainer = FrameLayout(context)
        mPlayerContainer.setBackgroundColor(mPlayerBackgroundColor)
        val params =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        this.addView(mPlayerContainer, params)
    }

    /**
     * 设置进度管理器
     */
    fun setProgressManager(progressManager: ProgressManager) {
        mProgressManager = progressManager
    }

    /**
     * 设置播放器背景
     */
    fun setPlayBackgroundColor(color: Int) {
        mPlayerContainer.setBackgroundColor(color)
    }

    /**
     * 是否处于未播放状态
     */
    private fun isInIdeaState(): Boolean {
        return mCurrentPlayState == STATE_IDLE
    }

    /**
     * 是否是中止状态
     */
    private fun isInStartAbortState(): Boolean {
        return mCurrentPlayState == STATE_START_ABORT
    }

    /**
     * 是否是播放状态
     */
    private fun isInPlayState(): Boolean {
        return mMediaPlayer != null
                && mCurrentPlayState != STATE_ERROR
                && mCurrentPlayState != STATE_IDLE
                && mCurrentPlayState != STATE_PREPARING
                && mCurrentPlayState != STATE_START_ABORT
                && mCurrentPlayState != STATE_PLAYBACK_COMPLETED
    }

    /**
     * 开始播放
     */
    override fun start() {
        if (isInIdeaState() || isInStartAbortState()) {
            startPlay()
        } else if (isInPlayState()) {
            startInPlaybackState()
        }
    }

    /**
     * 第一次播放
     * @return 是否播放成功
     */
    private fun startPlay(): Boolean {
        //是否使用移动网络播放
        if (showNetWarning()) {
            setPlayState(STATE_START_ABORT)
            return false
        }

        //监听音频焦点改变
        if (mEnableAudioFocus) {
            mAudioFocusHelper = AudioFocusHelper(this)
        }

        //设置进度管理
        if (mProgressManager != null) {
            mCurrentPosition = mProgressManager!!.getSavedProgress(mUrl)
        }

        initPlayer()
        addDisplay()
        startPrepare()
        return true
    }


    /**
     * 初始化播放器
     *
     */
    private fun initPlayer() {
        mMediaPlayer = mPlayerFactory?.createPlayer(context)
        mMediaPlayer?.mPlayerEventListener = this
        setInitOptions()
        mMediaPlayer?.initPlayer()
        setOptions()
    }

    /**
     * 初始化之后的配置项
     */
    private fun setOptions() {
        TODO("Not yet implemented")
    }

    /**
     * 初始化之前的配置项
     */
    protected open fun setInitOptions() {}


    /**
     * 添加到TextureView
     */
    private fun addDisplay() {

    }

    /**
     * 开始准备播放
     */
    private fun startPrepare() {

    }


    /**
     * 向Controller设置播放状态，用于控制Controller的ui展示
     */
    private fun setPlayState(playState: Int) {
        mCurrentPlayState = playState
        mVideoControl?.setPlayState(playState)
        if (mOnStateChangeListeners != null) {
            for (l: OnStateChangeListener in PlayerUtils.getSnapshot(mOnStateChangeListeners!!)) {
                l.onPlayStateChanged(playState)
            }
        }
    }

    /**
     * 显示使用移动网络警告
     */
    private fun showNetWarning(): Boolean {
        //播放本地文件 不检查网络
        if (isLocalDataSource()) return false
        return mVideoControl != null && mVideoControl!!.showNetWarning()
    }

    /**
     * 是否是本地文件
     */
    private fun isLocalDataSource(): Boolean {
        if (null != mAssetFileDescriptor) {
            return true
        } else if (!TextUtils.isEmpty(mUrl)) {
            val uri = Uri.parse(mUrl)
            return ContentResolver.SCHEME_ANDROID_RESOURCE == uri.scheme
                    || ContentResolver.SCHEME_FILE == uri.scheme
                    || "rawresource" == uri.scheme
        }
        return false
    }

    /**
     * 设置播放器控制类
     */
    private fun setVideoController(mediaController: BaseVideoController?) {
        mPlayerContainer.removeView(mVideoControl)
        mVideoControl = mediaController
        if (null != mediaController) {
            //TODO
            val params =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            this.addView(mVideoControl, params)
        }
    }

    /**
     * 播放器状态改变监听
     */
    interface OnStateChangeListener {
        fun onPlayerStateChanged(playerState: Int)
        fun onPlayStateChanged(playState: Int)
    }

    /**
     * OnStateChangeListener的空实现。用的时候只需要重写需要的方法
     */
    class SimpleOnStateChangeListener : OnStateChangeListener {
        override fun onPlayerStateChanged(playerState: Int) {}
        override fun onPlayStateChanged(playState: Int) {}
    }

    /**
     * 添加一个播放状态监听器，播放状态发生变化时将会调用。
     */
    open fun addOnStateChangeListener(listener: OnStateChangeListener) {
        if (mOnStateChangeListeners == null) {
            mOnStateChangeListeners = ArrayList()
        }
        mOnStateChangeListeners?.add(listener)
    }

    /**
     * 移除某个播放状态监听
     */
    open fun removeOnStateChangeListener(listener: OnStateChangeListener) {
        mOnStateChangeListeners?.remove(listener)
    }

    /**
     * 设置一个播放状态监听器，播放状态发生变化时将会调用，
     * 如果你想同时设置多个监听器，推荐 [.addOnStateChangeListener]。
     */
    open fun setOnStateChangeListener(listener: OnStateChangeListener) {
        if (mOnStateChangeListeners == null) {
            mOnStateChangeListeners = ArrayList()
        } else {
            mOnStateChangeListeners?.clear()
        }
        mOnStateChangeListeners?.add(listener)
    }

    /**
     * 移除所有播放状态监听
     */
    open fun clearOnStateChangeListeners() {
        mOnStateChangeListeners?.clear()
    }


    private fun startInPlaybackState() {
        TODO("Not yet implemented")
    }


    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun getDuration(): Long {
        TODO("Not yet implemented")
    }

    override fun getCurrentPosition(): Long {
        TODO("Not yet implemented")
    }

    override fun seekTo(pos: Long) {
        TODO("Not yet implemented")
    }

    override fun isPlaying(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getBufferedPercentage(): Int {
        TODO("Not yet implemented")
    }

    override fun startFullScreen() {
        TODO("Not yet implemented")
    }

    override fun stopFullScreen() {
        TODO("Not yet implemented")
    }

    override fun isFullScreen(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setMute(isMute: Boolean) {
        TODO("Not yet implemented")
    }

    override fun isMute(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setScreenScaleType(screenScaleType: Int) {
        TODO("Not yet implemented")
    }

    override fun setSpeed(speed: Float) {
        TODO("Not yet implemented")
    }

    override fun getSpeed(): Float {
        TODO("Not yet implemented")
    }

    override fun getTcpSpeed(): Long {
        TODO("Not yet implemented")
    }

    override fun replay(resetPosition: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setMirrorRotation(enable: Boolean) {
        TODO("Not yet implemented")
    }

    override fun doScreenShot(): Bitmap? {
        TODO("Not yet implemented")
    }

    override fun getVideoSize(): IntArray? {
        TODO("Not yet implemented")
    }

    override fun startTinyScreen() {
        TODO("Not yet implemented")
    }

    override fun stopTinyScreen() {
        TODO("Not yet implemented")
    }

    override fun isTinyScreen(): Boolean {
        TODO("Not yet implemented")
    }

    override fun onError() {
        TODO("Not yet implemented")
    }

    override fun onCompletion() {
        TODO("Not yet implemented")
    }

    override fun onInfo(what: Int, extra: Int) {
        TODO("Not yet implemented")
    }

    override fun onPrepared() {
        TODO("Not yet implemented")
    }

    override fun onVideoSizeChanged(width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    /**
     * 设置音量 0.0f-1.0f 之间
     *
     * @param v1 左声道音量
     * @param v2 右声道音量
     */
    open fun setVolume(v1: Float, v2: Float) {
        if (mMediaPlayer != null) {
            mMediaPlayer?.setVolume(v1, v2)
        }
    }
}