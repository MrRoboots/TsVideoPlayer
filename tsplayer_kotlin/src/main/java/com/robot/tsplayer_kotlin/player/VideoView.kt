package com.robot.tsplayer_kotlin.player

import android.content.Context
import android.util.AttributeSet

open class VideoView : BaseVideoView<AbstractPlayer> {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
}