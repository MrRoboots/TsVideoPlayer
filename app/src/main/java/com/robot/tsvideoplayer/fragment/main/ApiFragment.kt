package com.robot.tsvideoplayer.fragment.main

import android.view.View
import android.widget.Button
import com.robot.tsvideoplayer.R
import com.robot.tsvideoplayer.activity.api.PlayerActivity
import com.robot.tsvideoplayer.fragment.BaseFragment

class ApiFragment : BaseFragment(), View.OnClickListener {

    companion object {
        const val SAMPLE_URL = "http://vfx.mtime.cn/Video/2019/03/14/mp4/190314223540373995.mp4"
        const val SAMPLE_URL_CACHE = "http://video.cdn.aizys.com/zzx3.9g.mkv"
        const val SAMPLE_URL_CACHE2 = "https://res.exexm.com/cw_145225549855002"
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_api
    }


    override fun initView() {
        super.initView()
        val btnVod = findViewById<Button>(R.id.btn_vod)
        btnVod?.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_vod -> {
                context?.let { PlayerActivity.start(it, SAMPLE_URL, getString(R.string.str_api_vod), false) }
            }
        }
    }

}