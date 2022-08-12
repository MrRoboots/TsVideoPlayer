package com.robot.tsplayer_kotlin.controller

import android.content.Context
import android.view.OrientationEventListener

class OrientationHelper(context: Context?) : OrientationEventListener(context) {

    private var listener: OrientationChangeListener? = null
    private var lastMillis: Long = 0

    override fun onOrientationChanged(orientation: Int) {
        val currentMillis = System.currentTimeMillis()
        if (currentMillis - lastMillis < 300) return
        if (listener != null) {
            listener?.onOrientationChangeListener(orientation)
        }
        lastMillis = currentMillis
    }

    fun setOrientationChangeListener(listener: OrientationChangeListener) {
        this.listener = listener
    }

    interface OrientationChangeListener {
        fun onOrientationChangeListener(orientation: Int)
    }
}