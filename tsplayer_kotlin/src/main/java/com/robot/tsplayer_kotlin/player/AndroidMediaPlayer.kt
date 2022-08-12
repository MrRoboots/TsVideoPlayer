package com.robot.tsplayer_kotlin.player

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.TrafficStats
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder

open class AndroidMediaPlayer(context: Context) : AbstractPlayer(), MediaPlayer.OnBufferingUpdateListener,
    MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnInfoListener,
    MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener {

    private var mAppContext: Context = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var mBufferingUpdate: Int = 0
    private var lastTimeStamp: Long = 0
    private var lastTotalRxBytes: Long = 0
    private var mIsPreparing: Boolean = false

    override fun initPlayer() {
        mediaPlayer = MediaPlayer()
        setOptions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes =
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            mediaPlayer?.setAudioAttributes(audioAttributes)
        } else {
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }
        mediaPlayer?.setOnBufferingUpdateListener(this)
        mediaPlayer?.setOnErrorListener(this)
        mediaPlayer?.setOnCompletionListener(this)
        mediaPlayer?.setOnInfoListener(this)
        mediaPlayer?.setOnBufferingUpdateListener(this)
        mediaPlayer?.setOnPreparedListener(this)
        mediaPlayer?.setOnVideoSizeChangedListener(this)
    }

    override fun setDataSource(path: String, headers: Map<String?, String?>) {
        try {
            mediaPlayer?.setDataSource(mAppContext, Uri.parse(path), headers)
        } catch (e: Exception) {
            mPlayerEventListener.onError()
        }
    }

    override fun setDataSource(fd: AssetFileDescriptor) {
        try {
            mediaPlayer?.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        } catch (e: Exception) {
            mPlayerEventListener.onError()
        }
    }

    override fun start() {
        try {
            mediaPlayer?.start()
        } catch (e: IllegalStateException) {
            mPlayerEventListener.onError()
        }
    }

    override fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: IllegalStateException) {
            mPlayerEventListener.onError()
        }
    }

    override fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (e: IllegalStateException) {
            mPlayerEventListener.onError()
        }
    }

    override fun prepareAsync() {
        try {
            mediaPlayer?.prepareAsync()
        } catch (e: IllegalStateException) {
            mPlayerEventListener.onError()
        }
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    override fun isLooping(): Boolean {
        return mediaPlayer?.isLooping ?: false
    }

    override fun getCurrentPosition(): Long {
        return mediaPlayer?.currentPosition?.toLong() ?: 0
    }

    override fun getDuration(): Long {
        return mediaPlayer?.duration?.toLong() ?: 0
    }

    override fun reset() {
        stop()
        mediaPlayer?.reset()
        mediaPlayer?.setSurface(null)
        mediaPlayer?.setDisplay(null)
        mediaPlayer?.setVolume(1f, 1f)
    }

    override fun seekTo(time: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //使用这个api seekTo定位更加准确 支持android 8.0以上的设备 https://developer.android.com/reference/android/media/mediaPlayer?#SEEK_CLOSEST
                mediaPlayer?.seekTo(time, MediaPlayer.SEEK_CLOSEST)
            } else {
                mediaPlayer?.seekTo(time.toInt())
            }
        } catch (e: IllegalStateException) {
            mPlayerEventListener.onError()
        }
    }

    override fun getBufferedPercentage(): Int {
        return mBufferingUpdate
    }

    override fun setSurface(surface: Surface?) {
        try {
            mediaPlayer?.setSurface(surface)
        } catch (e: Exception) {
            mPlayerEventListener.onError()
        }
    }

    override fun setDisplay(holder: SurfaceHolder?) {
        try {
            mediaPlayer?.setDisplay(holder)
        } catch (e: Exception) {
            mPlayerEventListener.onError()
        }
    }

    override fun setVolume(v1: Float, v2: Float) {
        mediaPlayer?.setVolume(v1, v2)
    }

    override fun setLooping(isLooping: Boolean) {
        mediaPlayer?.isLooping = isLooping
    }

    override fun setOptions() {
    }

    override fun setSpeed(speed: Float) {
        // only support above Android M
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(speed)!!
            } catch (e: Exception) {
                mPlayerEventListener.onError()
            }
        }
    }

    override fun getSpeed(): Float {
        // only support above Android M
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return try {
                var speed: Float = mediaPlayer?.playbackParams?.speed ?: 1f
                if (speed == 0f) speed = 1f
                speed
            } catch (e: Exception) {
                1f
            }
        }
        return 1f
    }

    override fun getTcpSpeed(): Long {
        val nowTotalRxBytes =
            if (TrafficStats.getUidRxBytes(mAppContext.applicationInfo.uid) == TrafficStats.UNSUPPORTED.toLong()) 0 else TrafficStats.getTotalRxBytes() / 1024 //转为KB

        val nowTimeStamp = System.currentTimeMillis()
        val calculationTime: Long = nowTimeStamp - lastTimeStamp
        if (calculationTime == 0L) {
            return calculationTime
        }
        //毫秒转换
        val speed: Long = (nowTotalRxBytes - lastTotalRxBytes) * 1000 / calculationTime
        lastTimeStamp = nowTimeStamp
        lastTotalRxBytes = nowTotalRxBytes
        return speed
    }

    override fun release() {
        mediaPlayer?.setOnErrorListener(null)
        mediaPlayer?.setOnCompletionListener(null)
        mediaPlayer?.setOnInfoListener(null)
        mediaPlayer?.setOnBufferingUpdateListener(null)
        mediaPlayer?.setOnPreparedListener(null)
        mediaPlayer?.setOnVideoSizeChangedListener(null)
        stop()
        val mediaPlayer: MediaPlayer? = mediaPlayer
        this.mediaPlayer = null
        object : Thread() {
            override fun run() {
                try {
                    mediaPlayer?.release()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }


    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
        mBufferingUpdate = percent
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mPlayerEventListener.onError()
        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {
        mPlayerEventListener.onCompletion()
    }

    override fun onInfo(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        //解决MEDIA_INFO_VIDEO_RENDERING_START多次回调问题
        if (what == MEDIA_INFO_RENDERING_START) {
            if (mIsPreparing) {
                mPlayerEventListener.onInfo(what, extra)
                mIsPreparing = false
            }
        } else {
            mPlayerEventListener.onInfo(what, extra)
        }
        return true
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mPlayerEventListener.onPrepared()
        start()
        // 修复播放纯音频时状态出错问题
        if (!isVideo()) {
            mPlayerEventListener.onInfo(MEDIA_INFO_RENDERING_START, 0)
        }
    }

    private fun isVideo(): Boolean {
        try {
            val trackInfo: Array<MediaPlayer.TrackInfo> =
                mediaPlayer?.trackInfo as Array<MediaPlayer.TrackInfo>
            for (info in trackInfo) {
                if (info.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO) {
                    return true
                }
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
        val videoWidth = mp!!.videoWidth
        val videoHeight = mp.videoHeight
        if (videoWidth != 0 && videoHeight != 0) {
            mPlayerEventListener.onVideoSizeChanged(videoWidth, videoHeight)
        }
    }


}