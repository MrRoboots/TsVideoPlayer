package com.robot.tsplayer_kotlin.render

import android.content.Context
import com.robot.tsplayer_kotlin.player.VideoViewManager

class TextureRenderViewFactory : RenderViewFactory() {

    companion object {
        fun create(): TextureRenderViewFactory {
            return TextureRenderViewFactory()
        }
    }

    override fun createRenderView(context: Context): IRenderView {
        return TextureRenderView(context)
    }
}