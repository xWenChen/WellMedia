package com.mustly.wellmedia.lib.commonlib.route

import android.content.Context
import android.content.Intent

class DefaultRouteAnnotationHandler : RouteHub {

    val routeMap = HashMap<String, String>()

    override fun register(url: String, target: String) {
        routeMap[url] = target
    }

    override fun startUrl(context: Context, url: String) {
        val className = routeMap[url] ?: ""
        context.startActivity(getIntent(context, className))
    }

    override fun getIntent(context: Context, className: String): Intent {
        return Intent().setClassName(context, className)
    }
}