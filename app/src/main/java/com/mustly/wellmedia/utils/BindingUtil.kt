package com.mustly.wellmedia.utils

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.mustly.wellmedia.lib.commonlib.utils.ReflectionUtil

/**
 * description:
 *
 * @author   wchenzhang
 * date：    2022/8/15 19:32
 * version   1.0
 * modify by
 */
object BindingUtil {
    fun <T : ViewDataBinding?> getDataBinding(
        activity: Any,
        clazz: Class<*>?,
        index: Int,
        root: ViewGroup?,
        attachToRoot: Boolean
    ): T? {
        if (activity !is Activity && activity !is Fragment) {
            return null
        }

        val context = if (activity is Fragment) {
            activity.requireContext()
        } else {
            activity as Activity
        }

        val bindingClass: Class<T>? = ReflectionUtil.getGenericClass(activity, clazz, index)
        return ReflectionUtil.invokeMethod<T>(
            bindingClass,
            null,
            "inflate",
            arrayOf(
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.javaPrimitiveType
            ),
            arrayOf(LayoutInflater.from(context), root, attachToRoot)
        )
    }
}