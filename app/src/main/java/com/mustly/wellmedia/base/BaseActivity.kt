package com.mustly.wellmedia.base

import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.mustly.wellmedia.R
import com.mustly.wellmedia.colorRes

abstract class BaseActivity<VB : ViewDataBinding>(@LayoutRes val layoutResId: Int) : AppCompatActivity() {
    lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        // 需要在 onCreate 之前调用
        supportFragmentManager.fragmentFactory = MediaFragmentFactory
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, layoutResId)

        if(isConfigActivity()) {
            configActivity()
        }

        preParseData(savedInstanceState)

        initView()

        initData()
    }

    /**
     * 通过设置全屏，设置状态栏透明
     *
     * @url https://blog.csdn.net/brian512/article/details/52096445
     */
    private fun configActivity() {
        // 设置沉浸式状态栏
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = R.color.color_39e600.colorRes()
    }

    /**
     * 是否需要配置 Activity
     * */
    protected open fun isConfigActivity(): Boolean {
        return true
    }

    abstract fun preParseData(savedInstanceState: Bundle?)

    abstract fun initView()

    abstract fun initData()
}