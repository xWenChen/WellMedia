package com.mustly.wellmedia.base

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import com.mustly.wellmedia.lib.commonlib.route.Router
import com.mustly.wellmedia.utils.checkAndRequestPermission
import com.mustly.wellmedia.utils.checkAndRequestPermissions
import com.mustly.wellmedia.utils.keepScreenOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 基础 Fragment，封装了懒加载的相关逻辑
 * */
abstract class BaseFragment : Fragment() {
    companion object {
        const val TAG = "BaseFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = createView(inflater, container, savedInstanceState)
        initData(root)

        return root
    }

    fun startFunctionActivity(route: String, ) {
        val context = activity ?: return
        Router.go(context, PageRoute.FUNCTION_ACTIVITY) {
            putExtra(PageRoute.Param.KEY_FRAGMENT_TAG, route)
        }
    }

    fun requestPermission(
        permission: String,
        title: String = "",
        desc: String = "",
        callback: ((Boolean) -> Unit)
    ) {
        activity?.checkAndRequestPermission(permission, title, desc, callback) ?: Log.w(TAG, "check permission, not find activity")
    }

    suspend fun suspendRequestPermission(
        permission: String,
        title: String = "",
        desc: String = "",
    ): Boolean = suspendCancellableCoroutine { cont ->
        val aty = activity
        if (aty == null) {
            LogUtil.w(TAG, "check permission, not find activity")
            return@suspendCancellableCoroutine cont.resume(false)
        }

        aty.checkAndRequestPermission(permission, title, desc) {
            cont.resume(it)
        }
    }

    suspend fun suspendRequestPermissions(
        permissions: Array<String>,
        title: String = "",
        desc: String = "",
    ): Map<String, Boolean> = suspendCancellableCoroutine { cont ->
        val aty = activity
        if (aty == null) {
            LogUtil.w(TAG, "check permission, not find activity")
            return@suspendCancellableCoroutine cont.resume(permissions.associateWith { false })
        }

        aty.checkAndRequestPermissions(permissions, title, desc) {
            cont.resume(it)
        }
    }

    fun keepScreenOn(enable: Boolean) {
        activity?.keepScreenOn(enable)
    }

    /**
     * 作用类似于 activity.finish()
     * */
    fun finish() {
        try {
            activity?.supportFragmentManager?.popBackStackImmediate()
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
    }

    abstract fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View

    abstract fun initData(rootView: View)
}