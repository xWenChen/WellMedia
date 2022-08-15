package com.mustly.wellmedia.base

import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import com.mustly.wellmedia.R
import com.mustly.wellmedia.utils.BindingUtil
import com.mustly.wellmedia.utils.colorRes

abstract class BaseActivity<VB : ViewDataBinding> : AppCompatActivity() {
    lateinit var binding: VB

    var isConfigActivity = true
    var hideTitleBar = true
    var hideStatusBar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 需要在 onCreate 之前调用
        supportFragmentManager.fragmentFactory = MediaFragmentFactory
        super.onCreate(savedInstanceState)

        // requestWindowFeature() 方法必须在 setContentView() 调用前使用
        if(isConfigActivity) {
            configActivity()
        }

        binding = BindingUtil.getDataBinding<VB>(this, BaseActivity::class.java, 0, null, false)
            ?: error("can not get binding")

        setContentView(binding.root)

        preParseData()

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
        window.statusBarColor = R.color.color_39e600.colorRes
        if (hideTitleBar) {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            supportActionBar?.hide()
        }
        if (hideStatusBar) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            } else {
                window.decorView.windowInsetsController?.hide(WindowInsets.Type.statusBars())
            }
        }
    }

    abstract fun preParseData()

    abstract fun initView()

    abstract fun initData()
}