package com.mustly.wellmedia.lib.commonlib.route

import android.content.Context

object Router {
    private var handler: RouteAnnotationHandler = DefaultRouteAnnotationHandler()

    fun go(context: Context, url :String) {
        handler.startUrl(context, url)
    }

    fun getIntent(context: Context, className: String) = handler.getIntent(context, className)
}