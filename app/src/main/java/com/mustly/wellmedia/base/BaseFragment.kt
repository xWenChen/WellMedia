package com.mustly.wellmedia.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.mustly.wellmedia.utils.checkAndRequestPermission

/**
 * 基础 Fragment，封装了懒加载的相关逻辑
 * */
abstract class BaseFragment<VB : ViewDataBinding>(@LayoutRes val layoutResId: Int) : Fragment() {
    companion object {
        const val TAG = "BaseFragment"
    }

    lateinit var binding: VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater, layoutResId, container, false)

        initArguments(savedInstanceState)
        initView(binding.root)
        initData(binding.root.context)

        return binding.root
    }

    fun startFunctionActivity(
        route: String,
        dataSetCallback: (Bundle.() -> Unit)? = null
    ) {
        startActivity(Intent(activity, FunctionActivity::class.java), Bundle().apply {
            putString(PageRoute.Param.KEY_FRAGMENT_TAG, route)
            dataSetCallback?.invoke(this)
        })
    }

    fun checkAndRequestPermission(
        permission: String,
        title: String = "",
        desc: String = "",
        callback: ((Boolean) -> Unit)
    ) {
        activity?.checkAndRequestPermission(permission, title, desc, callback) ?: Log.w(TAG, "check permission, not find activity")
    }

    /**
     * 获取用户传入的参数
     * */
    abstract fun initArguments(savedInstanceState: Bundle?)

    abstract fun initView(rootView: View)

    abstract fun initData(context: Context)
}