package com.robot.tsplayer_kotlin.player

import android.content.res.AssetFileDescriptor
import android.view.Surface
import android.view.SurfaceHolder

/**
 * 抽象播放器
 */
abstract class AbstractPlayer {
    companion object {
        /**
         * 视频/音频开始渲染
         */
        const val MEDIA_INFO_RENDERING_START = 3
    }


    lateinit var mPlayerEventListener: PlayerEventListener

    /**
     * 初始化播放器
     */
    abstract fun initPlayer()


    /**
     * 设置播放地址
     * @path 播放地址
     * @headers 播放请求头
     */
    abstract fun setDataSource(path: String, headers: Map<String?, String?>?)


    /**
     * 用于播放raw和asset里面的视频文件
     */
    abstract fun setDataSource(fd: AssetFileDescriptor)


    /**
     * 播放
     */
    abstract fun start()

    /**
     * 暂停
     */
    abstract fun pause()

    /**
     * 停止
     */
    abstract fun stop()

    /**
     * 准备播放（异步）
     */
    abstract fun prepareAsync()

    /**
     * 是否正在播放
     */
    abstract fun isPlaying(): Boolean

    /**
     * 是否是循环播放
     */
    abstract fun isLooping(): Boolean

    /**
     * 获取当前播放位置
     */
    abstract fun getCurrentPosition(): Long

    /**
     * 获取播放时常
     */
    abstract fun getDuration(): Long

    /**
     * 重置播放器
     */
    abstract fun reset()

    /**
     * 拖动进度条
     */
    abstract fun seekTo(time: Long)

    /**
     * 获取缓冲百分比
     */
    abstract fun getBufferedPercentage(): Int

    /**
     * 设置渲染视频的View,主要用于TextureView
     */
    abstract fun setSurface(surface: Surface?)

    /**
     * 设置渲染视频的View,主要用于SurfaceView
     */
    abstract fun setDisplay(holder: SurfaceHolder?)

    /**
     * 设置音量
     */
    abstract fun setVolume(v1: Float, v2: Float)

    /**
     * 设置是否循环播放
     */
    abstract fun setLooping(isLooping: Boolean)

    /**
     * 设置其他播放配置
     */
    abstract fun setOptions()

    /**
     * 设置播放速度
     */
    abstract fun setSpeed(speed: Float)

    /**
     * 获取播放速度
     */
    abstract fun getSpeed(): Float

    /**
     * 获取当前缓冲的网速
     */
    abstract fun getTcpSpeed(): Long


    /**
     * 释放资源
     */
    abstract fun release()


    /**
     * 设置播放事件监听
     */
    fun setPlayerEventListener(playerEventListener: PlayerEventListener) {
        this.mPlayerEventListener = playerEventListener
    }

    public interface PlayerEventListener {
        fun onError()
        fun onCompletion()
        fun onInfo(what: Int, extra: Int)
        fun onPrepared()
        fun onVideoSizeChanged(width: Int, height: Int)
    }

}