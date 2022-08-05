package com.robot.tsplayer_kotlin.player

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.robot.tsplayer_kotlin.controller.MediaPlayerControl
import com.robot.tsplayer_kotlin.render.RenderViewFactory

/**
 * 带泛型的播放器
 */

open class BaseVideoView<P : AbstractPlayer> : FrameLayout, MediaPlayerControl,
    AbstractPlayer.PlayerEventListener {

    //播放器
    protected var mMediaPlayer: P? = null

    //工厂类，用于实例化播放核心
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

    companion object {
        const val SCREEN_SCALE_DEFAULT = 0
        const val SCREEN_SCALE_16_9 = 1
        const val SCREEN_SCALE_4_3 = 2
        const val SCREEN_SCALE_MATCH_PARENT = 3
        const val SCREEN_SCALE_ORIGINAL = 4
        const val SCREEN_SCALE_CENTER_CROP = 5
    }

    constructor(context: Context) : super(context, null)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        val config: VideoViewConfig? = VideoViewManager.getConfig()

        config?.let {
            mEnableAudioFocus = it.mEnableAudioFocus
            mProgressManager = it.mProgressManager
            mPlayerFactory = it.mPlayerFactory as PlayerFactory<P>?
            mCurrentScreenScaleType = it.mScreenScaleType
            mRenderViewFactory = it.mRenderViewFactory
        }
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

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }


}