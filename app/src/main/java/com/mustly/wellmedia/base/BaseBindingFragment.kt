package com.mustly.wellmedia.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.mustly.wellmedia.utils.BindingUtil

/**
 * 基础 Fragment，封装了懒加载的相关逻辑
 * */
abstract class BaseBindingFragment<VB : ViewDataBinding> : BaseFragment() {
    companion object {
        const val TAG = "BaseBindingFragment"
    }

    lateinit var binding: VB

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BindingUtil.getDataBinding<VB>(
            this,
            BaseBindingFragment::class.java,
            0,
            null,
            false
        ) ?: error("can not get binding")

        initView(binding.root)

        return binding.root
    }

    abstract fun initView(rootView: View)
}