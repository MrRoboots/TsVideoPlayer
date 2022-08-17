package com.robot.tsplayer_kotlin.controller

interface IVideoController {
    /**
     * 开始控制视图自动隐藏倒计时
     */
    fun startFadeOut()

    /**
     * 取消控制视图自动隐藏倒计时
     */
    fun stopFadeOut()

    /**
     * 控制视图是否处于显示状态
     */
    fun isShowing(): Boolean

    /**
     * 设置锁定状态
     * @param locked 是否锁定
     */
    fun setLocked(locked: Boolean)

    /**
     * 开始刷新进度
     */
    fun startProgress()

    /**
     * 停止刷新进度
     */
    fun stopProgress()

    /**
     * 是否处于锁定状态
     */
    fun isLocked(): Boolean

    /**
     * 隐藏视图
     */
    fun hide()

    /**
     * 显示视图
     */
    fun show()

    /**
     * 是否需要适配刘海
     */
    fun hasCutout(): Boolean

    /**
     * 获取刘海的高度
     */
    fun getCutoutHeight(): Int
}