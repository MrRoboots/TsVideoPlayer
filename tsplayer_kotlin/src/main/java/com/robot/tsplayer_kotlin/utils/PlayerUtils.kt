package com.robot.tsplayer_kotlin.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.TypedValue
import android.view.*


class PlayerUtils {

    companion object {
        private const val NO_NETWORK = 0
        private const val NETWORK_CLOSED = 1
        private const val NETWORK_ETHERNET = 2
        private const val NETWORK_WIFI = 3
        const val NETWORK_MOBILE = 4
        private const val NETWORK_UNKNOWN = -1


        /**
         * dp转为px
         */
        @JvmStatic
        fun dp2px(context: Context, dpValue: Float): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.resources.displayMetrics)
                .toInt()
        }

        /**
         * sp转为px
         */
        @JvmStatic
        fun sp2px(context: Context, dpValue: Float): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dpValue, context.resources.displayMetrics)
                .toInt()
        }

        /**
         * 如果WindowManager还未创建，则创建一个新的WindowManager返回。否则返回当前已创建的WindowManager。
         */
        @JvmStatic
        private fun getWindowManager(context: Context): WindowManager {
            return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        /**
         * 判断当前网络类型
         */
        @JvmStatic
        fun getNetworkType(context: Context): Int {
            //改为context.getApplicationContext()，防止在Android 6.0上发生内存泄漏
            val connectMgr = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nw = connectMgr.activeNetwork ?: return NO_NETWORK
                val actNw = connectMgr.getNetworkCapabilities(nw) ?: return NETWORK_CLOSED
                return when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NETWORK_WIFI
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NETWORK_MOBILE
                    //for other device how are able to connect with Ethernet
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NETWORK_ETHERNET
                    //for check internet over Bluetooth
//                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                    else -> NETWORK_UNKNOWN
                }
            } else {
                val networkInfo = connectMgr.activeNetworkInfo ?: return NO_NETWORK // 没有任何网络
                if (!networkInfo.isConnected) {
                    // 网络断开或关闭
                    return NETWORK_CLOSED
                }
            }
            // 未知网络
            return NETWORK_UNKNOWN
        }

        /**
         * 判断是否有网络
         */
        @JvmStatic
        private fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nw = connectivityManager.activeNetwork ?: return false
                val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
                return when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    //for other device how are able to connect with Ethernet
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    //for check internet over Bluetooth
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                    else -> false
                }
            } else {
                return connectivityManager.activeNetworkInfo?.isConnected ?: false
            }
        }

        /**
         * 获取集合的快照
         */
        @JvmStatic
        fun <T> getSnapshot(other: Collection<T>): List<T> {
            val result: MutableList<T> = ArrayList(other.size)
            for (item in other) {
                if (item != null) {
                    result.add(item)
                }
            }
            return result
        }

        /**
         * 递归获取activity
         */
        @JvmStatic
        fun scanForActivity(context: Context?): Activity? {
            if (context == null) return null
            if (context is Activity) {
                return context
            } else if (context is ContextWrapper) {
                return scanForActivity(context.baseContext)
            }
            return null
        }

        /**
         * 是否存在NavigationBar
         */
        @JvmStatic
        private fun hasNavigationBar(context: Context?): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val display = context?.let { getWindowManager(it).defaultDisplay }
                val size = Point()
                val realSize = Point()
                display?.getSize(size)
                display?.getRealSize(realSize)
                realSize.x != size.x || realSize.y != size.y
            } else {
                val menu = ViewConfiguration.get(context).hasPermanentMenuKey()
                val back = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
                !(menu || back)
            }
        }

        /**
         * 获取屏幕宽度
         */
        @JvmStatic
        fun getScreenWidth(context: Context, isIncludeNav: Boolean): Int {
            return if (isIncludeNav) {
                context.resources.displayMetrics.widthPixels + getNavigationBarHeight(context)
            } else {
                context.resources.displayMetrics.widthPixels
            }
        }

        /**
         * 获取屏幕高度
         */
        @JvmStatic
        fun getScreenHeight(context: Context, isIncludeNav: Boolean): Int {
            return if (isIncludeNav) {
                context.resources.displayMetrics.heightPixels + PlayerUtils.getNavigationBarHeight(context)
            } else {
                context.resources.displayMetrics.heightPixels
            }
        }

        /**
         * 获取NavigationBar的高度
         */
        @JvmStatic
        fun getNavigationBarHeight(context: Context): Int {
            if (!PlayerUtils.hasNavigationBar(context)) {
                return 0
            }
            val resources = context.resources
            val resourceId = resources.getIdentifier(
                "navigation_bar_height",
                "dimen", "android"
            )
            //获取NavigationBar的高度
            return resources.getDimensionPixelSize(resourceId)
        }

        /**
         * 获取竖屏下状态栏高度
         */
        @JvmStatic
        fun getStatusBarHeightPortrait(context: Context): Double {
            var statusBarHeight = 0
            //获取status_bar_height_portrait资源的ID
            val resourceId = context.resources.getIdentifier("status_bar_height_portrait", "dimen", "android")
            if (resourceId > 0) {
                //根据资源ID获取响应的尺寸值
                statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
            }
            return statusBarHeight.toDouble()
        }

        /**
         * 边缘检测
         */
        fun isEdge(context: Context?, e: MotionEvent): Boolean {
            val edgeSize = dp2px(context!!, 40f)
            return e.rawX < edgeSize
                    || e.rawX > getScreenWidth(context, true) - edgeSize
                    || e.rawY < edgeSize
                    || e.rawY > getScreenHeight(context, true) - edgeSize
        }

    }
}