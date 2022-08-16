package com.mustly.wellmedia.lib.commonlib.route

import android.content.Context
import android.content.pm.PackageManager
import androidx.startup.Initializer
import com.mustly.wellmedia.lib.annotation.Constants
import com.mustly.wellmedia.lib.annotation.Utils

/**
 * description:
 *
 * date：    2022/5/25 19:35
 * version   1.0
 * modify by
 */
class RouteInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        init(context)
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()

    private fun init(context: Context) = context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA
    ).metaData?.apply {
        keySet().forEach {
            val moduleName = getString(it, null)
            if (Constants.META_VALUE == moduleName) {
                injectRoute(it)
            }
        }
    }

    // 路由数据写入 RouteHub
    private fun injectRoute(moduleName: String) = try {
        Class.forName(getTableClassName(moduleName))
            .takeIf {
                // 判断是否是 RouteRegister 的子类
                RouteRegister::class.java.isAssignableFrom(it)
            }?.run {
                // 生成 RouteRegister 子类的实例
                (this as Class<out RouteRegister>).newInstance()
            }?.also { routeRegister ->
                // 路由数据写入 RouteHub
                routeRegister.register(RouteHub.routeTable)
            }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    private fun getTableClassName(moduleName: String) =
        Constants.GENERATED_ROUTE_REGISTER_PATH + "." + Utils.getRegisterClassName(moduleName)
}