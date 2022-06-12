package com.mustly.wellmedia.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import com.mustly.wellmedia.utils.checkAndRequestPermission
import java.lang.IllegalArgumentException

/**
 * 基础 Fragment，封装了懒加载的相关逻辑
 * */
abstract class BaseFragment(@LayoutRes layoutResId: Int) : Fragment(layoutResId) {
    companion object {
        const val TAG = "BaseFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
            ?: throw InflateException("fragment inflate error")

        initArguments(savedInstanceState)
        initView(view)
        initData(view.context)

        return view
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

fun FragmentTransaction.showFragment(fragment: Fragment) {
    this.show(fragment)
    this.setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
}

fun FragmentTransaction.hideFragment(fragment: Fragment) {
    this.hide(fragment)
    this.setMaxLifecycle(fragment, Lifecycle.State.STARTED)
}

fun FragmentTransaction.addFragment(containerId: Int, route: String, args: (Bundle.() -> Unit)? = null) {
    val bundle: Bundle?
    if(args == null) {
        bundle = null
    } else {
        bundle = Bundle()
        args.invoke(bundle)
    }

    route.getFragmentClass()?.also {
        this.add(containerId, it, bundle, route)
    } ?: Log.e("FragmentTransaction", "addFragment", IllegalArgumentException("the class of $route is null"))
}

fun FragmentTransaction.replaceFragment(containerId: Int, route: String, args: (Bundle.() -> Unit)? = null) {
    val bundle: Bundle?
    if(args == null) {
        bundle = null
    } else {
        bundle = Bundle()
        args.invoke(bundle)
    }

    route.getFragmentClass()?.also {
        this.replace(containerId, it, bundle, route)
    } ?: Log.e("FragmentTransaction", "replaceFragment", IllegalArgumentException("the class of $route is null"))
}

fun FragmentManager.commitTransaction(allowStateLoss: Boolean = true, commitNow: Boolean = false, task: (FragmentTransaction.() -> Unit)? = null) {
    val temp = this.beginTransaction().apply {
        task?.invoke(this)
    }
    if (allowStateLoss && commitNow) {
        temp.commitNowAllowingStateLoss()
    } else if (allowStateLoss) {
        temp.commitAllowingStateLoss()
    } else if (commitNow) {
        temp.commitNow()
    } else {
        temp.commit()
    }
}