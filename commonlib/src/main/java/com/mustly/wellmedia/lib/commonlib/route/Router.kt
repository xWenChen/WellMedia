package com.mustly.wellmedia.lib.commonlib.route

import android.content.Context
import android.content.Intent

typealias InitBlock = Intent.() -> Unit

object Router {
    fun go(
        context: Context,
        url: String,
        useNewTask: Boolean = false,
        initBlock: InitBlock = { }
    ) {
        context.startActivity(getIntent(context, url).apply {
            if (useNewTask) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            initBlock(this)
        })
    }

    fun getIntent(context: Context, url: String): Intent {
        val className = RouteHub.routeTable[url] ?: ""
        return Intent().setClassName(context, className)
    }
}