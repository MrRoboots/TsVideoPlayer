package com.robot.tsplayer_kotlin.player

class VideoViewManager {

    companion object {


        @JvmStatic
        var sInstance: VideoViewManager? = null

        /**
         * VideoViewManager实例
         */
        @JvmStatic
        fun getInstance(): VideoViewManager? {
            if (sInstance == null) {
                synchronized(VideoViewManager::class.java) {
                    if (sInstance == null) {
                        sInstance = VideoViewManager()
                    }
                }
            }
            return sInstance
        }

        /**
         * VideoViewConfig实例
         */
        @JvmStatic
        var sConfig: VideoViewConfig? = null

        /**
         * 设置VideoViewConfig
         */
        @JvmStatic
        fun setConfig(config: VideoViewConfig?) {
            if (sConfig == null) {
                synchronized(VideoViewConfig::class.java) {
                    if (sConfig == null) {
                        sConfig = config ?: VideoViewConfig.newBuilder().build()
                    }
                }
            }
        }

        /**
         * 获取VideoViewConfig
         */
        @JvmStatic
        fun getConfig(): VideoViewConfig? {
            setConfig(null)
            return sConfig
        }
    }



}