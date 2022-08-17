package com.robot.tsplayer_kotlin.player

import android.content.Context

class AndroidMediaPlayerFactory : PlayerFactory<AndroidMediaPlayer>() {

    companion object {
        @JvmStatic
        fun create(): AndroidMediaPlayerFactory {
            return AndroidMediaPlayerFactory()
        }
    }

    override fun createPlayer(context: Context): AndroidMediaPlayer {
        return AndroidMediaPlayer(context)
    }
}