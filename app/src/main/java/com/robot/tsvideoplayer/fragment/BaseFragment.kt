package com.robot.tsvideoplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.robot.tsplayer_kotlin.player.VideoViewManager

abstract class BaseFragment : Fragment() {

    var mRootView: View? = null
    private var mIsInitData = false
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (mRootView == null) {
            mRootView = inflater.inflate(getLayoutResId(), container, false)
            initView()
        }
        return mRootView
    }

    open fun initView() {

    }

    abstract fun getLayoutResId(): Int


    open fun <T : View> findViewById(@IdRes idRes: Int): T? {
        return mRootView?.findViewById(idRes)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isLazyLoad()) {
            fetchData()
        }
    }

    private fun fetchData() {
        if (mIsInitData) {
            return
        }
        initData()
        mIsInitData = true
    }

    open fun initData() {}

    private fun isLazyLoad(): Boolean {
        return false
    }

    override fun onResume() {
        super.onResume()
        fetchData()
    }

    /**
     * 子类可通过此方法直接拿到VideoViewManager
     */
    protected open fun getVideoViewManager(): VideoViewManager? {
        return VideoViewManager.getInstance()
    }

}