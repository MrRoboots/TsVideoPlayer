package com.robot.tsplayer_kotlin.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.robot.tsplayer_kotlin.utils.PlayerUtils.Companion.scanForActivity
import java.lang.Exception

/**
 * 刘海屏工具
 */
object CutoutUtil {
    /**
     * 是否为允许全屏界面显示内容到刘海区域的刘海屏机型（与AndroidManifest中配置对应）
     */
    fun allowDisplayToCutout(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 9.0系统全屏界面默认会保留黑边，不允许显示内容到刘海区域
            val window = activity.window
            val windowInsets = window.decorView.rootWindowInsets ?: return false
            val displayCutout = windowInsets.displayCutout ?: return false
            val rect = displayCutout.boundingRects
            rect.size > 0
        } else {
            (hasCutoutHuawei(activity)
                    || hasCutoutOPPO(activity)
                    || hasCutoutVIVO(activity)
                    || hasCutoutXIAOMI(activity))
        }
    }

    /**
     * 是否是华为刘海屏机型
     */
    private fun hasCutoutHuawei(activity: Activity): Boolean {
        return if (!Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true)) {
            false
        } else try {
            val cl = activity.classLoader
            val hwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil")
            if (hwNotchSizeUtil != null) {
                val get = hwNotchSizeUtil.getMethod("hasNotchInScreen")
                return get.invoke(hwNotchSizeUtil) as Boolean
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 是否是oppo刘海屏机型
     */
    private fun hasCutoutOPPO(activity: Activity): Boolean {
        return if (!Build.MANUFACTURER.equals("oppo", ignoreCase = true)) {
            false
        } else activity.packageManager.hasSystemFeature(
            "com.oppo.feature.screen.heteromorphism"
        )
    }

    /**
     * 是否是vivo刘海屏机型
     */
    @SuppressLint("PrivateApi")
    private fun hasCutoutVIVO(activity: Activity): Boolean {
        return if (!Build.MANUFACTURER.equals("vivo", ignoreCase = true)) {
            false
        } else try {
            val cl = activity.classLoader
            val ftFeatureUtil = cl.loadClass("android.util.FtFeature")
            if (ftFeatureUtil != null) {
                val get = ftFeatureUtil.getMethod("isFeatureSupport", Int::class.javaPrimitiveType)
                return get.invoke(ftFeatureUtil, 0x00000020) as Boolean
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 是否是小米刘海屏机型
     */
    @SuppressLint("PrivateApi")
    private fun hasCutoutXIAOMI(activity: Activity): Boolean {
        return if (!Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)) {
            false
        } else try {
            val cl = activity.classLoader
            val SystemProperties = cl.loadClass("android.os.SystemProperties")
            val paramTypes = arrayOfNulls<Class<*>?>(2)
            paramTypes[0] = String::class.java
            paramTypes[1] = Int::class.javaPrimitiveType
            val getInt = SystemProperties.getMethod("getInt", *paramTypes)
            //参数
            val params = arrayOfNulls<Any>(2)
            params[0] = "ro.miui.notch"
            params[1] = 0
            val hasCutout = getInt.invoke(SystemProperties, *params) as Int
            hasCutout == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 适配刘海屏，针对Android P以上系统
     */
    fun adaptCutoutAboveAndroidP(context: Context?, isAdapt: Boolean) {
        val activity = scanForActivity(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = activity.window.attributes
            if (isAdapt) {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
            activity.window.attributes = lp
        }
    }
}