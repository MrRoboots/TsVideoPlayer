package com.robot.tsplayer_kotlin.controller

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.robot.tsplayer_kotlin.player.VideoViewManager
import com.robot.tsplayer_kotlin.utils.PlayerUtils

abstract class BaseVideoController : FrameLayout {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    /**
     * 是否使用流量播放视频
     */
    fun showNetWarning(): Boolean {
        return PlayerUtils.getNetworkType(context) == PlayerUtils.NETWORK_MOBILE && !VideoViewManager.getInstance()
            ?.playOnMobileNetwork()!!
    }

    fun setPlayState(playState: Int) {
        TODO("Not yet implemented")
    }

}