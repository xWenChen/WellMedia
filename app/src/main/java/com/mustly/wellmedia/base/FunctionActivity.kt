package com.mustly.wellmedia.base

import android.os.Bundle
import android.util.Log
import com.mustly.wellmedia.R

class FunctionActivity : BaseActivity() {
    companion object {
        const val TAG = "FunctionActivity"
    }

    private var curFragmentTag  = ""

    override fun getLayoutResId(): Int {
        return R.layout.activity_function
    }

    override fun preParseData(savedInstanceState: Bundle?) {
        var tempTag = savedInstanceState?.getString(PageRoute.Param.KEY_FRAGMENT_TAG, "")
        if(tempTag.isNullOrEmpty()) {
            Log.e(TAG, "fragment tag is null, can not create a fragment")
            tempTag = PageRoute.AUDIO_PLAY_FRAGMENT
        }
        curFragmentTag = tempTag
    }

    override fun initView() {
        supportFragmentManager.commitTransaction {
            addFragment(getFragmentContainerId(), curFragmentTag)
        }
    }

    override fun initData() {

    }

    private fun getFragmentContainerId(): Int {
        return R.id.fcvFunction
    }
}