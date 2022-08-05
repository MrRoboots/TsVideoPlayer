package com.robot.tsplayer_kotlin.player

import android.content.Context

/**
 * 播放器代理类
 */
abstract class PlayerFactory<P : AbstractPlayer> {
    abstract fun createPlayer(context: Context): P
}