package com.robot.tsvideoplayer.activity

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.robot.tsplayer_kotlin.player.BaseVideoView
import com.robot.tsplayer_kotlin.player.VideoViewManager
import com.robot.tsvideoplayer.R

open class BaseActivity<T : BaseVideoView<*>> : AppCompatActivity() {

    var mVideoView: T? = null

    open fun getTitleResId(): Int {
        return R.string.app_name
    }

    open fun getLayoutResId(): Int {
        return 0
    }

    open fun getContentView(): View? {
        return null
    }

    open fun enableBack(): Boolean {
        return true
    }

    protected fun getVideoViewManager(): VideoViewManager? {
        return VideoViewManager.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getLayoutResId() != 0) {
            setContentView(getLayoutResId())
        } else if (getContentView() != null) {
            setContentView(getContentView())
        }

        //标题栏设置
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(getTitleResId())
            if (enableBack()) {
                actionBar.setDisplayHomeAsUpEnabled(true)
            }
        }

        initView()
    }

    protected open fun initView() {

    }

    protected open fun setTitle(title: String?) {
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = title
        }
    }


    /**
     * 把状态栏设成透明
     */
    protected open fun setStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val decorView = window.decorView

            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            /*  decorView.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets? ->
                  val defaultInsets = v.onApplyWindowInsets(insets)
                  defaultInsets.replaceSystemWindowInsets(
                      defaultInsets.systemWindowInsetLeft,
                      0,
                      defaultInsets.systemWindowInsetRight,
                      defaultInsets.systemWindowInsetBottom
                  )
              }*/

            ViewCompat.requestApplyInsets(decorView)
            window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (mVideoView != null) {
            mVideoView?.resume()
        }
    }


    override fun onPause() {
        super.onPause()
        if (mVideoView != null) {
            mVideoView?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mVideoView != null) {
            mVideoView?.release()
        }
    }

    override fun onBackPressed() {
        if (mVideoView == null || !mVideoView!!.onBackPressed()) {
            super.onBackPressed()
        }
    }

}