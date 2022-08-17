package com.robot.tsplayer_kotlin.utils

import android.util.Log
import com.robot.tsplayer_kotlin.player.VideoViewManager.Companion.getConfig

/**
 * 日志类
 */
object L {
    private const val TAG = "TSPlayer"
    private var isDebug = getConfig()!!.mIsEnableLog

    fun d(msg: String?) {
        if (isDebug) {
            Log.d(TAG, msg!!)
        }
    }

    fun e(msg: String?) {
        if (isDebug) {
            Log.e(TAG, msg!!)
        }
    }

    fun i(msg: String?) {
        if (isDebug) {
            Log.i(TAG, msg!!)
        }
    }

    fun w(msg: String?) {
        if (isDebug) {
            Log.w(TAG, msg!!)
        }
    }

    fun setDebug(isDebug: Boolean) {
        L.isDebug = isDebug
    }
}