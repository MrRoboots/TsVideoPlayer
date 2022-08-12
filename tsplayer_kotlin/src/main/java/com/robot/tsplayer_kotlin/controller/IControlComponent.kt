package com.robot.tsplayer_kotlin.controller

import android.view.View
import android.view.animation.Animation

/**
 * 可继承此接口实现自己的控制ui，以及监听播放器的状态
 */
interface IControlComponent {

    /**
     * 锁定状态改变 不响应手势状态 ui隐藏等
     */
    fun onLockStateChanged(locked: Boolean)

    /**
     * 回调控制器显示和隐藏状态
     */
    fun onVisibilityChanged(mShowing: Boolean, anim: Animation)

    /**
     * 绑定ControlComponent和Controller
     */
    fun attach(mControlWrapper: ControlWrapper)

    /**
     * 进度回调
     */
    fun setProgress(currentPosition: Int, duration: Int)

    /**
     * 获取控制ui view
     */
    fun getView(): View?

    /**
     * 播放器生命周期回调
     */
    fun onPlayStateChanged(playState: Int)

    /**
     *
     */
    fun onPlayerStateChange(playerState: Int)


}