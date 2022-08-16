package com.robot.tsplayer_kotlin.controller

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap

class ControlWrapper(mediaPlayerControl: MediaPlayerControl, videoControl: IVideoController) : MediaPlayerControl,
    IVideoController {

    private var mMediaPlayer: MediaPlayerControl
    private var mVideoControl: IVideoController

    init {
        this.mMediaPlayer = mediaPlayerControl
        this.mVideoControl = videoControl
    }

    /**
     * 开始控制视图自动隐藏倒计时
     */
    override fun startFadeOut() {
        mVideoControl.startFadeOut()
    }

    /**
     * 取消控制视图自动隐藏倒计时
     */
    override fun stopFadeOut() {
        mVideoControl.stopFadeOut()
    }

    /**
     * 控制视图是否处于显示状态
     */
    override fun isShowing(): Boolean {
        return mVideoControl.isShowing()
    }

    /**
     * 设置锁定状态
     * @param locked 是否锁定
     */
    override fun setLocked(locked: Boolean) {
        mVideoControl.setLocked(locked)
    }

    /**
     * 开始刷新进度
     */
    override fun startProgress() {
        mVideoControl.startProgress()
    }

    /**
     * 停止刷新进度
     */
    override fun stopProgress() {
        mVideoControl.stopProgress()
    }

    /**
     * 是否处于锁定状态
     */
    override fun isLocked(): Boolean {
        return mVideoControl.isLocked()
    }

    /**
     * 隐藏视图
     */
    override fun hide() {
        mVideoControl.hide()
    }

    /**
     * 显示视图
     */
    override fun show() {
        mVideoControl.show()
    }

    /**
     * 是否需要适配刘海
     */
    override fun hasCutout(): Boolean {
        return mVideoControl.hasCutout()
    }

    /**
     * 获取刘海的高度
     */
    override fun getCutoutHeight(): Int {
        return mVideoControl.getCutoutHeight()
    }

    override fun start() {
        mMediaPlayer.start()
    }

    override fun pause() {
        mMediaPlayer.pause()
    }

    override fun getDuration(): Long {
        return mMediaPlayer.getDuration()
    }

    override fun getCurrentPosition(): Long {
        return mMediaPlayer.getCurrentPosition()
    }

    override fun seekTo(pos: Long) {
        mMediaPlayer.seekTo(pos)
    }

    override fun isPlaying(): Boolean {
        return mMediaPlayer.isPlaying()
    }

    override fun getBufferedPercentage(): Int {
        return mMediaPlayer.getBufferedPercentage()
    }

    override fun startFullScreen() {
        mMediaPlayer.startFullScreen()
    }

    override fun stopFullScreen() {
        mMediaPlayer.stopFullScreen()
    }

    override fun isFullScreen(): Boolean {
        return mMediaPlayer.isFullScreen()
    }

    override fun setMute(isMute: Boolean) {
        mMediaPlayer.setMute(isMute)
    }

    override fun isMute(): Boolean {
        return mMediaPlayer.isMute()
    }

    override fun setScreenScaleType(screenScaleType: Int) {
        mMediaPlayer.setScreenScaleType(screenScaleType)
    }

    override fun setSpeed(speed: Float) {
        mMediaPlayer.setSpeed(speed)
    }

    override fun getSpeed(): Float {
        return mMediaPlayer.getSpeed()
    }

    override fun getTcpSpeed(): Long {
        return mMediaPlayer.getTcpSpeed()
    }

    override fun replay(resetPosition: Boolean) {
        mMediaPlayer.replay(resetPosition)
    }

    override fun setMirrorRotation(enable: Boolean) {
        mMediaPlayer.setMirrorRotation(enable)
    }

    override fun doScreenShot(): Bitmap? {
        return mMediaPlayer.doScreenShot()
    }

    override fun getVideoSize(): IntArray? {
        return mMediaPlayer.getVideoSize()
    }

    override fun setRotation(rotation: Float) {
        return mMediaPlayer.setRotation(rotation)
    }

    override fun startTinyScreen() {
        mMediaPlayer.startTinyScreen()
    }

    override fun stopTinyScreen() {
        mMediaPlayer.stopTinyScreen()
    }

    override fun isTinyScreen(): Boolean {
        return mMediaPlayer.isTinyScreen()
    }

    /**
     * 切换隐藏显示
     */
    fun toggleShowState() {
        if (isShowing()) {
            hide()
        } else {
            show()
        }
    }

    /**
     * 切换播放暂停
     */
    fun togglePlay() {
        if (isPlaying()) {
            pause()
        } else {
            start()
        }
    }


    /**
     * 横竖屏切换，会旋转屏幕
     */
    fun toggleFullScreen(activity: Activity?) {
        if (activity == null || activity.isFinishing) return
        if (isFullScreen()) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            stopFullScreen()
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            startFullScreen()
        }
    }

    /**
     * 横竖屏切换，不会旋转屏幕
     */
    fun toggleFullScreen() {
        if (isFullScreen()) {
            stopFullScreen()
        } else {
            startFullScreen()
        }
    }

    /**
     * 横竖屏切换，根据适配宽高决定是否旋转屏幕
     */
    fun toggleFullScreenByVideoSize(activity: Activity?) {
        if (activity == null || activity.isFinishing) return
        val size = getVideoSize()
        val width = size!![0]
        val height = size[1]
        if (isFullScreen()) {
            stopFullScreen()
            if (width > height) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        } else {
            startFullScreen()
            if (width > height) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }

}