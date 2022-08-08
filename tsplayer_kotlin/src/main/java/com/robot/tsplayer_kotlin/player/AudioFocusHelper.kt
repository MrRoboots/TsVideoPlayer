package com.robot.tsplayer_kotlin.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import java.lang.ref.WeakReference

/**
 * 音频获取焦点类
 */
class AudioFocusHelper(@NonNull videoView: BaseVideoView<*>) : AudioManager.OnAudioFocusChangeListener {

    private val mHandle = Looper.getMainLooper()?.let { Handler(it) }

    private var mWeakVideoView: WeakReference<BaseVideoView<*>>? = null

    private val mAudioManager: AudioManager?

    private var mStartRequested = false
    private var mPausedForLoss = false
    private var mCurrentFocus: Int = 0

    private var status: Int = 0
    private lateinit var focusRequest: AudioFocusRequest

    init {
        this.mWeakVideoView = WeakReference<BaseVideoView<*>>(videoView)
        mAudioManager =
            this.mWeakVideoView?.get()?.context?.applicationContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (mCurrentFocus == focusChange) {
            return
        }

        mHandle?.post() {
            handleAudioFocusChange(focusChange)
        }

        mCurrentFocus = focusChange
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        val videoView: BaseVideoView<*> = mWeakVideoView?.get() ?: return

        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN,//获得焦点
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {//暂时获得焦点
                if (mStartRequested || mPausedForLoss) {
                    videoView.start()
                    mStartRequested = false
                    mPausedForLoss = false
                }
                if (!videoView.isMute()) //恢复音量
                    videoView.setVolume(1.0f, 1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS,//焦点丢失
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { //焦点暂时丢失
                if (videoView.isPlaying()) {
                    videoView.pause()
                    mPausedForLoss = true
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {//需要降低音量
                if (videoView.isPlaying() && !videoView.isMute()) {
                    videoView.setVolume(0.1f, 0.1f)
                }
            }
        }
    }

    /**
     * 获取音频焦点的请求
     */
    fun requestFocus() {
        if (mCurrentFocus == AudioManager.AUDIOFOCUS_GAIN) {
            return
        }
        if (mAudioManager == null) {
            return
        }

        /**
         * 兼容8.0
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()
            if (mHandle != null) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this, mHandle)
                    .build()
                status = mAudioManager.requestAudioFocus(focusRequest)
            }
        } else {
            status =
                mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }

        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == status) {
            mCurrentFocus = AudioManager.AUDIOFOCUS_GAIN
            return
        }

        mStartRequested = true
    }

    /**
     *请求系统放弃音频焦点
     */
    fun abandonFocus() {
        if (mAudioManager == null) {
            return
        }
        mStartRequested = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.abandonAudioFocusRequest(focusRequest)
        } else {
            mAudioManager.abandonAudioFocus(this)
        }
    }


}