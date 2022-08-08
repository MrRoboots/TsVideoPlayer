package com.robot.tsplayer_kotlin.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build


class PlayerUtils {

    companion object {
        private const val NO_NETWORK = 0
        private const val NETWORK_CLOSED = 1
        private const val NETWORK_ETHERNET = 2
        private const val NETWORK_WIFI = 3
        const val NETWORK_MOBILE = 4
        private const val NETWORK_UNKNOWN = -1

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
    }


}