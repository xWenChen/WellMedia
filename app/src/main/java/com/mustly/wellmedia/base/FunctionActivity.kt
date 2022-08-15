package com.mustly.wellmedia.base

import android.util.Log
import com.mustly.wellmedia.R
import com.mustly.wellmedia.databinding.ActivityFunctionBinding
import com.mustly.wellmedia.utils.addFragment
import com.mustly.wellmedia.utils.commitTransaction

class FunctionActivity : BaseActivity<ActivityFunctionBinding>() {
    companion object {
        const val TAG = "FunctionActivity"
    }

    private var curFragmentTag  = ""

    override fun preParseData() {
        var tempTag = intent?.getStringExtra(PageRoute.Param.KEY_FRAGMENT_TAG)
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