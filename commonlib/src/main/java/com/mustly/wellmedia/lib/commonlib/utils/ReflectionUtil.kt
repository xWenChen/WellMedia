package com.mustly.wellmedia.lib.commonlib.utils

import com.mustly.wellmedia.lib.commonlib.log.LogUtil
import java.lang.reflect.ParameterizedType

/**
 * description:
 *
 * 反射工具类
 *
 * @author   wchenzhang
 * date：    2022/8/15 15:14
 * version   1.0
 * modify by
 */
object ReflectionUtil {
    const val TAG = "ReflectionUtil"

    //获取第几个泛型参数的实际类型
    fun <T> getGenericClass(instance: Any?, clazz: Class<*>?, index: Int): Class<T>? {
        if (instance == null) {
            return null
        }
        try {
            var startClazz: Class<*> = instance.javaClass
            while (startClazz.superclass != clazz && startClazz.superclass != Any::class.java) {
                startClazz = startClazz.superclass
            }
            if (startClazz != Any::class.java) {
                return (startClazz.genericSuperclass as ParameterizedType).actualTypeArguments[index] as Class<T>
            }
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
        return null
    }

    fun <T> invokeMethod(
        clazz: Class<*>?,
        any: Any?,
        methodName: String?,
        classes: Array<Class<*>?>,
        params: Array<Any?>
    ): T? {
        if (clazz == null) {
            return null
        }
        try {
            val method = clazz.getMethod(methodName, *classes)
            return method.invoke(any, *params) as T?
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
        return null
    }
}