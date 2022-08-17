package com.robot.tsplayer_kotlin.player

import com.robot.tsplayer_kotlin.render.RenderViewFactory
import com.robot.tsplayer_kotlin.render.TextureRenderViewFactory

/**
 * 播放器全局配置
 */

class VideoViewConfig(builder: Builder) {

    companion object {
        @JvmStatic
        fun newBuilder(): Builder {
            return Builder()
        }
    }

    var mPlayOnMobileNetwork = false

    var mEnableOrientation = false

    var mEnableAudioFocus = false

    var mIsEnableLog = false

    var mProgressManager: ProgressManager? = null

    var mPlayerFactory: PlayerFactory<*>? = null

    var mScreenScaleType = 0

    var mRenderViewFactory: RenderViewFactory? = null

    var mAdaptCutout = false

    init {
        mIsEnableLog = builder.mIsEnableLog
        mEnableOrientation = builder.mEnableOrientation
        mPlayOnMobileNetwork = builder.mPlayOnMobileNetwork
        mEnableAudioFocus = builder.mEnableAudioFocus
        mProgressManager = builder.mProgressManager
        mScreenScaleType = builder.mScreenScaleType
        mPlayerFactory = if (builder.mPlayerFactory == null) {
            //默认为AndroidMediaPlayer
            AndroidMediaPlayerFactory.create()
        } else {
            builder.mPlayerFactory
        }
        mRenderViewFactory = if (builder.mRenderViewFactory == null) {
            //默认使用TextureView渲染视频
            TextureRenderViewFactory.create()
        } else {
            builder.mRenderViewFactory
        }
        mAdaptCutout = builder.mAdaptCutout
    }

    class Builder {
        var mPlayOnMobileNetwork = false
        var mEnableOrientation = true
        var mEnableAudioFocus = true
        var mIsEnableLog = false
        var mProgressManager: ProgressManager? = null
        var mPlayerFactory: PlayerFactory<*>? = null
        var mScreenScaleType = 0
        var mRenderViewFactory: RenderViewFactory? = null
        var mAdaptCutout = true

        /**
         * 是否监听设备方向来切换全屏/半屏， 默认不开启
         */
        fun setEnableOrientation(enableOrientation: Boolean): Builder {
            mEnableOrientation = enableOrientation
            return this
        }

        /**
         * 移动网络是否继续播放
         */
        fun setPlayOmMobileNetwork(enablePlayOnMobileNetwork: Boolean): Builder {
            mPlayOnMobileNetwork = enablePlayOnMobileNetwork
            return this
        }

        /**
         * 是否开启AudioFocus监听， 默认开启
         */
        fun setEnableAudioFocus(enableAudioFocus: Boolean): Builder {
            mEnableAudioFocus = enableAudioFocus
            return this
        }

        /**
         * 设置进度管理器，用于保存播放进度
         */
        fun setProgressManager(progressManager: ProgressManager?): Builder {
            mProgressManager = progressManager
            return this
        }

        /**
         * 是否打印日志
         */
        fun setLogEnabled(enableLog: Boolean): Builder {
            mIsEnableLog = enableLog
            return this
        }

        /**
         * 自定义播放核心
         */
        fun setPlayerFactory(playerFactory: PlayerFactory<*>): Builder {
            mPlayerFactory = playerFactory
            return this
        }

        /**
         * 设置视频比例
         */
        fun setScreenScaleType(screenScaleType: Int): Builder {
            mScreenScaleType = screenScaleType
            return this
        }

        /**
         * 自定义RenderView
         */
        fun setRenderViewFactory(renderViewFactory: RenderViewFactory): Builder {
            mRenderViewFactory = renderViewFactory
            return this
        }

        /**
         * 是否适配刘海屏，默认适配
         */
        fun setAdaptCutout(adaptCutout: Boolean): Builder {
            mAdaptCutout = adaptCutout
            return this
        }

        fun build(): VideoViewConfig {
            return VideoViewConfig(this)
        }
    }
}