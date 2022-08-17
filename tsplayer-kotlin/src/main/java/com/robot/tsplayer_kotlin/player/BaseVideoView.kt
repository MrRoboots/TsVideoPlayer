package com.robot.tsplayer_kotlin.player

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.robot.tsplayer_kotlin.R
import com.robot.tsplayer_kotlin.controller.BaseVideoController
import com.robot.tsplayer_kotlin.controller.MediaPlayerControl
import com.robot.tsplayer_kotlin.render.IRenderView
import com.robot.tsplayer_kotlin.render.RenderViewFactory
import com.robot.tsplayer_kotlin.render.TextureRenderViewFactory
import com.robot.tsplayer_kotlin.utils.L.d
import com.robot.tsplayer_kotlin.utils.PlayerUtils
import java.io.IOException

/**
 * 带泛型的播放器
 *
 * 播放器视图层级
 * 1.FrameLayout
 * 2.mRenderView关联到mMediaPlayer
 * 3.mPlayerContainer添加mRenderView?.view
 * 4.mPlayerContainer添加控制层mVideoControl
 */

open class BaseVideoView<P : AbstractPlayer> @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), MediaPlayerControl,
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
    private var mIsLooping: Boolean = false

    /**
     * 播放器背景颜色
     */
    private var mPlayerBackgroundColor: Int = Color.BLACK

    /**
     * 承载播放器布局
     */
    private var mPlayerContainer: FrameLayout? = null

    //--------- data sources ---------//
    private var mUrl: String? = null //当前播放视频的地址
    private var mHeaders: Map<String?, String?>? = HashMap() //当前视频地址的请求头
    private var mAssetFileDescriptor: AssetFileDescriptor? = null //assets文件

    private var mCurrentPosition: Long = 0 //当前正在播放视频的位置

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
    private var mAudioFocusHelper: AudioFocusHelper? = null

    /**
     * 是否静音
     */
    private var isMute: Boolean = false


    /**
     * 渲染view
     */
    private var mRenderView: IRenderView? = null

    /**
     * 视频尺寸大小
     */
    private var mVideoSize = intArrayOf(0, 0)


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

        const val PLAYER_NORMAL = 10 // 普通播放器
        const val PLAYER_FULL_SCREEN = 11 // 全屏播放器
        const val PLAYER_TINY_SCREEN = 12 // 小屏播放器
    }

    var mCurrentPlayState = STATE_IDLE //当前播放器的状态

    private var mCurrentPlayerState = PLAYER_NORMAL //播放器类型

    private var mIsFullScreen = false //是否处于全屏状态
    private var mIsTinyScreen = false //是否处于小屏状态

    /**
     * 小窗视频尺寸大小
     */
    private var mTinyScreenSize = intArrayOf(0, 0)

    init {
        val config: VideoViewConfig? = VideoViewManager.getConfig()

        config?.let {
            mEnableAudioFocus = it.mEnableAudioFocus
            mProgressManager = it.mProgressManager
            mPlayerFactory = it.mPlayerFactory as PlayerFactory<P>
            mCurrentScreenScaleType = it.mScreenScaleType
            mRenderViewFactory = it.mRenderViewFactory
        }

        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.BaseVideoView)
        mIsLooping = typedArray.getBoolean(R.styleable.BaseVideoView_looping, false)
        mEnableAudioFocus = typedArray.getBoolean(R.styleable.BaseVideoView_enableAudioFocus, mEnableAudioFocus)
        mCurrentScreenScaleType = typedArray.getInt(R.styleable.BaseVideoView_screenScaleType, mCurrentScreenScaleType)
        mPlayerBackgroundColor = typedArray.getInt(R.styleable.BaseVideoView_playerBackgroundColor, Color.BLACK)
        typedArray.recycle()

        initView()
    }

    /**
     * 初始化界面
     */
    private fun initView() {
        mPlayerContainer = FrameLayout(context)
        mPlayerContainer?.setBackgroundColor(mPlayerBackgroundColor)
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.addView(mPlayerContainer, params)
    }

    /**
     * 设置进度管理器
     */
    fun setProgressManager(progressManager: ProgressManager) {
        mProgressManager = progressManager
    }

    /**
     * 循环播放， 默认不循环播放
     */
    fun setLooping(looping: Boolean) {
        mIsLooping = looping
        if (mMediaPlayer != null) {
            mMediaPlayer?.setLooping(looping)
        }
    }

    /**
     * 音频获取焦点类
     */
    fun setEnableAudioFocus(audioFocusHelper: AudioFocusHelper) {
        mAudioFocusHelper = audioFocusHelper
    }

    /**
     *自定义播放核心 可继承PlayerFactory实现自定义核心
     */
    fun setPlayerFactory(playerFactory: PlayerFactory<*>?) {
        if (playerFactory == null) {
            throw IllegalArgumentException("playerFactory can not be null!")
        }
        mPlayerFactory = playerFactory as PlayerFactory<P>?
    }

    /**
     * 自定义渲染RenderView
     */
    fun setRenderView(renderViewFactory: RenderViewFactory?) {
        if (renderViewFactory == null) {
            throw IllegalArgumentException("renderViewFactory can not be null!")
        }
        mRenderViewFactory = renderViewFactory
    }

    /**
     * 设置播放器背景
     */
    fun setPlayBackgroundColor(color: Int) {
        mPlayerContainer?.setBackgroundColor(color)
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
    private fun isInPlaybackState(): Boolean {
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
        } else if (isInPlaybackState()) {
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
            mCurrentPosition = mProgressManager?.getSavedProgress(mUrl) ?: 0
        }

        initPlayer()
        addDisplay()
        startPrepare(false)
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
        mMediaPlayer?.setLooping(mIsLooping)
        val volume = if (isMute) 0.0f else 1.0f
        mMediaPlayer?.setVolume(volume, volume)
    }

    /**
     * 初始化之前的配置项
     */
    protected open fun setInitOptions() {}


    /**
     * 添加到TextureView
     */
    private fun addDisplay() {
        mRenderView?.let {
            mPlayerContainer?.removeView(it.view)
            it.release()
        }

        mRenderViewFactory = TextureRenderViewFactory.create()
        mRenderView = mRenderViewFactory?.createRenderView(context)
        mMediaPlayer?.let { mRenderView?.attachToPlayer(it) }

        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER)
        mPlayerContainer?.addView(mRenderView?.view, 0, params)
    }

    /**
     * 开始准备播放
     */
    private fun startPrepare(reset: Boolean) {
        if (reset) {
            mMediaPlayer?.reset()
            setOptions()
        }
        if (prepareDataSource()) {
            mMediaPlayer?.prepareAsync()
            setPlayState(STATE_PREPARING)

            val playerState = if (isFullScreen()) {
                PLAYER_FULL_SCREEN
            } else if (isTinyScreen()) {
                PLAYER_TINY_SCREEN
            } else {
                PLAYER_NORMAL
            }
            setPlayerState(playerState)
        }
    }

    /**
     * 准备播放
     */
    private fun prepareDataSource(): Boolean {
        if (mAssetFileDescriptor != null) {
            mMediaPlayer?.setDataSource(mAssetFileDescriptor!!)
            return true
        } else if (!TextUtils.isEmpty(mUrl)) {
            mUrl?.let { mMediaPlayer?.setDataSource(it, mHeaders) }
            return true
        }
        return false
    }


    /**
     * 向Controller设置播放状态，用于控制Controller的ui展示
     */
    private fun setPlayState(playState: Int) {
        mCurrentPlayState = playState
        mVideoControl?.setPlayState(playState)
        if (mOnStateChangeListeners != null) {
            for (l: OnStateChangeListener? in PlayerUtils.getSnapshot(mOnStateChangeListeners!!)) {
                l?.onPlayStateChanged(playState)
            }
        }
    }

    /**
     * 设置全屏非全屏状态
     */
    private fun setPlayerState(playerState: Int) {
        mCurrentPlayerState = playerState
        mVideoControl?.setPlayerState(playerState)
        if (mOnStateChangeListeners != null) {
            for (l: OnStateChangeListener? in PlayerUtils.getSnapshot(mOnStateChangeListeners!!)) {
                l?.onPlayStateChanged(playerState)
            }
        }
    }

    /**
     * 显示使用移动网络警告
     */
    private fun showNetWarning(): Boolean {
        //播放本地文件 不检查网络
        if (isLocalDataSource()) return false
        return mVideoControl != null && mVideoControl?.showNetWarning() == true
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
    open fun setVideoController(mediaController: BaseVideoController?) {
        mPlayerContainer?.removeView(mVideoControl)
        mVideoControl = mediaController
        if (null != mediaController) {
            mVideoControl?.setMediaPlayer(this)
            val params =
                LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            mPlayerContainer?.addView(mVideoControl, params)
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
    open class SimpleOnStateChangeListener : OnStateChangeListener {
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

    /**
     * 在播放状态下
     */
    private fun startInPlaybackState() {
        mMediaPlayer?.start()
        setPlayState(STATE_PLAYING)
        if (mAudioFocusHelper != null && !isMute()) {
            mAudioFocusHelper?.requestFocus()
        }
        mPlayerContainer?.keepScreenOn = true
    }


    /**
     * 暂停播放
     */
    override fun pause() {
        if (isInPlaybackState() && mMediaPlayer?.isPlaying() == true) {
            mMediaPlayer?.pause()
            setPlayState(STATE_PAUSED)
            //释放音频焦点
            if (mAudioFocusHelper != null && !isMute()) {
                mAudioFocusHelper?.abandonFocus()
            }
            //不保持常亮
            mPlayerContainer?.keepScreenOn = false
        }
    }

    /**
     * 继续播放
     */
    fun resume() {
        if (isInPlaybackState() && mMediaPlayer?.isPlaying() == false) {
            mMediaPlayer?.start()
            setPlayState(STATE_PLAYING)
            //获取音频焦点
            if (mAudioFocusHelper != null && !isMute()) {
                mAudioFocusHelper?.requestFocus()
            }
            //保持常亮
            mPlayerContainer?.keepScreenOn = true
        }
    }

    /**
     * 播放器是否是空闲状态
     */
    private fun isInIdleState(): Boolean {
        return mCurrentPlayState == STATE_IDLE
    }

    /**
     * 释放资源
     */
    fun release() {
        if (isInIdleState()) {
            //释放播放器
            if (mMediaPlayer != null) {
                mMediaPlayer?.release()
                mMediaPlayer = null
            }

            //释放reader
            if (mRenderView != null) {
                mPlayerContainer?.removeView(mRenderView?.view)
                mRenderView?.release()
                mRenderView = null
            }

            //释放assets
            if (mAssetFileDescriptor != null) {
                try {
                    mAssetFileDescriptor?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            //释放音频焦点
            if (mAudioFocusHelper != null) {
                mAudioFocusHelper?.abandonFocus()
                mAudioFocusHelper = null
            }

            //关闭屏幕常亮
            mPlayerContainer?.keepScreenOn = false

            //保存进度
            saveProgress()

            //重制生命周期 进度
            mCurrentPosition = 0

            //设置状态
            setPlayState(STATE_IDLE)
        }
    }

    /**
     * 保存进度
     */
    private fun saveProgress() {
        if (mProgressManager != null && mCurrentPosition > 0) {
            d("saveProgress: $mCurrentPosition")
            mProgressManager?.saveProgress(mUrl, mCurrentPosition)
        }
    }

    /**
     * 获取时长
     */
    override fun getDuration(): Long {
        if (isInPlaybackState()) {
            return mMediaPlayer?.getDuration()!!
        }
        return 0
    }

    /**
     * 获取当前播放进度
     */
    override fun getCurrentPosition(): Long {
        if (isInPlaybackState()) {
            mCurrentPosition = mMediaPlayer?.getCurrentPosition()!!
            return mCurrentPosition
        }
        return 0
    }

    /**
     * 拖动进度
     */
    override fun seekTo(pos: Long) {
        if (isInPlaybackState()) mMediaPlayer?.seekTo(pos)
    }

    /**
     * 是否播放
     */
    override fun isPlaying(): Boolean {
        return isInPlaybackState() && mMediaPlayer?.isPlaying() == true
    }

    /**
     * 获取缓冲百分比
     */
    override fun getBufferedPercentage(): Int {
        return mMediaPlayer?.getBufferedPercentage() ?: 0
    }

    /**
     * 设置全屏
     */
    override fun startFullScreen() {
        //是全屏不继续执行
        if (mIsFullScreen) return

        //获取decorView类
        val decorView = getDecorView() ?: return

        mIsFullScreen = true

        //隐藏NavigationBar和StatusBar
        hideSysBar(decorView)

        //从当前最底层view移除当前播放mPlayerContainer
        this.removeView(mPlayerContainer)

        //添加到DecorView
        decorView.addView(mPlayerContainer)

        //设置全屏状态
        setPlayerState(PLAYER_FULL_SCREEN)
    }

    /**
     * 隐藏NavigationBar和StatusBar
     */
    private fun hideSysBar(decorView: ViewGroup) {
        val window = getActivity()?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun showSysBar(decorView: ViewGroup) {
        val window = getActivity()?.window as Window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, decorView).show(WindowInsetsCompat.Type.systemBars())
    }


    /**
     * 退出全屏
     */
    override fun stopFullScreen() {
        if (!mIsFullScreen) return
        //获取decorView类
        val decorView = getDecorView() ?: return

        mIsFullScreen = false
        //显示NavigationBar和StatusBar
        showSysBar(decorView)
        //移除表示退出全屏
        decorView.removeView(mPlayerContainer)
        this.addView(mPlayerContainer)
        //设置为普通播放状态
        setPlayerState(PLAYER_NORMAL)
    }

    /**
     * 是否全屏
     */
    override fun isFullScreen(): Boolean {
        return mIsFullScreen
    }

    /**
     * 设置静音
     */
    override fun setMute(isMute: Boolean) {
        this.isMute = isMute
        if (mMediaPlayer != null) {
            val volume = if (isMute) 0.0f else 1.0f
            mMediaPlayer?.setVolume(volume, volume)
        }
    }

    /**
     * 是否静音
     */
    override fun isMute(): Boolean {
        return this.isMute
    }

    /**
     * 设置屏幕比例类型
     */
    override fun setScreenScaleType(screenScaleType: Int) {
        mCurrentScreenScaleType = screenScaleType
        if (mRenderView != null) {
            mRenderView?.setScaleType(screenScaleType)
        }
    }

    /**
     * 设置播放倍速
     */
    override fun setSpeed(speed: Float) {
        if (isInPlaybackState()) {
            mMediaPlayer?.setSpeed(speed)
        }
    }

    /**
     * 获取倍速
     */
    override fun getSpeed(): Float {
        if (isInPlaybackState()) {
            return mMediaPlayer?.getSpeed() ?: 1f
        }
        return 1f
    }

    /**
     * 设置视频地址
     */
    fun setUrl(url: String?) {
        if (url != null) {
            setUrl(url, null)
        }
    }

    /**
     * 设置包含请求头信息的视频地址
     *
     * @param url     视频地址
     * @param headers 请求头
     */
    fun setUrl(url: String, headers: Map<String?, String?>?) {
        mAssetFileDescriptor = null
        mUrl = url
        mHeaders = headers
    }

    /**
     * 用于播放assets里面的视频文件
     */
    fun setAssetFileDescriptor(fd: AssetFileDescriptor?) {
        mUrl = null
        mAssetFileDescriptor = fd
    }

    /**
     * 一开始播放就seek到预先设置好的位置
     */
    fun skipPositionWhenPlay(position: Int) {
        mCurrentPosition = position.toLong()
    }


    /**
     * 获取缓冲下载速度
     */
    override fun getTcpSpeed(): Long {
        if (isInPlaybackState()) {
            return mMediaPlayer?.getTcpSpeed() ?: 0
        }
        return 0
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && mIsFullScreen) {
            //重新获取焦点隐藏
            getDecorView()?.let { hideSysBar(it) }
        }
    }

    /**
     * 重新播放
     * resetPosition 是否重新播放
     */
    override fun replay(resetPosition: Boolean) {
        if (resetPosition) {
            mCurrentPosition = 0
        }
        addDisplay()
        startPrepare(true)
    }

    /**
     * 设置镜像旋转，暂不支持SurfaceView
     */
    override fun setMirrorRotation(enable: Boolean) {
        if (mRenderView != null) {
            mRenderView?.view?.scaleX = if (enable) -1f else 1f
        }
    }

    /**
     * 屏幕截图,暂不支持SurfaceView
     */
    override fun doScreenShot(): Bitmap? {
        if (mRenderView != null) {
            return mRenderView?.doScreenShot()
        }
        return null
    }

    /**
     * 获取视频宽高,其中width: mVideoSize[0], height: mVideoSize[1]
     */
    override fun getVideoSize(): IntArray? {
        return mVideoSize
    }

    /**
     * 设置小窗视频尺寸
     */
    fun setTinyScreenSize(size: IntArray) {
        mTinyScreenSize = size
    }

    /**
     * 启动小屏幕播放
     */
    override fun startTinyScreen() {
        if (mIsTinyScreen) return
        val contentView = getContentView() ?: return
        this.removeView(mPlayerContainer)

        var width = mTinyScreenSize[0]
        var height = mTinyScreenSize[1]

        if (width <= 0) {
            width = PlayerUtils.getScreenWidth(context, false) / 2
        }

        if (height <= 0) {
            height = width * 9 / 16
        }

        val params = LayoutParams(width, height)
        params.gravity = Gravity.BOTTOM or Gravity.END
        contentView.addView(mPlayerContainer, params)
        mIsTinyScreen = true
        setPlayerState(PLAYER_TINY_SCREEN)
    }

    /**
     * 停止小屏幕播放
     */
    override fun stopTinyScreen() {
        if (!mIsTinyScreen) return
        val contentView = getContentView() ?: return
        //从Content移除
        contentView.removeView(mPlayerContainer)
        //添加到playerContainer
        val params = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        this.addView(mPlayerContainer, params)
        mIsTinyScreen = false
        setPlayerState(PLAYER_NORMAL)
    }

    /**
     * 是否是小屏
     */
    override fun isTinyScreen(): Boolean {
        return mIsTinyScreen
    }

    /**
     * 播放报错
     */
    override fun onError() {
        mPlayerContainer?.keepScreenOn = false
        setPlayState(STATE_ERROR)
    }

    /**
     * 播放完成
     */
    override fun onCompletion() {
        mPlayerContainer?.keepScreenOn = false
        mCurrentPosition = 0
        if (mProgressManager != null) {
            mProgressManager?.saveProgress(mUrl, 0)
        }
        setPlayState(STATE_PLAYBACK_COMPLETED)
    }

    /**
     * 视频信息
     */
    override fun onInfo(what: Int, extra: Int) {
        when (what) {
            // 视频/音频开始渲染
            AbstractPlayer.MEDIA_INFO_RENDERING_START -> {
                setPlayState(STATE_PLAYING);
                mPlayerContainer?.keepScreenOn = true;
            }
        }
    }

    /**
     * 准备完成
     */
    override fun onPrepared() {
        setPlayState(STATE_PREPARED)
        if (mAudioFocusHelper != null && !isMute) {
            mAudioFocusHelper?.requestFocus()
        }
        if (mCurrentPosition > 0) {
            seekTo(mCurrentPosition)
        }
    }

    /**
     * 视频大小改变
     */
    override fun onVideoSizeChanged(width: Int, height: Int) {
        mVideoSize[0] = width
        mVideoSize[1] = height
        if (mRenderView != null) {
            mRenderView?.setScaleType(mCurrentScreenScaleType)
            mRenderView?.setVideoSize(width, height)
        }
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

    /**
     * 获取DecorView
     */
    protected open fun getDecorView(): ViewGroup? {
        val activity = getActivity() ?: return null
        return activity.window.decorView as ViewGroup
    }

    /**
     * 获取activity中的content view,其id为android.R.id.content
     */
    open fun getContentView(): ViewGroup? {
        val activity = getActivity() ?: return null
        return activity.findViewById(android.R.id.content)
    }

    /**
     * 获取Activity
     */
    open fun getActivity(): Activity? {
        var activity: Activity?
        if (mVideoControl != null) {
            activity = PlayerUtils.scanForActivity(mVideoControl?.context)
            if (activity == null) {
                activity = PlayerUtils.scanForActivity(context)
            }
        } else {
            activity = PlayerUtils.scanForActivity(context)
        }
        return activity
    }

    /**
     * 改变返回键逻辑，用于activity
     */
    open fun onBackPressed(): Boolean {
        return mVideoControl != null && mVideoControl?.onBackPressed() == true
    }

    override fun onSaveInstanceState(): Parcelable? {
        d("onSaveInstanceState: $mCurrentPosition")
        //activity切到后台后可能被系统回收，故在此处进行进度保存
        saveProgress()
        return super.onSaveInstanceState()
    }

    override fun setRotation(rotation: Float) {
        if (mRenderView != null) {
            mRenderView?.setVideoRotation(rotation.toInt())
        }
    }
}