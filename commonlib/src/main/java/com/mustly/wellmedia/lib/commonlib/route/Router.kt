package com.mustly.wellmedia.lib.commonlib.route

import android.content.Context
import android.content.Intent

object Router {
    fun go(context: Context, url :String) {
        val className = RouteHub.routeTable[url] ?: ""
        context.startActivity(
            getIntent(context, className).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun getIntent(context: Context, className: String): Intent {
        return Intent().setClassName(context, className)
    }
}