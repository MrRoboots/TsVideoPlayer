package com.robot.tsvideoplayer.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import com.robot.tsplayer_kotlin.controller.ControlWrapper
import com.robot.tsplayer_kotlin.player.BaseVideoView
import com.robot.tsplayer_kotlin.player.VideoViewManager.Companion.getConfig

object Utils {
    /**
     * 获取当前的播放核心
     */
    @JvmStatic
    val currentPlayerFactory: Any?
        get() {
            val config = getConfig()
            var playerFactory: Any? = null
            try {
                val mPlayerFactoryField = config!!.javaClass.getDeclaredField("mPlayerFactory")
                mPlayerFactoryField.isAccessible = true
                playerFactory = mPlayerFactoryField[config]
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return playerFactory
        }

    @JvmStatic
    fun getCurrentPlayerFactoryInVideoView(controlWrapper: ControlWrapper): Any? {
        var playerFactory: Any? = null
        try {
            val mPlayerControlField = controlWrapper.javaClass.getDeclaredField("mPlayerControl")
            mPlayerControlField.isAccessible = true
            val playerControl = mPlayerControlField[controlWrapper]
            if (playerControl is BaseVideoView<*>) {
                playerFactory = getCurrentPlayerFactoryInVideoView(playerControl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return playerFactory
    }

    @JvmStatic
    fun getCurrentPlayerFactoryInVideoView(BaseVideoView: BaseVideoView<*>): Any? {
        var playerFactory: Any? = null
        try {
            val mPlayerFactoryField = BaseVideoView.javaClass.superclass.getDeclaredField("mPlayerFactory")
            mPlayerFactoryField.isAccessible = true
            playerFactory = mPlayerFactoryField[BaseVideoView]
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return playerFactory
    }

    /**
     * 将View从父控件中移除
     */
    @JvmStatic
    fun removeViewFormParent(v: View?) {
        if (v == null) return
        val parent = v.parent
        if (parent is FrameLayout) {
            parent.removeView(v)
        }
    }

    /**
     * Returns a string containing player state debugging information.
     */
    @JvmStatic
    fun playState2str(state: Int): String {
        val playStateString: String = when (state) {
            BaseVideoView.STATE_IDLE -> "idle"
            BaseVideoView.STATE_PREPARING -> "preparing"
            BaseVideoView.STATE_PREPARED -> "prepared"
            BaseVideoView.STATE_PLAYING -> "playing"
            BaseVideoView.STATE_PAUSED -> "pause"
            BaseVideoView.STATE_BUFFERING -> "buffering"
            BaseVideoView.STATE_BUFFERED -> "buffered"
            BaseVideoView.STATE_PLAYBACK_COMPLETED -> "playback completed"
            BaseVideoView.STATE_ERROR -> "error"
            else -> "idle"
        }
        return String.format("playState: %s", playStateString)
    }

    /**
     * Returns a string containing player state debugging information.
     */
    @JvmStatic
    fun playerState2str(state: Int): String {
        val playerStateString: String = when (state) {
            BaseVideoView.PLAYER_NORMAL -> "normal"
            BaseVideoView.PLAYER_FULL_SCREEN -> "full screen"
            BaseVideoView.PLAYER_TINY_SCREEN -> "tiny screen"
            else -> "normal"
        }
        return String.format("playerState: %s", playerStateString)
    }

    /**
     * Gets the corresponding path to a file from the given content:// URI
     *
     * @param context    Context
     * @param contentUri The content:// URI to find the file path from
     * @return the file path as a string
     */
    @SuppressLint("Range")
    @JvmStatic
    fun getFileFromContentUri(context: Context, contentUri: Uri?): String? {
        if (contentUri == null) {
            return null
        }
        if (ContentResolver.SCHEME_FILE == contentUri.scheme) {
            return contentUri.toString()
        }
        var filePath: String? = null
        val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME)
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            contentUri, filePathColumn, null,
            null, null
        )
        if (cursor != null) {
            cursor.moveToFirst()
            filePath = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))
            cursor.close()
        }
        return filePath
    }
}