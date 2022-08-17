package com.robot.tsplayer_kotlin.controller

interface IGestureComponent : IControlComponent {

    /**
     * 开始滑动
     */
    fun onStartSlide()

    /**
     * 结束滑动
     */
    fun onStopSlide()

    /**
     * pos滑动进度
     * currentPosition当前进度
     * duration总进度
     */
    fun onPositionChange(pos: Int, currentPosition: Int, duration: Int)

    /**
     * 调节亮度
     * percent亮度百分比
     */
    fun onBrightnessChange(percent: Int)

    /**
     * 调节声音
     * percent声音百分比
     */
    fun onVolumeChange(percent: Int)

}
